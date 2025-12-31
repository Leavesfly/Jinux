package jinux.init;

import jinux.kernel.Task;
import jinux.kernel.Kernel;
import jinux.include.Syscalls;
import jinux.lib.LibC;
import jinux.demo.LibCExample;
import jinux.demo.SignalExample;
import jinux.demo.PipeExample;
import jinux.demo.ProcessExample;
import jinux.demo.SystemCapabilitiesDemo;
import jinux.demo.InteractiveDemo;
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
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        
        console.println("\n[INIT] Init process (PID " + task.getPid() + ") started!");
        console.println("[INIT] Jinux operating system is now running.\n");
        
        // 演示 LibC 用户态库
        console.println("[INIT] Demonstrating LibC user-space library...\n");
        LibCExample.demo(kernel);
        LibCExample.compareWithDirectCall(kernel);
        
        // 演示信号机制
        console.println("[INIT] Demonstrating Signal mechanism...\n");
        SignalExample.showSignalInfo(kernel);
        SignalExample.demo(kernel);
        SignalExample.demoWithLibC(kernel);
        
        // 演示管道机制
        console.println("[INIT] Demonstrating Pipe mechanism...\n");
        PipeExample.showUseCases(kernel);
        PipeExample.demo(kernel);
        PipeExample.demoWithLibC(kernel);
        
        // 演示进程创建和程序执行
        console.println("[INIT] Demonstrating Process Creation & Execution...\n");
        ProcessExample.demoForkExec(kernel);
        ProcessExample.demo(kernel);
        
        // 演示系统调用
        demonstrateSystemCalls();
        
        // 演示进程管理
        demonstrateProcessManagement();
        
        // 演示内存管理
        demonstrateMemoryManagement();
        
        // 综合系统能力演示
        console.println("\n[INIT] Running comprehensive system capabilities demonstration...\n");
        SystemCapabilitiesDemo.runAllDemos(kernel);
        
        // 交互式演示
        console.println("\n[INIT] Running interactive demonstration...\n");
        InteractiveDemo.runInteractiveDemo(kernel);
        
        // 启动 Shell
        console.println("\n[INIT] Starting Simple Shell...");
        console.println("[INIT] You can now interact with Jinux!\n");
        
        SimpleShell shell = new SimpleShell(kernel);
        shell.run();
        
        console.println("[INIT] Shell exited.");
        
        console.println("[INIT] Init process exiting...");
    }
    
    /**
     * 演示系统调用功能
     */
    private void demonstrateSystemCalls() {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        
        console.println("========== System Call Demonstration ==========");
        
        // getpid
        long pid = syscall.dispatch(Syscalls.SYS_GETPID, 0, 0, 0);
        console.println("[DEMO] getpid() = " + pid);
        
        // getppid
        long ppid = syscall.dispatch(Syscalls.SYS_GETPPID, 0, 0, 0);
        console.println("[DEMO] getppid() = " + ppid);
        
        // time
        long time = syscall.dispatch(Syscalls.SYS_TIME, 0, 0, 0);
        console.println("[DEMO] time() = " + time + " (Unix timestamp)");
        
        // write
        String msg = "Hello from init process!\n";
        byte[] msgBytes = msg.getBytes();
        long written = syscall.dispatch(Syscalls.SYS_WRITE, 1, 0, msgBytes.length);
        console.println("[DEMO] write(1, \"" + msg.trim() + "\", " + msgBytes.length + ") = " + written);
        
        console.println("===============================================\n");
    }
    
    /**
     * 演示进程管理功能
     */
    private void demonstrateProcessManagement() {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        
        console.println("========== Process Management Demo ===========");
        
        // 打印当前进程信息
        console.println("[DEMO] Current process: " + task);
        console.println("[DEMO] Process state: " + task.getStateName());
        console.println("[DEMO] Priority: " + task.getPriority());
        console.println("[DEMO] Time counter: " + task.getCounter());
        
        // 模拟 fork（实际调用会创建子进程，这里仅演示）
        console.println("\n[DEMO] Demonstrating fork() system call...");
        console.println("[DEMO] (In a real system, this would create a child process)");
        
        console.println("===============================================\n");
    }
    
    /**
     * 演示内存管理功能
     */
    private void demonstrateMemoryManagement() {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        
        console.println("========== Memory Management Demo =============");
        
        // 获取当前 brk
        long currentBrk = task.getAddressSpace().getBrk();
        console.println("[DEMO] Current brk: 0x" + Long.toHexString(currentBrk));
        
        // 扩展堆
        long newBrk = 0x10000;
        long result = syscall.dispatch(Syscalls.SYS_BRK, newBrk, 0, 0);
        console.println("[DEMO] brk(0x" + Long.toHexString(newBrk) + ") = 0x" + Long.toHexString(result));
        
        // 打印地址空间信息
        var addrSpace = task.getAddressSpace();
        console.println("[DEMO] Address space layout:");
        console.println("  Code: 0x" + Long.toHexString(addrSpace.getCodeStart()) + 
            " - 0x" + Long.toHexString(addrSpace.getCodeEnd()));
        console.println("  Data: 0x" + Long.toHexString(addrSpace.getDataStart()) + 
            " - 0x" + Long.toHexString(addrSpace.getDataEnd()));
        console.println("  Brk:  0x" + Long.toHexString(addrSpace.getBrk()));
        console.println("  Stack: 0x" + Long.toHexString(addrSpace.getStackTop()));
        
        console.println("===============================================\n");
    }
    
    /**
     * 停止 init 进程
     */
    public void stop() {
        running = false;
    }
}
