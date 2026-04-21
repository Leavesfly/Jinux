package jinux.demo;

import jinux.kernel.Kernel;
import jinux.kernel.Task;
import jinux.kernel.Scheduler;
import jinux.mm.MemoryManager;
import jinux.drivers.ConsoleDevice;
import jinux.include.Const;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Jinux 操作系统 ASCII 可视化工具
 * 用于教学演示，展示内核各子系统的运行状态
 * 
 * @author Jinux Project
 */
public class Visualizer {
    
    // ==================== ANSI 颜色常量 ====================
    
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_YELLOW = "\033[33m";
    private static final String ANSI_BLUE = "\033[34m";
    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_BOLD = "\033[1m";
    
    /**
     * 可视化进程状态转换图
     * 展示所有进程状态及转换条件，并列出当前进程表
     */
    public static void visualizeProcessStates(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        Scheduler scheduler = kernel.getScheduler();
        
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║          Process State Transition Diagram           ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        // 绘制状态转换图
        console.println("\n  ┌──────────┐");
        console.println("  │ CREATED  │");
        console.println("  └─────┬────┘");
        console.println("        │ fork()");
        console.println("        ▼");
        console.println("  ┌──────────┐");
        console.println("  │  READY   │◄──────────────────────┐");
        console.println("  └─────┬────┘                       │");
        console.println("        │ schedule()                 │ signal()/wakeUp()");
        console.println("        ▼                            │");
        console.println("  ┌──────────┐      wait()/exit()    │");
        console.println("  │ RUNNING  │──────────────────────►│");
        console.println("  └─────┬────┘                       │");
        console.println("        │ I/O, sleep()               │");
        console.println("        ▼                            │");
        console.println("  ┌──────────┐                       │");
        console.println("  │ WAITING  │───────────────────────┘");
        console.println("  └─────┬────┘");
        console.println("        │ exit()");
        console.println("        ▼");
        console.println("  ┌──────────┐");
        console.println("  │  ZOMBIE  │");
        console.println("  └─────┬────┘");
        console.println("        │ wait() by parent");
        console.println("        ▼");
        console.println("  ┌──────────┐");
        console.println("  │ STOPPED  │ (signal: SIGSTOP/SIGCONT)");
        console.println("  └──────────┘");
        
        // 显示当前进程表
        console.println("\n" + ANSI_BOLD + "Current Process Table:" + ANSI_RESET);
        console.println(String.format("%-6s %-6s %-15s %-8s %-8s", 
            "PID", "PPID", "STATE", "COUNTER", "PRIORITY"));
        console.println("------------------------------------------------------");
        
        Task[] tasks = scheduler.getTaskTable();
        for (Task task : tasks) {
            if (task != null) {
                String stateColor = getStateColor(task.getState());
                String stateName = task.getStateName();
                
                console.print(String.format("%-6d %-6d ", task.getPid(), task.getPpid()));
                console.print(stateColor + String.format("%-15s", stateName) + ANSI_RESET);
                console.println(String.format(" %-8d %-8d", task.getCounter(), task.getPriority()));
            }
        }
        
        console.println("\n" + ANSI_CYAN + "Legend:" + ANSI_RESET);
        console.println("  " + ANSI_GREEN + "RUNNING" + ANSI_RESET + "    - Currently executing or ready to run");
        console.println("  " + ANSI_YELLOW + "READY" + ANSI_RESET + "      - Waiting for CPU (INTERRUPTIBLE)");
        console.println("  " + ANSI_BLUE + "WAITING" + ANSI_RESET + "    - Blocked on I/O or event (UNINTERRUPTIBLE)");
        console.println("  " + ANSI_RED + "ZOMBIE" + ANSI_RESET + "     - Terminated, waiting for parent");
        console.println("  " + ANSI_RESET + "STOPPED" + ANSI_RESET + "    - Stopped by signal");
    }
    
    /**
     * 获取状态对应的颜色
     */
    private static String getStateColor(int state) {
        switch (state) {
            case Const.TASK_RUNNING:
                return ANSI_GREEN;
            case Const.TASK_INTERRUPTIBLE:
                return ANSI_YELLOW;
            case Const.TASK_UNINTERRUPTIBLE:
                return ANSI_BLUE;
            case Const.TASK_ZOMBIE:
                return ANSI_RED;
            case Const.TASK_STOPPED:
                return ANSI_RESET;
            default:
                return ANSI_RESET;
        }
    }
    
