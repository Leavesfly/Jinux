package jinux.shell.commands;

import jinux.kernel.Signal;
import jinux.lib.LibC;
import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * signal/kill 命令 - 发送信号到进程
 */
public class SignalCommand implements Command {
    
    @Override
    public String getName() {
        return "signal";
    }
    
    @Override
    public String getDescription() {
        return "Send signal to process";
    }
    
    @Override
    public String getUsage() {
        return "signal <pid> <signum>";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        if (args.length < 2) {
            context.println("Usage: signal <pid> <signum>");
            context.println("       kill <pid> <signum>");
            context.println("\nCommon signals:");
            context.println("  1  SIGHUP    - Hangup");
            context.println("  2  SIGINT    - Interrupt");
            context.println("  9  SIGKILL   - Kill (cannot be caught)");
            context.println("  15 SIGTERM   - Terminate");
            context.println("  17 SIGCHLD   - Child status changed");
            context.println("");
            return;
        }
        
        try {
            int pid = Integer.parseInt(args[0]);
            int signum = Integer.parseInt(args[1]);
            
            LibC libc = new LibC(context.getSyscallDispatcher());
            int ret = libc.kill(pid, signum);
            if (ret == 0) {
                context.println("Signal " + signum + " sent to process " + pid);
            } else {
                context.println("Failed to send signal (error: " + ret + ")");
            }
        } catch (NumberFormatException e) {
            context.println("Error: PID and signal number must be integers");
        }
        context.println("");
    }
}
