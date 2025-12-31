package jinux.demo;

import jinux.kernel.Kernel;
import jinux.kernel.Task;
import jinux.kernel.Scheduler;
import jinux.kernel.Signal;
import jinux.mm.MemoryManager;
import jinux.mm.AddressSpace;
import jinux.ipc.Pipe;
import jinux.fs.File;
import jinux.fs.VirtualFileSystem;
import jinux.lib.LibC;

/**
 * 系统能力综合演示
 * 展示 Jinux 启动后的各种核心功能
 * 
 * @author Jinux Project
 */
public class SystemCapabilitiesDemo {
    
    /**
     * 运行所有系统能力演示
     */
    public static void runAllDemos(Kernel kernel) {
        var console = kernel.getConsole();
        
        console.println("\n");
        console.println("╔════════════════════════════════════════════════════════════╗");
        console.println("║                                                            ║");
        console.println("║         Jinux System Capabilities Demonstration            ║");
        console.println("║                                                            ║");
        console.println("╚════════════════════════════════════════════════════════════╝");
        console.println("");
        
        // 1. 系统信息展示
        showSystemInfo(kernel);
        
        // 2. 进程调度演示
        demonstrateScheduling(kernel);
        
        // 3. 内存管理演示
        demonstrateMemoryOperations(kernel);
        
        // 4. 文件系统演示
        demonstrateFileSystem(kernel);
        
        // 5. 进程间通信演示
        demonstrateIPC(kernel);
        
        // 6. 信号处理演示
        demonstrateSignalHandling(kernel);
        
        // 7. 系统调用性能演示
        demonstrateSyscallPerformance(kernel);
        
        console.println("\n");
        console.println("╔════════════════════════════════════════════════════════════╗");
        console.println("║         All Demonstrations Completed Successfully!        ║");
        console.println("╚════════════════════════════════════════════════════════════╝");
        console.println("");
    }
    
    /**
     * 展示系统基本信息
     */
    public static void showSystemInfo(Kernel kernel) {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        LibC libc = new LibC(syscall);
        
        console.println("\n═══════════════════════════════════════════════════════════");
        console.println("  [1/7] System Information");
        console.println("═══════════════════════════════════════════════════════════\n");
        
        // 进程信息
        int pid = libc.getpid();
        int ppid = libc.getppid();
        console.println("  Process Information:");
        console.println("    PID (Process ID):     " + pid);
        console.println("    PPID (Parent PID):    " + ppid);
        
        // 时间信息
        long time = libc.time();
        console.println("\n  Time Information:");
        console.println("    System Time:          " + time + " (Unix timestamp)");
        console.println("    Human Readable:       " + new java.util.Date(time * 1000));
        
        // 内存信息
        MemoryManager mm = kernel.getMemoryManager();
        console.println("\n  Memory Information:");
        mm.printStats();
        
        // 文件系统信息
        VirtualFileSystem vfs = kernel.getVFS();
        if (vfs != null) {
            console.println("\n  File System Information:");
            console.println("    Root FS initialized:  Yes");
            console.println("    Inodes available:     1024");
            console.println("    Data blocks:           10240");
        }
        
        console.println("");
    }
    
    /**
     * 演示进程调度能力
     */
    public static void demonstrateScheduling(Kernel kernel) {
        var console = kernel.getConsole();
        Scheduler scheduler = kernel.getScheduler();
        Task currentTask = scheduler.getCurrentTask();
        
        console.println("\n═══════════════════════════════════════════════════════════");
        console.println("  [2/7] Process Scheduling Demonstration");
        console.println("═══════════════════════════════════════════════════════════\n");
        
        if (currentTask != null) {
            console.println("  Current Process:");
            console.println("    PID:      " + currentTask.getPid());
            console.println("    State:    " + currentTask.getStateName());
            console.println("    Priority: " + currentTask.getPriority());
            console.println("    Counter:  " + currentTask.getCounter());
            
            // 模拟时间片消耗
            console.println("\n  Simulating time slice consumption:");
            int initialCounter = currentTask.getCounter();
            for (int i = 0; i < 3; i++) {
                // 模拟调度器减少计数器
                int oldCounter = currentTask.getCounter();
                currentTask.setCounter(Math.max(0, oldCounter - 1));
                console.println("    Tick " + (i + 1) + ": counter " + oldCounter + " -> " + currentTask.getCounter());
            }
            
            // 恢复计数器
            currentTask.setCounter(initialCounter);
            console.println("    (Counter restored for demonstration)");
        }
        
        console.println("\n  Scheduling Algorithm:");
        console.println("    Type:      Time-sliced Round-Robin with Priority");
        console.println("    Formula:   counter = counter/2 + priority");
        console.println("    Frequency: 100 Hz (10ms per tick)");
        
        console.println("");
    }
    