    /**
     * 可视化内存布局
     * 展示物理内存使用情况和虚拟地址空间布局
     */
    public static void visualizeMemoryLayout(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        MemoryManager mm = kernel.getMemoryManager();
        
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║              Memory Layout Visualization            ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        // 物理内存使用情况
        int totalPages = mm.getPhysicalMemory().getTotalPages();
        int freePages = mm.getPhysicalMemory().getFreePages();
        int usedPages = totalPages - freePages;
        double usagePercent = (double) usedPages / totalPages * 100;
        
        console.println("\n" + ANSI_BOLD + "Physical Memory Usage:" + ANSI_RESET);
        console.println(String.format("  Total Pages: %d (%d MB)", totalPages, 
            (totalPages * Const.PAGE_SIZE) / 1024 / 1024));
        console.println(String.format("  Used Pages:  %d (%d MB)", usedPages, 
            (usedPages * Const.PAGE_SIZE) / 1024 / 1024));
        console.println(String.format("  Free Pages:  %d (%d MB)", freePages, 
            (freePages * Const.PAGE_SIZE) / 1024 / 1024));
        
        // 绘制进度条（宽度50字符）
        int barWidth = 50;
        int filledBars = (int) (usagePercent / 100 * barWidth);
        int emptyBars = barWidth - filledBars;
        
        console.print("  [");
        console.print(ANSI_GREEN);
        for (int i = 0; i < filledBars; i++) {
            console.print("█");
        }
        console.print(ANSI_RESET);
        console.print(ANSI_YELLOW);
        for (int i = 0; i < emptyBars; i++) {
            console.print("░");
        }
        console.print(ANSI_RESET);
        console.println(String.format("] %.1f%%", usagePercent));
        
        // 虚拟地址空间布局
        console.println("\n" + ANSI_BOLD + "Virtual Address Space Layout (per process):" + ANSI_RESET);
        console.println("  ┌─────────────────────────┐ 0xFFFFFFFF");
        console.println("  │                         │");
        console.println("  │       Stack             │ ← Grows downward");
        console.println("  │    (High addresses)     │");
        console.println("  ├─────────────────────────┤");
        console.println("  │                         │");
        console.println("  │         ↓               │");
        console.println("  │      (Gap)              │");
        console.println("  │         ↑               │");
        console.println("  ├─────────────────────────┤");
        console.println("  │                         │");
        console.println("  │         Heap            │ ← Grows upward (brk/sbrk)");
        console.println("  ├─────────────────────────┤");
        console.println("  │                         │");
        console.println("  │      Data Segment       │ (Initialized & BSS)");
        console.println("  ├─────────────────────────┤");
        console.println("  │                         │");
        console.println("  │      Code Segment       │ (.text, read-only)");
        console.println("  │    (Low addresses)      │");
        console.println("  └─────────────────────────┘ 0x00000000");
        
        console.println("\n" + ANSI_CYAN + "Note:" + ANSI_RESET);
        console.println("  - Each process has its own virtual address space");
        console.println("  - Virtual addresses are mapped to physical pages via page tables");
        console.println("  - Kernel occupies low 1MB (0x00000000 - 0x000FFFFF)");
    }
    
