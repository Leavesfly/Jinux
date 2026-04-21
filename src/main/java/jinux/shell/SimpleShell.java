package jinux.shell;

import jinux.drivers.ConsoleDevice;
import jinux.fs.File;
import jinux.kernel.Kernel;
import jinux.kernel.Scheduler;
import jinux.kernel.Task;
import jinux.lib.LibC;
import jinux.mm.MemoryManager;
import jinux.shell.commands.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * SimpleShell - Jinux 简单命令行解释器
 * 
 * 实现基本的内置命令，提供交互式命令行界面。
 * 支持命令历史、ANSI 彩色输出、统一错误提示。
 * 使用 Command 模式和 CommandRegistry 进行命令分发。
 */
public class SimpleShell {
    
    private final Kernel kernel;
    private final ConsoleDevice console;
    private final Scheduler scheduler;
    private final MemoryManager memoryManager;
    private final LibC libc;
    private boolean running;
    
    /** 命令历史记录 */
    private final List<String> commandHistory;
    
    /** 命令注册表 */
    private final CommandRegistry commandRegistry;
    
    /** Shell 上下文 */
    private ShellContext shellContext;
    
    /** 历史记录最大容量 */
    private static final int MAX_HISTORY_SIZE = 100;
    
    /** Shell 提示符 */
    private static final String PROMPT = "jinux$ ";
    
    /** Shell 版本 */
    private static final String VERSION = "0.0.1-alpha";
    
    // ==================== ANSI 颜色常量 ====================
    
    private static final String ANSI_RESET   = "\033[0m";
    private static final String ANSI_RED     = "\033[31m";
    private static final String ANSI_GREEN   = "\033[32m";
    private static final String ANSI_YELLOW  = "\033[33m";
    private static final String ANSI_BLUE    = "\033[34m";
    private static final String ANSI_CYAN    = "\033[36m";
    private static final String ANSI_BOLD    = "\033[1m";
    
    public SimpleShell(Kernel kernel) {
        this.kernel = kernel;
        this.console = kernel.getConsole();
        this.scheduler = kernel.getScheduler();
        this.memoryManager = kernel.getMemoryManager();
        this.libc = new LibC(kernel.getSyscallDispatcher());
        this.running = true;
        this.commandHistory = new ArrayList<>();
        this.commandRegistry = new CommandRegistry();
        
        // 创建 Shell 上下文（使用调度器中的当前任务）
        Task currentTask = scheduler.getCurrentTask();
        this.shellContext = new ShellContext(kernel, currentTask);
        
        // 初始化命令注册表
        initCommands();
    }
    
    /**
     * 初始化命令注册表，注册所有内置命令
     */
    private void initCommands() {
        // 注册基本命令
        commandRegistry.register(new HelpCommand());
        commandRegistry.register(new PsCommand());
        commandRegistry.register(new MemCommand());
        commandRegistry.register(new TimeCommand());
        commandRegistry.register(new SignalCommand());
        commandRegistry.register(new DemoCommand());
        commandRegistry.register(new RunCommand());
        commandRegistry.register(new ProgramsCommand());
        commandRegistry.register(new EchoCommand());
        commandRegistry.register(new ClearCommand());
        commandRegistry.register(new UptimeCommand());
        commandRegistry.register(new VersionCommand());
        commandRegistry.register(new HistoryCommand(commandHistory));
        commandRegistry.register(new ExitCommand(() -> { running = false; }));
        
        // 注册教学增强命令
        commandRegistry.register(new QuizCommand());
        commandRegistry.register(new VisualizeCommand());
        commandRegistry.register(new LabCommand());
        commandRegistry.register(new ExplainCommand());
        commandRegistry.register(new ProgressCommand());
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
                    addToHistory(line);
                    executeCommand(line);
                }
                
            } catch (Exception e) {
                printError(e.getMessage());
            }
        }
        
        console.println("\n" + ANSI_GREEN + "[SHELL] Goodbye!" + ANSI_RESET);
    }
    
    /**
     * 打印 Shell 欢迎信息
     */
    private void printBanner() {
        console.println("");
        console.println(ANSI_CYAN + ANSI_BOLD + "  ╔══════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_CYAN + ANSI_BOLD + "  ║     Jinux Simple Shell v" + VERSION + "     ║" + ANSI_RESET);
        console.println(ANSI_CYAN + ANSI_BOLD + "  ║   Java Implementation of Linux 0.01  ║" + ANSI_RESET);
        console.println(ANSI_CYAN + ANSI_BOLD + "  ╚══════════════════════════════════════╝" + ANSI_RESET);
        console.println(ANSI_YELLOW + "  Type 'help' for available commands" + ANSI_RESET);
        console.println(ANSI_YELLOW + "  Type 'exit' to quit" + ANSI_RESET);
        console.println("");
    }
    
    /**
     * 将命令添加到历史记录
     */
    private void addToHistory(String command) {
        if (commandHistory.size() >= MAX_HISTORY_SIZE) {
            commandHistory.remove(0);
        }
        commandHistory.add(command);
    }
    
    /**
     * 打印错误信息（统一红色格式）
     */
    private void printError(String message) {
        console.println(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
    }
    
    /**
     * 打印警告信息（统一黄色格式）
     */
    private void printWarning(String message) {
        console.println(ANSI_YELLOW + "[WARN] " + message + ANSI_RESET);
    }
    
    /**
     * 打印成功信息（统一绿色格式）
     */
    private void printSuccess(String message) {
        console.println(ANSI_GREEN + message + ANSI_RESET);
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
        Command cmd = commandRegistry.getCommand(command);
        if (cmd != null) {
            cmd.execute(args, shellContext);
        } else {
            printError(command + ": command not found");
            String similar = commandRegistry.findSimilarCommand(command);
            if (similar != null) {
                console.println(ANSI_YELLOW + "  Did you mean: " + ANSI_BOLD + similar + ANSI_RESET + ANSI_YELLOW + " ?" + ANSI_RESET);
            }
            console.println("  Type " + ANSI_BOLD + "'help'" + ANSI_RESET + " for available commands");
        }
    }
    
    /**
     * 解析命令行，支持单引号和双引号包裹的参数
     * 例如：echo "hello world" 'foo bar' baz → ["echo", "hello world", "foo bar", "baz"]
     */
    private String[] parseCommandLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inDoubleQuote = false;
        boolean inSingleQuote = false;
        
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            
            if (inDoubleQuote) {
                if (ch == '"') {
                    inDoubleQuote = false;
                } else {
                    current.append(ch);
                }
            } else if (inSingleQuote) {
                if (ch == '\'') {
                    inSingleQuote = false;
                } else {
                    current.append(ch);
                }
            } else if (ch == '"') {
                inDoubleQuote = true;
            } else if (ch == '\'') {
                inSingleQuote = true;
            } else if (Character.isWhitespace(ch)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(ch);
            }
        }
        
        // 添加最后一个 token
        if (current.length() > 0) {
            tokens.add(current.toString());
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
            printError("Syntax error: pipe requires at least two commands");
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
}
