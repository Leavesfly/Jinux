package jinux.shell.commands;

import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * run 命令 - 运行程序
 */
public class RunCommand implements Command {
    
    @Override
    public String getName() {
        return "run";
    }
    
    @Override
    public String getDescription() {
        return "Run a program (demo)";
    }
    
    @Override
    public String getUsage() {
        return "run <program> [args...]";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        if (args.length < 1) {
            context.println("Usage: run <program> [args...]");
            context.println("Example: run hello");
            context.println("Type 'programs' to list available programs");
            context.println("");
            return;
        }
        
        String program = args[0];
        context.println("\nRunning program: " + program);
        context.println("Note: This is a demonstration of program execution");
        context.println("In a full implementation, the program would:");
        context.println("  1. Be loaded using execve()");
        context.println("  2. Replace current process");
        context.println("  3. Execute with new code\n");
        
        context.println("[Simulated execution of '" + program + "']");
        context.println("");
    }
}