    /**
     * 可视化调度时间线
     * 模拟展示进程调度的时间线过程
     */
    public static void visualizeSchedulingTimeline(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        Scheduler scheduler = kernel.getScheduler();
        
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║          Scheduling Timeline Simulation             ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        // 创建模拟进程
        List<Map<String, Object>> simProcesses = new ArrayList<>();
        String[] colors = {ANSI_GREEN, ANSI_YELLOW, ANSI_BLUE, ANSI_CYAN};
        String[] names = {"Init", "Shell", "Editor", "Compiler"};
        
        for (int i = 0; i < 4; i++) {
            Map<String, Object> proc = new HashMap<>();
            proc.put("name", names[i]);
            proc.put("priority", 15 - i * 2);
            proc.put("counter", 10 - i);
            proc.put("color", colors[i]);
            simProcesses.add(proc);
        }
        
        console.println("\nSimulated Processes:");
        console.println(String.format("%-10s %-10s %-10s", "Name", "Priority", "Counter"));
        console.println("----------------------------------------");
        for (Map<String, Object> proc : simProcesses) {
            console.print((String) proc.get("color"));
            console.println(String.format("%-10s %-10d %-10d" + ANSI_RESET, 
                proc.get("name"), proc.get("priority"), proc.get("counter")));
        }
        
        // 模拟8个时间片的调度
        console.println("\n" + ANSI_BOLD + "Scheduling Timeline (8 time slices):" + ANSI_RESET);
        console.println("Time | Process Execution");
        console.println("-----+--------------------------------------------------");
        
        int currentTime = 0;
        int currentProcIndex = 0;
        
        for (int slice = 0; slice < 8; slice++) {
            Map<String, Object> currentProc = simProcesses.get(currentProcIndex);
            
            console.print(String.format("  %d  | ", currentTime));
            console.print((String) currentProc.get("color"));
            console.print("[████] " + currentProc.get("name"));
            console.print(ANSI_RESET);
            
            // 显示其他进程等待
            for (int i = 0; i < 4; i++) {
                if (i != currentProcIndex) {
                    console.print(" [    ]");
                }
            }
            console.println("");
            
            // 更新计数器
            int counter = (Integer) currentProc.get("counter");
            counter--;
            currentProc.put("counter", counter);
            
            // 如果计数器用完，重新分配
            if (counter <= 0) {
                currentProc.put("counter", (Integer) currentProc.get("priority"));
                console.println("     | ^ Counter expired, reset to priority");
            }
            
            // 切换到下一个进程（简单轮转）
            currentProcIndex = (currentProcIndex + 1) % 4;
            currentTime++;
        }
        
        console.println("\n" + ANSI_CYAN + "Scheduling Algorithm:" + ANSI_RESET);
        console.println("  - Round-robin with priority-based time slices");
        console.println("  - Each process gets counter ticks based on priority");
        console.println("  - When counter reaches 0, recalculate: counter = counter/2 + priority");
    }
    
    /**
     * 可视化文件系统树
     * 展示文件系统的目录树结构
     */
    public static void visualizeFileSystemTree(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║            File System Tree Structure               ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        console.println("\n" + ANSI_BLUE + "/" + ANSI_RESET + " (root)");
        console.println("├── " + ANSI_BLUE + "etc/" + ANSI_RESET);
        console.println("│   ├── passwd");
        console.println("│   ├── group");
        console.println("│   └── fstab");
        console.println("├── " + ANSI_BLUE + "dev/" + ANSI_RESET);
        console.println("│   ├── " + ANSI_YELLOW + "console" + ANSI_RESET + " (char device)");
        console.println("│   ├── " + ANSI_YELLOW + "tty0" + ANSI_RESET + " (char device)");
        console.println("│   └── " + ANSI_YELLOW + "null" + ANSI_RESET + " (char device)");
        console.println("├── " + ANSI_BLUE + "tmp/" + ANSI_RESET);
        console.println("│   └── .X11-unix/");
        console.println("├── " + ANSI_BLUE + "home/" + ANSI_RESET);
        console.println("│   └── " + ANSI_BLUE + "user/" + ANSI_RESET);
        console.println("│       ├── .bashrc");
        console.println("│       └── " + ANSI_BLUE + "projects/" + ANSI_RESET);
        console.println("├── " + ANSI_BLUE + "bin/" + ANSI_RESET);
        console.println("│   ├── sh");
        console.println("│   ├── ls");
        console.println("│   └── cat");
        console.println("└── " + ANSI_BLUE + "usr/" + ANSI_RESET);
        console.println("    ├── " + ANSI_BLUE + "bin/" + ANSI_RESET);
        console.println("    └── " + ANSI_BLUE + "lib/" + ANSI_RESET);
        
        console.println("\n" + ANSI_CYAN + "Legend:" + ANSI_RESET);
        console.println("  " + ANSI_BLUE + "Blue" + ANSI_RESET + "   - Directories");
        console.println("  " + ANSI_GREEN + "Green" + ANSI_RESET + "  - Regular files");
        console.println("  " + ANSI_YELLOW + "Yellow" + ANSI_RESET + " - Device files (character/block)");
    }
    
