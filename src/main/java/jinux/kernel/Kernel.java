package jinux.kernel;

import jinux.mm.MemoryManager;
import jinux.mm.AddressSpace;
import jinux.drivers.*;
import jinux.fs.VirtualFileSystem;
import jinux.include.Const;

/**
 * Jinux 内核主类
 * 对应 Linux 0.01 中的 kernel/main.c (main 函数)
 * 
 * 负责初始化各个子系统并启动系统
 * 
 * @author Jinux Project
 */
public class Kernel {
    
    /** 内存管理器 */
    private final MemoryManager memoryManager;
    
    /** 进程调度器 */
    private final Scheduler scheduler;
    
    /** 虚拟文件系统 */
    private final VirtualFileSystem vfs;
    
    /** 系统调用分发器 */
    private final SystemCallDispatcher syscallDispatcher;
    
    /** 控制台设备 */
    private final ConsoleDevice console;
    
    /** 虚拟磁盘设备 */
    private final VirtualDiskDevice disk;
    
    /** 时钟设备 */
    private final ClockDevice clock;
    
    /** 系统是否运行中 */
    private volatile boolean running;
    
    /**
     * 构造内核
     */
    public Kernel() {
        System.out.println("\n========================================");
        System.out.println("        Jinux Operating System");
        System.out.println("    (Java Implementation of Linux 0.01)");
        System.out.println("========================================\n");
        
        // 初始化内存管理
        this.memoryManager = new MemoryManager();
        
        // 初始化调度器
        this.scheduler = new Scheduler();
        
        // 初始化系统调用
        this.syscallDispatcher = new SystemCallDispatcher(scheduler, memoryManager);
        
        // 初始化设备
        this.console = new ConsoleDevice();
        this.disk = new VirtualDiskDevice("jinux-disk.img", 10); // 10MB
        this.clock = new ClockDevice(scheduler);
        
        // 初始化文件系统
        this.vfs = new VirtualFileSystem();
        this.syscallDispatcher.setVfs(vfs);
        
        this.running = false;
    }
    
    /**
     * 内核初始化
     * 对应 Linux 0.01 的 main() 函数
     */
    public void init() {
        System.out.println("[KERNEL] Starting kernel initialization...\n");
        
        // 初始化设备
        console.init();
        disk.init();
        clock.init();
        
        // 初始化文件系统
        vfs.setDisk(disk);
        vfs.init();
        
        System.out.println();
        memoryManager.printStats();
        vfs.printStats();
        System.out.println();
        
        System.out.println("[KERNEL] Kernel initialization complete.\n");
    }
    
    /**
     * 创建 init 进程（PID 1）
     * 对应 Linux 0.01 的第一个用户进程
     */
    public void createInitProcess() {
        System.out.println("[KERNEL] Creating init process (PID 1)...");
        
        // 分配 PID
        int pid = scheduler.allocatePid();
        
        // 创建地址空间
        AddressSpace addressSpace = memoryManager.createAddressSpace();
        
        // 分配一些初始内存（代码段、数据段、栈）
        // 简化：分配几个页面用于基本运行
        for (int i = 0; i < 4; i++) {
            long vaddr = i * Const.PAGE_SIZE;
            addressSpace.allocateAndMap(vaddr, 
                jinux.mm.PageTable.PAGE_PRESENT | 
                jinux.mm.PageTable.PAGE_RW | 
                jinux.mm.PageTable.PAGE_USER);
        }
        
        // 创建进程
        Task initTask = new Task(pid, 0, addressSpace); // ppid=0（没有父进程）
        
        // 设置可执行代码（使用 InitProcess）
        jinux.init.InitProcess initProc = new jinux.init.InitProcess(this, initTask);
        initTask.setExecutable(initProc);
        
        // 添加到调度器
        scheduler.addTask(initTask);
        
        System.out.println("[KERNEL] Init process created: " + initTask);
    }
    
    /**
     * 启动内核
     */
    public void start() {
        System.out.println("\n[KERNEL] Starting Jinux...\n");
        
        running = true;
        
        // 启动时钟
        clock.start();
        
        // 开始调度（选择第一个进程）
        scheduler.schedule();
        
        // 运行 init 进程
        Task initTask = scheduler.getCurrentTask();
        if (initTask != null && initTask.getExecutable() != null) {
            Thread initThread = new Thread(initTask.getExecutable(), "init-process");
            initTask.setExecutionThread(initThread);
            initThread.start();
            
            // 等待 init 进程结束
            try {
                initThread.join();
            } catch (InterruptedException e) {
                System.err.println("[KERNEL] Interrupted while waiting for init process");
            }
        }
        
        System.out.println("\n[KERNEL] Shutting down...");
        shutdown();
    }
    
    /**
     * 关闭内核
     */
    public void shutdown() {
        running = false;
        
        // 停止时钟
        clock.stop();
        
        // 关闭磁盘
        disk.close();
        
        // 打印最终统计信息
        System.out.println("\n========== Final Statistics ==========");
        memoryManager.printStats();
        scheduler.printProcessList();
        System.out.println("======================================\n");
        
        System.out.println("[KERNEL] Jinux halted.");
    }
    
    /**
     * 测试系统调用
     */
    public void testSystemCalls() {
        console.println("\n[KERNEL] Testing system calls...\n");
        
        Task currentTask = scheduler.getCurrentTask();
        if (currentTask == null) {
            console.println("[KERNEL] ERROR: No current task for testing");
            return;
        }
        
        // 测试 getpid
        long pid = syscallDispatcher.dispatch(jinux.include.Syscalls.SYS_GETPID, 0, 0, 0);
        console.println("[TEST] getpid() = " + pid);
        
        // 测试 time
        long time = syscallDispatcher.dispatch(jinux.include.Syscalls.SYS_TIME, 0, 0, 0);
        console.println("[TEST] time() = " + time);
        
        // 测试 brk
        long newBrk = syscallDispatcher.dispatch(jinux.include.Syscalls.SYS_BRK, 0x10000, 0, 0);
        console.println("[TEST] brk(0x10000) = 0x" + Long.toHexString(newBrk));
        
        console.println("\n[KERNEL] System call tests complete.\n");
    }
    
    // ==================== Getters ====================
    
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }
    
    public Scheduler getScheduler() {
        return scheduler;
    }
    
    public SystemCallDispatcher getSyscallDispatcher() {
        return syscallDispatcher;
    }
    
    public ConsoleDevice getConsole() {
        return console;
    }
    
    public VirtualDiskDevice getDisk() {
        return disk;
    }
    
    public ClockDevice getClock() {
        return clock;
    }
}
