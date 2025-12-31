package jinux.shell;

import jinux.drivers.ConsoleDevice;
import jinux.fs.File;
import jinux.kernel.Kernel;
import jinux.kernel.Scheduler;
import jinux.kernel.Signal;
import jinux.kernel.Task;
import jinux.lib.LibC;
import jinux.mm.MemoryManager;
import jinux.exec.ProgramLoader;
import jinux.demo.SystemCapabilitiesDemo;
import jinux.demo.InteractiveDemo;

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
        // 检查是否有后台执行标记
        boolean background = false;
        if (line.trim().endsWith("&")) {
            background = true;
            line = line.trim().substring(0, line.trim().length() - 1).trim();
        }
        
        // 检查是否有管道
        if (line.contains("|")) {
            executePipe(line, background);
            return;
        }
        
        // 检查是否有重定向
        if (line.contains(">") || line.contains("<") || line.contains(">>")) {
            executeRedirect(line, background);
            return;
        }
        
        // 普通命令执行
        executeSimpleCommand(line, background);
    }
    
    /**
     * 执行简单命令（无管道、无重定向）
     */
    private void executeSimpleCommand(String line, boolean background) {
        // 解析命令行
        String[] parts = parseCommandLine(line);
        if (parts.length == 0) {
            return;
        }
        
        String command = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        
        if (background) {
            // 后台执行：在单独的线程中执行
            new Thread(() -> {
                executeBuiltinCommand(command, args);
            }, "shell-bg-" + command).start();
            console.println("[" + command + "] Running in background...");
            return;
        }
        
        // 执行内置命令
        executeBuiltinCommand(command, args);
    }
    
    /**
     * 执行内置命令
     */
    private void executeBuiltinCommand(String command, String[] args) {
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
    
    /**
     * 执行管道命令
     * 例如：echo "hello" | echo
     */
    private void executePipe(String line, boolean background) {
        String[] commands = line.split("\\|");
        if (commands.length < 2) {
            console.println("Syntax error: pipe requires at least two commands");
            return;
        }
        
        console.println("[SHELL] Executing pipe: " + line);
        
        // 简化实现：顺序执行命令，将前一个命令的输出作为后一个命令的输入
        // 实际实现应该使用 pipe 系统调用创建管道
        
        List<String> outputs = new ArrayList<>();
        
        for (int i = 0; i < commands.length; i++) {
            String cmdLine = commands[i].trim();
            if (cmdLine.isEmpty()) {
                continue;
            }
            
            // 执行命令并捕获输出
            String output = executeCommandAndCaptureOutput(cmdLine);
            if (output != null && !output.isEmpty()) {
                outputs.add(output);
            }
        }
        
        // 打印最后一个命令的输出
        if (!outputs.isEmpty()) {
            console.println(outputs.get(outputs.size() - 1));
        }
    }
    
    /**
     * 执行重定向命令
     * 例如：echo "hello" > file.txt
     */
    private void executeRedirect(String line, boolean background) {
        // 解析重定向
        String inputFile = null;
        String outputFile = null;
        boolean append = false;
        String command = line;
        
        // 检查输入重定向 <
        if (line.contains("<")) {
            String[] parts = line.split("<", 2);
            if (parts.length == 2) {
                command = parts[0].trim();
                inputFile = parts[1].trim();
            }
        }
        
        // 检查输出重定向 > 或 >>
        if (line.contains(">>")) {
            String[] parts = line.split(">>", 2);
            if (parts.length == 2) {
                command = parts[0].trim();
                outputFile = parts[1].trim();
                append = true;
            }
        } else if (line.contains(">")) {
            String[] parts = line.split(">", 2);
            if (parts.length == 2) {
                command = parts[0].trim();
                outputFile = parts[1].trim();
                append = false;
            }
        }
        
        console.println("[SHELL] Executing redirect: " + line);
        console.println("  Command: " + command);
        if (inputFile != null) {
            console.println("  Input from: " + inputFile);
        }
        if (outputFile != null) {
            console.println("  Output to: " + outputFile + (append ? " (append)" : " (overwrite)"));
        }
        
        // 执行命令并捕获输出
        String output = executeCommandAndCaptureOutput(command);
        
        // 处理输出重定向
        if (outputFile != null) {
            try {
                // 使用 LibC 写入文件
                int flags = File.O_CREAT | File.O_WRONLY;
                if (append) {
                    flags |= File.O_APPEND;
                } else {
                    flags |= File.O_TRUNC;
                }
                
                int fd = libc.open(outputFile, flags, 0644);
                if (fd >= 0) {
                    byte[] data = output.getBytes();
                    libc.write(fd, data, data.length);
                    libc.close(fd);
                    console.println("[SHELL] Output written to: " + outputFile);
                } else {
                    console.println("[SHELL] Failed to open file: " + outputFile);
                }
            } catch (Exception e) {
                console.println("[SHELL] Error writing to file: " + e.getMessage());
            }
        } else {
            // 没有输出重定向，打印到控制台
            if (output != null && !output.isEmpty()) {
                console.println(output);
            }
        }
        
        // 处理输入重定向（简化：暂时不支持）
        if (inputFile != null) {
            console.println("[SHELL] Input redirection not fully implemented yet");
        }
    }
    
    /**
     * 执行命令并捕获输出
     * 返回命令的输出字符串
     */
    private String executeCommandAndCaptureOutput(String line) {
        String[] parts = parseCommandLine(line);
        if (parts.length == 0) {
            return "";
        }
        
        String command = parts[0];
        String[] args = new String[parts.length - 1];
        System.arraycopy(parts, 1, args, 0, args.length);
        
        // 捕获输出的 StringBuilder
        StringBuilder output = new StringBuilder();
        
        // 对于 echo 命令，返回参数
        if (command.equals("echo")) {
            for (int i = 0; i < args.length; i++) {
                if (i > 0) output.append(" ");
                output.append(args[i]);
            }
            return output.toString();
        }
        
        // 对于其他命令，执行并返回空（简化实现）
        // 实际应该重定向 stdout 来捕获输出
        executeBuiltinCommand(command, args);
        
        return output.toString();
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
        console.println("                  Types: signal, pipe, libc, system, interactive, all");
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
        String type = args.length > 0 ? args[0] : "help";
        
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
            case "system":
            case "capabilities":
                SystemCapabilitiesDemo.runAllDemos(kernel);
                break;
            case "interactive":
                InteractiveDemo.runInteractiveDemo(kernel);
                break;
            case "all":
                runSignalDemo();
                runPipeDemo();
                runLibCDemo();
                SystemCapabilitiesDemo.runAllDemos(kernel);
                InteractiveDemo.runInteractiveDemo(kernel);
                break;
            case "help":
            default:
                console.println("\nAvailable demo types:");
                console.println("  signal       - Signal mechanism demonstration");
                console.println("  pipe         - Pipe IPC demonstration");
                console.println("  libc         - LibC library demonstration");
                console.println("  system       - Comprehensive system capabilities demo");
                console.println("  interactive  - Interactive demonstration");
                console.println("  all          - Run all demonstrations");
                console.println("\nUsage: demo [type]");
                console.println("Example: demo system");
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