    /**
     * 可视化管道数据流
     * 展示进程间通过管道通信的数据流动
     */
    public static void visualizePipeDataFlow(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║              Pipe Data Flow Visualization           ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        console.println("\n" + ANSI_BOLD + "Pipe Communication Model:" + ANSI_RESET);
        console.println("");
        console.println("  ┌──────────────┐         ┌──────────────────┐         ┌──────────────┐");
        console.println("  │ Write Process│         │  Pipe Buffer     │         │ Read Process │");
        console.println("  │   (PID: 2)   │────────►│  (4096 bytes)    │────────►│   (PID: 3)   │");
        console.println("  └──────────────┘  write() └──────────────────┘  read() └──────────────┘");
        console.println("");
        
        // 展示环形缓冲区
        console.println(ANSI_BOLD + "Ring Buffer Structure:" + ANSI_RESET);
        console.println("");
        console.println("  Buffer: [D][D][D][ ][ ][ ][ ][W][R][ ][ ][ ][ ][ ][ ][ ]");
        console.println("           0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15");
        console.println("");
        console.println("  D = Data written but not yet read");
        console.println("  W = Write pointer (next write position)");
        console.println("  R = Read pointer (next read position)");
        console.println("");
        
        console.println(ANSI_BOLD + "Pipe Characteristics:" + ANSI_RESET);
        console.println("  - Capacity: 4096 bytes (one page)");
        console.println("  - Unidirectional: one writer, one reader");
        console.println("  - FIFO order: First In, First Out");
        console.println("  - Blocking: read() blocks if empty, write() blocks if full");
        console.println("  - Implemented as: circular buffer in kernel memory");
        console.println("");
        
        console.println(ANSI_BOLD + "Data Flow Example:" + ANSI_RESET);
        console.println("  1. Writer calls write(pipe_fd, data, len)");
        console.println("  2. Kernel copies data to pipe buffer at write_ptr");
        console.println("  3. Write pointer advances: write_ptr = (write_ptr + len) % 4096");
        console.println("  4. Reader calls read(pipe_fd, buf, bufsize)");
        console.println("  5. Kernel copies data from read_ptr to user buffer");
        console.println("  6. Read pointer advances: read_ptr = (read_ptr + len) % 4096");
    }
    
    /**
     * 可视化系统调用流程
     * 展示从用户态到内核态的完整调用链
     */
    public static void visualizeSyscallFlow(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║          System Call Flow (fork example)            ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        console.println("\n" + ANSI_BOLD + "User Space" + ANSI_RESET);
        console.println("┌─────────────────────────────────────────────────────┐");
        console.println("│                                                     │");
        console.println("│  Application Code:                                  │");
        console.println("│    pid = fork();                                    │");
        console.println("│         │                                           │");
        console.println("│         ▼                                           │");
        console.println("│  LibC Wrapper:                                      │");
        console.println("│    long fork() {                                    │");
        console.println("│      return syscall(SYS_fork, 0, 0, 0);             │");
        console.println("│    }                                                │");
        console.println("│         │                                           │");
        console.println("│         ▼                                           │");
        console.println("│  Assembly Trap:                                     │");
        console.println("│    int $0x80  ; Software interrupt                  │");
        console.println("│    eax = SYS_fork (system call number)              │");
        console.println("│                                                     │");
        console.println("├═════════════════════════════════════════════════════┤");
        console.println("│  ⚡ TRAP TO KERNEL MODE ⚡                           │");
        console.println("├═════════════════════════════════════════════════════┤");
        console.println("│                                                     │");
        console.println("│  Kernel Space:                                      │");
        console.println("│         │                                           │");
        console.println("│         ▼                                           │");
        console.println("│  Interrupt Handler:                                 │");
        console.println("│    system_call()                                    │");
        console.println("│      - Save registers                               │");
        console.println("│      - Switch to kernel stack                       │");
        console.println("│         │                                           │");
        console.println("│         ▼                                           │");
        console.println("│  System Call Dispatcher:                            │");
        console.println("│    syscallDispatcher.dispatch(eax, ebx, ecx, edx)   │");
        console.println("│      - Validate syscall number                      │");
        console.println("│      - Lookup handler table                         │");
        console.println("│         │                                           │");
        console.println("│         ▼                                           │");
        console.println("│  ProcessSyscalls.sysFork():                         │");
        console.println("│    - Allocate new PID                               │");
        console.println("│    - Create new task_struct                         │");
        console.println("│    - Copy parent's address space (COW)              │");
        console.println("│    - Copy file descriptor table                     │");
        console.println("│    - Set child state to READY                       │");
        console.println("│         │                                           │");
        console.println("│         ▼                                           │");
        console.println("│  Return to User Space:                              │");
        console.println("│    - Restore registers                              │");
        console.println("│    - Return PID to parent, 0 to child               │");
        console.println("│                                                     │");
        console.println("└─────────────────────────────────────────────────────┘");
        
        console.println("\n" + ANSI_CYAN + "Key Points:" + ANSI_RESET);
        console.println("  - System calls are the ONLY way for user programs to access kernel");
        console.println("  - Context switch involves saving/restoring CPU state");
        console.println("  - fork() uses Copy-On-Write for efficiency");
        console.println("  - Return value differs: parent gets child PID, child gets 0");
    }
    
