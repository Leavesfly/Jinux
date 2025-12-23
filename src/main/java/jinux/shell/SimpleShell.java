package jinux.shell;

import jinux.drivers.ConsoleDevice;
import jinux.kernel.Kernel;
import jinux.kernel.Scheduler;
import jinux.kernel.Signal;
import jinux.kernel.Task;
import jinux.lib.LibC;
import jinux.mm.MemoryManager;
import jinux.exec.ProgramLoader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * SimpleShell - Jinux 简单命令行解释器
 * 
 * 实现基本的内置命令，提供交互式命令行界面。
 * 由于缺少 execve 系统调用，暂时只支持内置命令。
 */
public class SimpleShell {
    
    private final Kernel kernel;
    private final ConsoleDevice console;
    private final Scheduler scheduler;
    private final MemoryManager memoryManager;
    private final LibC libc;
    private boolean running;
    
    /** Shell 提示符 */
    private static final String PROMPT = "jinux$ ";
    
    /** Shell 版本 */
    private static final String VERSION = "0.0.1-alpha";
    
    public SimpleShell(Kernel kernel) {
        this.kernel = kernel;
        this.console = kernel.getConsole();
        this.scheduler = kernel.getScheduler();
        this.memoryManager = kernel.getMemoryManager();
        this.libc = new LibC(kernel.getSyscallDispatcher());
        this.running = true;
    }
    
    /**
     * 启动 Shell
     */
    public void run() {
        printBanner();
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        while (running) {
            try {
                // 显示提示符
                console.print(PROMPT);
                
                // 读取用户输入
                String line = reader.readLine();
                if (line == null) {
                    break; // EOF
                }
                
                // 处理命令
                line = line.trim();
                if (!line.isEmpty()) {
                    executeCommand(line);
                }
                
            } catch (Exception e) {
                console.println("[SHELL] Error: " + e.getMessage());
            }
        }
        
        console.println("\n[SHELL] Goodbye!");
    }
    
    /**
     * 打印 Shell 欢迎信息
     */
    private void printBanner() {
        console.println("\n========================================");
        console.println("   Jinux Simple Shell v" + VERSION);
        console.println("========================================");
        console.println("Type 'help' for available commands");
        console.println("Type 'exit' to quit\n");
    }
    
    /**
     * 执行命令
     */
    private void executeCommand(String line) {
        // 解析命令行
        String[] parts = parseCommandLine(line);
        if (parts.length == 0) {
            return;
        }
        
        String command = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        
        // 执行内置命令
        switch (command) {
            case "help":
                cmdHelp(args);
                break;
            case "ps":
                cmdPs(args);
                break;
            case "mem":
                cmdMem(args);
                break;
            case "time":
                cmdTime(args);
                break;
            case "signal":
            case "kill":
                cmdSignal(args);
                break;
            case "demo":
                cmdDemo(args);
                break;
            case "run":
                cmdRun(args);
                break;
            case "programs":
                cmdPrograms(args);
                break;
            case "echo":
                cmdEcho(args);
                break;
            case "clear":
                cmdClear(args);
                break;
            case "uptime":
                cmdUptime(args);
                break;
            case "version":
                cmdVersion(args);
                break;
            case "exit":
            case "quit":
                cmdExit(args);
                break;
            default:
                console.println(command + ": command not found");
                console.println("Type 'help' for available commands");
                break;
        }
    }
    
    /**
     * 解析命令行（简单空格分割）
     */
    private String[] parseCommandLine(String line) {
        List<String> tokens = new ArrayList<>();
        String[] parts = line.split("\\s+");
        for (String part : parts) {
            if (!part.isEmpty()) {
                tokens.add(part);
            }
        }
        return tokens.toArray(new String[0]);
    }
    
    // ==================== 内置命令实现 ====================
    
    /**
     * help - 显示帮助信息
     */
    private void cmdHelp(String[] args) {
        console.println("\nAvailable commands:");
        console.println("  help          - Show this help message");
        console.println("  ps            - List all processes");
        console.println("  mem           - Show memory statistics");
        console.println("  time          - Show current system time");
        console.println("  signal <pid> <signum> - Send signal to process");
        console.println("  kill <pid> <signum>   - Alias for signal");
        console.println("  demo [type]   - Run demonstrations");
        console.println("                  Types: signal, pipe, libc, all");
        console.println("  run <prog>    - Run a program (demo)");
        console.println("  programs      - List available programs");
        console.println("  echo <text>   - Print text to console");
        console.println("  clear         - Clear screen");
        console.println("  uptime        - Show system uptime");
        console.println("  version       - Show Jinux version");
        console.println("  exit          - Exit shell");
        console.println("");
    }
    
    /**
     * ps - 显示进程列表
     */
    private void cmdPs(String[] args) {
        console.println("\nPID\tSTATE\t\tPRIORITY\tCOUNTER");
        console.println("----------------------------------------------------");
        
        // 获取当前任务（简化版本，只显示当前任务）
        Task currentTask = scheduler.getCurrentTask();
        if (currentTask != null) {
            console.println(String.format("%d\t%-12s\t%d\t\t%d",
                currentTask.getPid(),
                currentTask.getState(),
                currentTask.getPriority(),
                currentTask.getCounter()
            ));
        } else {
            console.println("No running tasks");
        }
        console.println("");
    }
    