    /**
     * 演示内存管理操作
     */
    public static void demonstrateMemoryOperations(Kernel kernel) {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        LibC libc = new LibC(syscall);
        Task currentTask = kernel.getScheduler().getCurrentTask();
        
        console.println("\n═══════════════════════════════════════════════════════════");
        console.println("  [3/7] Memory Management Demonstration");
        console.println("═══════════════════════════════════════════════════════════\n");
        
        if (currentTask != null) {
            AddressSpace addrSpace = currentTask.getAddressSpace();
            
            console.println("  Address Space Layout:");
            console.println("    Code Segment:   0x" + Long.toHexString(addrSpace.getCodeStart()) + 
                          " - 0x" + Long.toHexString(addrSpace.getCodeEnd()));
            console.println("    Data Segment:   0x" + Long.toHexString(addrSpace.getDataStart()) + 
                          " - 0x" + Long.toHexString(addrSpace.getDataEnd()));
            console.println("    Heap (brk):     0x" + Long.toHexString(addrSpace.getBrk()));
            console.println("    Stack Top:      0x" + Long.toHexString(addrSpace.getStackTop()));
            
            // 演示 brk 系统调用
            console.println("\n  Demonstrating brk() system call:");
            long oldBrk = addrSpace.getBrk();
            console.println("    Current brk:  0x" + Long.toHexString(oldBrk));
            
            long newBrk = oldBrk + 0x1000; // 扩展 4KB
            long result = libc.brk(newBrk);
            console.println("    New brk:      0x" + Long.toHexString(newBrk));
            console.println("    Result:       0x" + Long.toHexString(result));
            
            // 演示 malloc
            console.println("\n  Demonstrating malloc() (simplified):");
            long alloc1 = libc.malloc(1024);
            console.println("    malloc(1024)  = 0x" + Long.toHexString(alloc1));
            long alloc2 = libc.malloc(2048);
            console.println("    malloc(2048)  = 0x" + Long.toHexString(alloc2));
            
            // 恢复 brk
            libc.brk(oldBrk);
        }
        
        console.println("\n  Memory Management Features:");
        console.println("    ✓ Physical memory: 16MB (3840 pages)");
        console.println("    ✓ Virtual memory:  64MB per process");
        console.println("    ✓ Page size:        4KB");
        console.println("    ✓ Page table:       Two-level");
        
        console.println("");
    }
    