    /**
     * 可视化信号流程
     * 展示信号从发送到处理的完整过程
     */
    public static void visualizeSignalFlow(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║              Signal Handling Flow                   ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        console.println("\n" + ANSI_BOLD + "Signal Lifecycle:" + ANSI_RESET);
        console.println("");
        console.println("  1. SEND: kill(pid, SIGUSR1)");
        console.println("     │");
        console.println("     ▼");
        console.println("  2. SET: target_task.signalPending |= (1 << SIGUSR1)");
        console.println("     │");
        console.println("     ▼");
        console.println("  3. CHECK: On next schedule/return from syscall");
        console.println("     │   if (task.hasPendingSignals()) { ... }");
        console.println("     │");
        console.println("     ▼");
        console.println("  4. HANDLE: Execute signal handler");
        console.println("     ├─ SIG_DFL  → Default action (terminate/ignore/etc)");
        console.println("     ├─ SIG_IGN  → Ignore signal");
        console.println("     └─ Custom   → Call user-defined handler");
        
        // 展示信号位图
        console.println("\n" + ANSI_BOLD + "Signal Bitmap (32 bits):" + ANSI_RESET);
        console.println("");
        console.println("  Bit:  31 30 29 ...  3  2  1  0");
        console.println("        ┌──┬──┬──┬───┬──┬──┬──┐");
        console.println("        │  │  │  │...│  │  │  │");
        console.println("        └──┴──┴──┴───┴──┴──┴──┘");
        console.println("         ↑              ↑  ↑  ↑");
        console.println("       Unused      SIGQUIT|  |");
        console.println("                      SIGINT|  ");
        console.println("                         SIGHUP");
        console.println("");
        console.println("  Common Signals:");
        console.println("    Bit 1:  SIGHUP  (Hangup)");
        console.println("    Bit 2:  SIGINT  (Interrupt, Ctrl+C)");
        console.println("    Bit 3:  SIGQUIT (Quit, Ctrl+\\)");
        console.println("    Bit 9:  SIGKILL (Kill, cannot be caught/ignored)");
        console.println("    Bit 10: SIGUSR1 (User-defined)");
        console.println("    Bit 12: SIGUSR2 (User-defined)");
        
        console.println("\n" + ANSI_CYAN + "Signal Properties:" + ANSI_RESET);
        console.println("  - Asynchronous: Can arrive at any time");
        console.println("  - Pending: Stored in signal bitmap until handled");
        console.println("  - Blocked: Can be masked using signalBlocked bitmap");
        console.println("  - SIGKILL/SIGSTOP: Cannot be blocked, caught, or ignored");
    }
    