    /**
     * mem - 显示内存统计
     */
    private void cmdMem(String[] args) {
        console.println("");
        memoryManager.printStats();
        console.println("");
    }
    
    /**
     * time - 显示系统时间
     */
    private void cmdTime(String[] args) {
        long time = libc.time();
        console.println("\nSystem time: " + time + " seconds since epoch");
        console.println("Date: " + new java.util.Date(time * 1000));
        console.println("");
    }
    
    /**
     * signal/kill - 发送信号到进程
     */
    private void cmdSignal(String[] args) {
        if (args.length < 2) {
            console.println("Usage: signal <pid> <signum>");
            console.println("       kill <pid> <signum>");
            console.println("\nCommon signals:");
            console.println("  1  SIGHUP    - Hangup");
            console.println("  2  SIGINT    - Interrupt");
            console.println("  9  SIGKILL   - Kill (cannot be caught)");
            console.println("  15 SIGTERM   - Terminate");
            console.println("  17 SIGCHLD   - Child status changed");
            console.println("");
            return;
        }
        
        try {
            int pid = Integer.parseInt(args[0]);
            int signum = Integer.parseInt(args[1]);
            
            int ret = libc.kill(pid, signum);
            if (ret == 0) {
                console.println("Signal " + signum + " sent to process " + pid);
            } else {
                console.println("Failed to send signal (error: " + ret + ")");
            }
        } catch (NumberFormatException e) {
            console.println("Error: PID and signal number must be integers");
        }
        console.println("");
    }
    
    /**
     * demo - 运行演示
     */
    private void cmdDemo(String[] args) {
        String type = args.length > 0 ? args[0] : "all";
        
        switch (type) {
            case "signal":
                runSignalDemo();
                break;
            case "pipe":
                runPipeDemo();
                break;
            case "libc":
                runLibCDemo();
                break;
            case "all":
                runSignalDemo();
                runPipeDemo();
                runLibCDemo();
                break;
            default:
                console.println("Unknown demo type: " + type);
                console.println("Available types: signal, pipe, libc, all");
                break;
        }
        console.println("");
    }
    
    /**
     * run - 运行程序
     */
    private void cmdRun(String[] args) {
        if (args.length < 1) {
            console.println("Usage: run <program> [args...]");
            console.println("Example: run hello");
            console.println("Type 'programs' to list available programs");
            console.println("");
            return;
        }
        
        String program = args[0];
        console.println("\nRunning program: " + program);
        console.println("Note: This is a demonstration of program execution");
        console.println("In a full implementation, the program would:");
        console.println("  1. Be loaded using execve()");
        console.println("  2. Replace current process");
        console.println("  3. Execute with new code\n");
        
        // 演示程序执行（简化版）
        console.println("[Simulated execution of '" + program + "']");
        console.println("");
    }
    
    /**
     * programs - 列出可用程序
     */
    private void cmdPrograms(String[] args) {
        console.println("");
        ProgramLoader.listPrograms();
    }
    
    /**
     * echo - 打印文本
     */
    private void cmdEcho(String[] args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(args[i]);
        }
        console.println(sb.toString());
    }
    
    /**
     * clear - 清屏
     */
    private void cmdClear(String[] args) {
        // 简单实现：打印多个换行
        for (int i = 0; i < 50; i++) {
            console.println("");
        }
    }
    
    /**
     * uptime - 显示系统运行时间
     */
    private void cmdUptime(String[] args) {
        long currentTime = libc.time();
        // 简化：假设系统启动时间为 currentTime - 100
        long uptime = 100; // 秒
        
        console.println("\nSystem uptime: " + uptime + " seconds");
        console.println("  " + (uptime / 3600) + " hours, " + 
                       ((uptime % 3600) / 60) + " minutes, " + 
                       (uptime % 60) + " seconds");
        console.println("");
    }
    
    /**
     * version - 显示版本信息
     */
    private void cmdVersion(String[] args) {
        console.println("\nJinux Operating System");
        console.println("  Version: 0.01-alpha");
        console.println("  Shell Version: " + VERSION);
        console.println("  Java Implementation of Linux 0.01");
        console.println("");
    }
    
    /**
     * exit - 退出 Shell
     */
    private void cmdExit(String[] args) {
        console.println("Exiting shell...");
        running = false;
    }
    
    // ==================== 演示方法 ====================
    
    private void runSignalDemo() {
        try {
            console.println("\n[DEMO] Signal mechanism demonstration:");
            
            int pid = libc.getpid();
            console.println("  Current PID: " + pid);
            
            // 设置 SIGINT 处理器为忽略
            console.println("  Setting SIGINT handler to SIG_IGN...");
            libc.signal(Signal.SIGINT, Signal.SIG_IGN);
            
            console.println("  Signal demo complete");
            
        } catch (Exception e) {
            console.println("  Error in signal demo: " + e.getMessage());
        }
    }
    
    private void runPipeDemo() {
        console.println("\n[DEMO] Pipe mechanism demonstration:");
        console.println("  Pipe demo would create communication channel");
        console.println("  between parent and child processes");
        console.println("  (Full implementation requires fork support)");
    }
    
    private void runLibCDemo() {
        console.println("\n[DEMO] LibC demonstration:");
        
        int pid = libc.getpid();
        console.println("  getpid() = " + pid);
        
        long time = libc.time();
        console.println("  time() = " + time);
        
        console.println("  LibC demo complete");
    }
}
