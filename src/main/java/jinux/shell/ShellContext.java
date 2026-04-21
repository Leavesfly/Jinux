package jinux.shell;

import jinux.drivers.ConsoleDevice;
import jinux.kernel.Kernel;
import jinux.kernel.Scheduler;
import jinux.kernel.SystemCallDispatcher;
import jinux.kernel.Task;
import jinux.fs.VirtualFileSystem;

/**
 * Shell 上下文
 * 封装命令执行所需的依赖对象，提供便捷的输出方法
 */
public class ShellContext {
    
    private final Kernel kernel;
    private final ConsoleDevice console;
    private final SystemCallDispatcher syscallDispatcher;
    private final Scheduler scheduler;
    private final VirtualFileSystem vfs;
    private final Task currentTask;
    
    // ANSI 颜色常量
    private static final String ANSI_RESET   = "\033[0m";
    private static final String ANSI_RED     = "\033[31m";
    private static final String ANSI_GREEN   = "\033[32m";
    private static final String ANSI_YELLOW  = "\033[33m";
    
    public ShellContext(Kernel kernel, Task currentTask) {
        this.kernel = kernel;
        this.console = kernel.getConsole();
        this.syscallDispatcher = kernel.getSyscallDispatcher();
        this.scheduler = kernel.getScheduler();
        this.vfs = kernel.getVfs();
        this.currentTask = currentTask;
    }
    
    /**
     * 获取 Kernel 实例
     */
    public Kernel getKernel() {
        return kernel;
    }
    
    /**
     * 获取 ConsoleDevice 实例
     */
    public ConsoleDevice getConsole() {
        return console;
    }
    
    /**
     * 获取 SystemCallDispatcher 实例
     */
    public SystemCallDispatcher getSyscallDispatcher() {
        return syscallDispatcher;
    }
    
    /**
     * 获取 Scheduler 实例
     */
    public Scheduler getScheduler() {
        return scheduler;
    }
    
    /**
     * 获取 VirtualFileSystem 实例
     */
    public VirtualFileSystem getVfs() {
        return vfs;
    }
    
    /**
     * 获取当前任务
     */
    public Task getCurrentTask() {
        return currentTask;
    }
    
    /**
     * 打印普通消息
     */
    public void println(String message) {
        console.println(message);
    }
    
    /**
     * 打印错误消息（红色）
     */
    public void printError(String message) {
        console.println(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
    }
    
    /**
     * 打印成功消息（绿色）
     */
    public void printSuccess(String message) {
        console.println(ANSI_GREEN + message + ANSI_RESET);
    }
    
    /**
     * 打印警告消息（黄色）
     */
    public void printWarning(String message) {
        console.println(ANSI_YELLOW + "[WARN] " + message + ANSI_RESET);
    }
}
