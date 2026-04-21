package jinux.init;

import jinux.kernel.Task;
import jinux.kernel.Kernel;
import jinux.shell.SimpleShell;

/**
 * Init 进程实现
 * 对应 Linux 0.01 中的第一个用户进程 /etc/init
 * 
 * 负责系统初始化和管理用户进程
 * 
 * @author Jinux Project
 */
public class InitProcess implements Runnable {
    
    private final Kernel kernel;
    private final Task task;
    private volatile boolean running;
    
    public InitProcess(Kernel kernel, Task task) {
        this.kernel = kernel;
        this.task = task;
        this.running = true;
    }
    
    @Override
    public void run() {
        initializeSystem();
        startShell();
    }
    
    /**
     * 初始化系统，输出欢迎信息
     */
    private void initializeSystem() {
        var console = kernel.getConsole();
        console.println("\n[INIT] Init process (PID " + task.getPid() + ") started!");
        console.println("[INIT] Jinux operating system is now running.\n");
    }
    
    /**
     * 启动 Shell，退出后自动重启（init 进程不应退出）
     */
    private void startShell() {
        var console = kernel.getConsole();
        while (running) {
            console.println("\n[INIT] Starting Simple Shell...");
            console.println("[INIT] You can now interact with Jinux!\n");
            
            try {
                SimpleShell shell = new SimpleShell(kernel);
                shell.run();
                console.println("[INIT] Shell exited. Restarting in 1 second...");
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                console.println("[INIT] Shell crashed: " + e.getMessage());
                console.println("[INIT] Restarting Shell in 2 seconds...");
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
    
    /**
     * 停止 init 进程
     */
    public void stop() {
        running = false;
    }
}