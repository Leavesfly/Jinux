package jinux.shell.commands;

import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * exit/quit 命令 - 退出 Shell
 */
public class ExitCommand implements Command {
    
    private final Runnable onExit;
    
    public ExitCommand(Runnable onExit) {
        this.onExit = onExit;
    }
    
    @Override
    public String getName() {
        return "exit";
    }
    
    @Override
    public String getDescription() {
        return "Exit shell";
    }
    
    @Override
    public String getUsage() {
        return "exit";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        context.println("Exiting shell...");
        onExit.run();
    }
}