    /**
     * 显示综合仪表盘
     * 在一个视图中展示系统整体状态
     */
    public static void showDashboard(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        Scheduler scheduler = kernel.getScheduler();
        MemoryManager mm = kernel.getMemoryManager();
        
        console.println("\n" + ANSI_BOLD + ANSI_CYAN + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "║              Jinux System Dashboard                 ║" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        // 系统运行时间
        long uptime = scheduler.getJiffies();
        long seconds = uptime / Const.HZ;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║  System Uptime: " + String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60) + 
            " (" + uptime + " jiffies)" + ANSI_RESET);
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        // 进程统计
        int totalTasks = 0;
        int runningTasks = 0;
        int waitingTasks = 0;
        int zombieTasks = 0;
        
        Task[] tasks = scheduler.getTaskTable();
        for (Task task : tasks) {
            if (task != null) {
                totalTasks++;
                switch (task.getState()) {
                    case Const.TASK_RUNNING:
                        runningTasks++;
                        break;
                    case Const.TASK_INTERRUPTIBLE:
                    case Const.TASK_UNINTERRUPTIBLE:
                        waitingTasks++;
                        break;
                    case Const.TASK_ZOMBIE:
                        zombieTasks++;
                        break;
                }
            }
        }
        
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║  Process Statistics                                   ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╠══════════════════════════════════════════════════════════╣" + ANSI_RESET);
        console.println(String.format("║  Total: %-4d  Running: %-3d  Waiting: %-3d  Zombie: %-3d  ║",
            totalTasks, runningTasks, waitingTasks, zombieTasks));
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        // 内存使用率
        int totalPages = mm.getPhysicalMemory().getTotalPages();
        int freePages = mm.getPhysicalMemory().getFreePages();
        int usedPages = totalPages - freePages;
        double usagePercent = (double) usedPages / totalPages * 100;
        
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║  Memory Usage                                         ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╠══════════════════════════════════════════════════════════╣" + ANSI_RESET);
        
        int barWidth = 40;
        int filledBars = (int) (usagePercent / 100 * barWidth);
        int emptyBars = barWidth - filledBars;
        
        console.print("║  ");
        console.print(ANSI_GREEN);
        for (int i = 0; i < filledBars; i++) {
            console.print("█");
        }
        console.print(ANSI_RESET);
        console.print(ANSI_YELLOW);
        for (int i = 0; i < emptyBars; i++) {
            console.print("░");
        }
        console.print(ANSI_RESET);
        console.println(String.format("  ║"));
        console.println(String.format("║  %.1f%% used (%d/%d pages)                          ║",
            usagePercent, usedPages, totalPages));
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        // 文件系统状态
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║  File System Status                                   ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╠══════════════════════════════════════════════════════════╣" + ANSI_RESET);
        console.println("║  Root filesystem: Mounted                             ║");
        console.println("║  Device: /dev/hda1 (virtual)                          ║");
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        // 最近的系统调用（模拟）
        console.println("\n" + ANSI_BOLD + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + "║  Recent System Calls                                  ║" + ANSI_RESET);
        console.println(ANSI_BOLD + "╠══════════════════════════════════════════════════════════╣" + ANSI_RESET);
        console.println("║  [OK] getpid()                                       ║");
        console.println("║  [OK] brk(0x10000)                                   ║");
        console.println("║  [OK] write(STDOUT, \"Hello\", 5)                     ║");
        console.println(ANSI_BOLD + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        
        console.println("");
    }
    
    /**
     * 显示可视化菜单
     * 让用户选择要查看的可视化内容
     */
    public static void showVisualizationMenu(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        while (true) {
            console.println("\n" + ANSI_BOLD + ANSI_CYAN + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
            console.println(ANSI_BOLD + ANSI_CYAN + "║         Jinux Visualization Menu                    ║" + ANSI_RESET);
            console.println(ANSI_BOLD + ANSI_CYAN + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
            
            console.println("\n  1. Process States & Transitions");
            console.println("  2. Memory Layout");
            console.println("  3. Scheduling Timeline");
            console.println("  4. File System Tree");
            console.println("  5. Pipe Data Flow");
            console.println("  6. System Call Flow");
            console.println("  7. Signal Handling Flow");
            console.println("  8. System Dashboard");
            console.println("  0. Exit");
            
            console.print("\n" + ANSI_BOLD + "Select option (0-8): " + ANSI_RESET);
            
            try {
                String input = reader.readLine();
                if (input == null || input.isEmpty()) {
                    continue;
                }
                
                int choice = Integer.parseInt(input.trim());
                
                switch (choice) {
                    case 1:
                        visualizeProcessStates(kernel);
                        break;
                    case 2:
                        visualizeMemoryLayout(kernel);
                        break;
                    case 3:
                        visualizeSchedulingTimeline(kernel);
                        break;
                    case 4:
                        visualizeFileSystemTree(kernel);
                        break;
                    case 5:
                        visualizePipeDataFlow(kernel);
                        break;
                    case 6:
                        visualizeSyscallFlow(kernel);
                        break;
                    case 7:
                        visualizeSignalFlow(kernel);
                        break;
                    case 8:
                        showDashboard(kernel);
                        break;
                    case 0:
                        console.println("\nExiting visualization menu. Goodbye!");
                        return;
                    default:
                        console.println(ANSI_RED + "\nInvalid option. Please try again." + ANSI_RESET);
                }
                
            } catch (NumberFormatException e) {
                console.println(ANSI_RED + "\nInvalid input. Please enter a number." + ANSI_RESET);
            } catch (Exception e) {
                console.println(ANSI_RED + "\nError reading input: " + e.getMessage() + ANSI_RESET);
            }
        }
    }
}
