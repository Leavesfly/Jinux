package jinux.demo;

import jinux.kernel.Kernel;
import jinux.kernel.Signal;
import jinux.lib.LibC;

/**
 * 信号机制演示
 * 
 * @author Jinux Project
 */
public class SignalExample {
    
    /**
     * 演示信号的基本使用
     */
    public static void demo(Kernel kernel) {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        
        console.println("\n========== Signal Mechanism Demo ==========\n");
        
        // 1. 获取当前进程 PID
        long pid = syscall.dispatch(jinux.include.Syscalls.SYS_GETPID, 0, 0, 0);
        console.println("[DEMO] Current process PID: " + pid);
        
        // 2. 设置信号处理器 - 忽略 SIGINT
        console.println("\n[DEMO] Setting SIGINT handler to SIG_IGN...");
        long oldHandler = syscall.dispatch(jinux.include.Syscalls.SYS_SIGNAL, 
            Signal.SIGINT, Signal.SIG_IGN, 0);
        console.println("[DEMO] Old SIGINT handler: " + oldHandler);
        
        // 3. 发送 SIGINT 信号（应该被忽略）
        console.println("\n[DEMO] Sending SIGINT to self (should be ignored)...");
        syscall.dispatch(jinux.include.Syscalls.SYS_KILL, pid, Signal.SIGINT, 0);
        
        // 处理信号
        syscall.processSignals(kernel.getScheduler().getCurrentTask());
        console.println("[DEMO] Process still running after SIGINT (ignored)");
        
        // 4. 恢复默认处理器
        console.println("\n[DEMO] Restoring SIGINT handler to SIG_DFL...");
        syscall.dispatch(jinux.include.Syscalls.SYS_SIGNAL, 
            Signal.SIGINT, Signal.SIG_DFL, 0);
        
        // 5. 演示 SIGCHLD（子进程退出信号）
        console.println("\n[DEMO] SIGCHLD is ignored by default");
        console.println("[DEMO] Default action for SIGCHLD: " + 
            Signal.getDefaultAction(Signal.SIGCHLD));
        
        // 6. 演示不可捕获的信号
        console.println("\n[DEMO] SIGKILL and SIGSTOP cannot be caught or ignored");
        long result = syscall.dispatch(jinux.include.Syscalls.SYS_SIGNAL, 
            Signal.SIGKILL, Signal.SIG_IGN, 0);
        if (result < 0) {
            console.println("[DEMO] Attempting to ignore SIGKILL failed (as expected)");
        }
        
        console.println("\n========== Signal Demo Complete ==========\n");
    }
    
    /**
     * 演示使用 LibC 封装的信号函数
     */
    public static void demoWithLibC(Kernel kernel) {
        var console = kernel.getConsole();
        LibC libc = new LibC(kernel.getSyscallDispatcher());
        
        console.println("\n========== LibC Signal Functions Demo ==========\n");
        
        int pid = libc.getpid();
        console.println("[DEMO] Using LibC: PID = " + pid);
        
        // 设置信号处理器
        console.println("\n[DEMO] libc.signal(SIGTERM, SIG_IGN)");
        long oldHandler = libc.signal(Signal.SIGTERM, Signal.SIG_IGN);
        console.println("[DEMO] Old handler: " + oldHandler);
        
        // 发送信号
        console.println("\n[DEMO] libc.kill(pid, SIGTERM)");
        int ret = libc.kill(pid, Signal.SIGTERM);
        console.println("[DEMO] kill() returned: " + ret);
        
        // 处理信号
        kernel.getSyscallDispatcher().processSignals(kernel.getScheduler().getCurrentTask());
        console.println("[DEMO] Process still alive (SIGTERM was ignored)");
        
        console.println("\n========== LibC Signal Demo Complete ==========\n");
    }
    
    /**
     * 演示常见信号及其默认行为
     */
    public static void showSignalInfo(Kernel kernel) {
        var console = kernel.getConsole();
        
        console.println("\n========== Common Signals and Default Actions ==========\n");
        
        int[] commonSignals = {
            Signal.SIGHUP,
            Signal.SIGINT,
            Signal.SIGQUIT,
            Signal.SIGILL,
            Signal.SIGKILL,
            Signal.SIGSEGV,
            Signal.SIGPIPE,
            Signal.SIGALRM,
            Signal.SIGTERM,
            Signal.SIGCHLD,
            Signal.SIGCONT,
            Signal.SIGSTOP
        };
        
        for (int sig : commonSignals) {
            String name = Signal.getSignalName(sig);
            Signal.SignalAction action = Signal.getDefaultAction(sig);
            console.println(String.format("  %2d %-10s -> %s", sig, name, action));
        }
        
        console.println("\n========================================\n");
    }
}
