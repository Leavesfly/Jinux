package jinux.shell.commands;

import jinux.demo.InteractiveDemo;
import jinux.demo.SystemCapabilitiesDemo;
import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * demo 命令 - 运行演示
 */
public class DemoCommand implements Command {
    
    @Override
    public String getName() {
        return "demo";
    }
    
    @Override
    public String getDescription() {
        return "Run demonstrations";
    }
    
    @Override
    public String getUsage() {
        return "demo [type]";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        String type = args.length > 0 ? args[0] : "help";
        
        switch (type) {
            case "signal":
                runSignalDemo(context);
                break;
            case "pipe":
                runPipeDemo(context);
                break;
            case "libc":
                runLibCDemo(context);
                break;
            case "system":
            case "capabilities":
                SystemCapabilitiesDemo.runAllDemos(context.getKernel());
                break;
            case "interactive":
                InteractiveDemo.runInteractiveDemo(context.getKernel());
                break;
            case "all":
                runSignalDemo(context);
                runPipeDemo(context);
                runLibCDemo(context);
                SystemCapabilitiesDemo.runAllDemos(context.getKernel());
                InteractiveDemo.runInteractiveDemo(context.getKernel());
                break;
            case "help":
            default:
                context.println("\nAvailable demo types:");
                context.println("  signal       - Signal mechanism demonstration");
                context.println("  pipe         - Pipe IPC demonstration");
                context.println("  libc         - LibC library demonstration");
                context.println("  system       - Comprehensive system capabilities demo");
                context.println("  interactive  - Interactive demonstration");
                context.println("  all          - Run all demonstrations");
                context.println("\nUsage: demo [type]");
                context.println("Example: demo system");
                break;
        }
        context.println("");
    }
    
    private void runSignalDemo(ShellContext context) {
        try {
            context.println("\n[DEMO] Signal mechanism demonstration:");
            
            jinux.lib.LibC libc = new jinux.lib.LibC(context.getSyscallDispatcher());
            int pid = libc.getpid();
            context.println("  Current PID: " + pid);
            
            context.println("  Setting SIGINT handler to SIG_IGN...");
            libc.signal(jinux.kernel.Signal.SIGINT, jinux.kernel.Signal.SIG_IGN);
            
            context.println("  Signal demo complete");
            
        } catch (Exception e) {
            context.println("  Error in signal demo: " + e.getMessage());
        }
    }
    
    private void runPipeDemo(ShellContext context) {
        context.println("\n[DEMO] Pipe mechanism demonstration:");
        context.println("  Pipe demo would create communication channel");
        context.println("  between parent and child processes");
        context.println("  (Full implementation requires fork support)");
    }
    
    private void runLibCDemo(ShellContext context) {
        context.println("\n[DEMO] LibC demonstration:");
        
        jinux.lib.LibC libc = new jinux.lib.LibC(context.getSyscallDispatcher());
        int pid = libc.getpid();
        context.println("  getpid() = " + pid);
        
        long time = libc.time();
        context.println("  time() = " + time);
        
        context.println("  LibC demo complete");
    }
}