    /**
     * 演示文件系统能力
     */
    public static void demonstrateFileSystem(Kernel kernel) {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        LibC libc = new LibC(syscall);
        
        console.println("\n═══════════════════════════════════════════════════════════");
        console.println("  [4/7] File System Demonstration");
        console.println("═══════════════════════════════════════════════════════════\n");
        
        console.println("  File System Architecture:");
        console.println("    Type:        MINIX-like Virtual File System");
        console.println("    Inodes:      1024");
        console.println("    Data blocks: 10240");
        console.println("    Block size:  1024 bytes");
        
        // 演示文件操作（如果支持）
        console.println("\n  File Operations:");
        console.println("    Supported syscalls:");
        console.println("      ✓ open()   - Open/create file");
        console.println("      ✓ read()   - Read from file");
        console.println("      ✓ write()  - Write to file");
        console.println("      ✓ close()  - Close file descriptor");
        console.println("      ✓ lseek()  - Seek file pointer");
        console.println("      ✓ creat()  - Create file");
        console.println("      ✓ unlink() - Delete file");
        console.println("      ✓ chdir()  - Change directory");
        console.println("      ✓ mkdir()  - Create directory");
        console.println("      ✓ rmdir()  - Remove directory");
        
        // 尝试创建一个文件（演示）
        console.println("\n  Demonstrating file creation:");
        try {
            String testFile = "/tmp/demo_file.txt";
            int flags = File.O_CREAT | File.O_WRONLY | File.O_TRUNC;
            int fd = libc.open(testFile, flags, 0644);
            
            if (fd >= 0) {
                console.println("    ✓ Created file: " + testFile + " (fd=" + fd + ")");
                
                // 写入数据
                String content = "Hello from Jinux!\nThis is a demo file.";
                byte[] data = content.getBytes();
                int written = libc.write(fd, data, data.length);
                console.println("    ✓ Wrote " + written + " bytes");
                
                // 关闭文件
                libc.close(fd);
                console.println("    ✓ Closed file descriptor");
            } else {
                console.println("    (File system operations require full VFS implementation)");
            }
        } catch (Exception e) {
            console.println("    (File operations demonstration skipped)");
        }
        
        console.println("");
    }
    
    /**
     * 演示进程间通信
     */
    public static void demonstrateIPC(Kernel kernel) {
        var console = kernel.getConsole();
        
        console.println("\n═══════════════════════════════════════════════════════════");
        console.println("  [5/7] Inter-Process Communication (IPC) Demonstration");
        console.println("═══════════════════════════════════════════════════════════\n");
        
        // 创建管道
        console.println("  Creating a pipe...");
        Pipe pipe = new Pipe();
        console.println("    ✓ Pipe created (capacity: " + Pipe.PIPE_BUF + " bytes)");
        
        // 写入数据
        console.println("\n  Writing data to pipe:");
        String[] messages = {
            "Message 1: Hello from producer!",
            "Message 2: Pipe communication works!",
            "Message 3: This is a circular buffer."
        };
        
        int totalWritten = 0;
        for (String msg : messages) {
            byte[] data = (msg + "\n").getBytes();
            int written = pipe.write(data, data.length);
            totalWritten += written;
            console.println("    ✓ Wrote: \"" + msg + "\" (" + written + " bytes)");
        }
        console.println("    Total written: " + totalWritten + " bytes");
        console.println("    Pipe available: " + pipe.available() + " bytes");
        
        // 读取数据
        console.println("\n  Reading data from pipe:");
        byte[] buffer = new byte[1024];
        int totalRead = 0;
        int chunkCount = 0;
        
        while (pipe.available() > 0 && chunkCount < 10) {
            int read = pipe.read(buffer, buffer.length);
            if (read > 0) {
                totalRead += read;
                String content = new String(buffer, 0, read).trim();
                console.println("    ✓ Read chunk " + (++chunkCount) + ": " + read + " bytes");
                console.println("      Content: \"" + content.substring(0, Math.min(40, content.length())) + "...\"");
            } else {
                break;
            }
        }
        console.println("    Total read: " + totalRead + " bytes");
        
        // 测试 EOF
        console.println("\n  Testing EOF detection:");
        pipe.closeWrite();
        int eofRead = pipe.read(buffer, 10);
        console.println("    ✓ Read after close: " + eofRead + " (0 = EOF)");
        
        console.println("\n  IPC Features:");
        console.println("    ✓ Pipe creation: pipe() syscall");
        console.println("    ✓ Circular buffer: 4KB capacity");
        console.println("    ✓ Blocking I/O: Read/write can block");
        console.println("    ✓ EOF detection: Returns 0 when write end closed");
        console.println("    ✓ SIGPIPE: Signal when read end closed");
        
        console.println("");
    }
    
    /**
     * 演示信号处理能力
     */
    public static void demonstrateSignalHandling(Kernel kernel) {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        LibC libc = new LibC(syscall);
        
        console.println("\n═══════════════════════════════════════════════════════════");
        console.println("  [6/7] Signal Handling Demonstration");
        console.println("═══════════════════════════════════════════════════════════\n");
        
        int pid = libc.getpid();
        console.println("  Current PID: " + pid);
        
        // 显示常见信号
        console.println("\n  Common Signals:");
        int[] signals = {
            Signal.SIGINT, Signal.SIGTERM, Signal.SIGKILL,
            Signal.SIGSTOP, Signal.SIGCONT, Signal.SIGCHLD
        };
        
        for (int sig : signals) {
            String name = Signal.getSignalName(sig);
            Signal.SignalAction action = Signal.getDefaultAction(sig);
            String catchable = (sig == Signal.SIGKILL || sig == Signal.SIGSTOP) ? "No" : "Yes";
            console.println(String.format("    %2d %-10s -> %-15s (catchable: %s)", 
                sig, name, action, catchable));
        }
        
        // 演示信号处理
        console.println("\n  Demonstrating signal handling:");
        console.println("    Setting SIGINT handler to SIG_IGN...");
        long oldHandler = libc.signal(Signal.SIGINT, Signal.SIG_IGN);
        console.println("    ✓ Old handler: " + oldHandler);
        
        console.println("    Sending SIGINT to self...");
        int ret = libc.kill(pid, Signal.SIGINT);
        console.println("    ✓ kill() returned: " + ret);
        
        // 处理信号
        syscall.processSignals(kernel.getScheduler().getCurrentTask());
        console.println("    ✓ Process still running (signal was ignored)");
        
        // 恢复默认处理
        libc.signal(Signal.SIGINT, Signal.SIG_DFL);
        console.println("    ✓ Restored default handler");
        
        console.println("\n  Signal Features:");
        console.println("    ✓ 32 standard Unix signals");
        console.println("    ✓ Signal handlers: SIG_DFL, SIG_IGN, custom");
        console.println("    ✓ Signal delivery: kill() syscall");
        console.println("    ✓ Signal masking: Block/unblock signals");
        console.println("    ✓ Protected signals: SIGKILL, SIGSTOP");
        
        console.println("");
    }
    
    /**
     * 演示系统调用性能
     */
    public static void demonstrateSyscallPerformance(Kernel kernel) {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        LibC libc = new LibC(syscall);
        
        console.println("\n═══════════════════════════════════════════════════════════");
        console.println("  [7/7] System Call Performance Demonstration");
        console.println("═══════════════════════════════════════════════════════════\n");
        
        console.println("  Benchmarking system calls (1000 iterations each):\n");
        
        // getpid 性能测试
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            libc.getpid();
        }
        long getpidTime = System.nanoTime() - start;
        console.println("    getpid():  " + (getpidTime / 1000) + " ns/op");
        
        // time 性能测试
        start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            libc.time();
        }
        long timeTime = System.nanoTime() - start;
        console.println("    time():    " + (timeTime / 1000) + " ns/op");
        
        // brk 性能测试
        start = System.nanoTime();
        long brk = libc.brk(0x20000);
        for (int i = 0; i < 100; i++) {
            libc.brk(brk + i * 0x1000);
        }
        long brkTime = System.nanoTime() - start;
        console.println("    brk():     " + (brkTime / 100) + " ns/op");
        
        console.println("\n  Available System Calls:");
        console.println("    Process:   fork, exit, wait, getpid, getppid, pause");
        console.println("    File I/O:  open, close, read, write, lseek, creat, unlink");
        console.println("    Directory: chdir, mkdir, rmdir");
        console.println("    IPC:       pipe");
        console.println("    Signal:    signal, kill");
        console.println("    Memory:    brk");
        console.println("    Time:      time");
        console.println("    Other:     sync");
        console.println("    Total:     22+ system calls");
        
        console.println("");
    }
}

