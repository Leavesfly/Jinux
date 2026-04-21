package jinux.shell.commands;

import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * version 命令 - 显示版本信息
 */
public class VersionCommand implements Command {
    
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_CYAN  = "\033[36m";
    private static final String ANSI_BOLD  = "\033[1m";
    
    @Override
    public String getName() {
        return "version";
    }
    
    @Override
    public String getDescription() {
        return "Show Jinux version";
    }
    
    @Override
    public String getUsage() {
        return "version";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        context.println("");
        context.println(ANSI_BOLD + "Jinux Operating System" + ANSI_RESET);
        context.println("  " + ANSI_CYAN + "Kernel Version:" + ANSI_RESET + " 0.01-alpha");
        context.println("  " + ANSI_CYAN + "Shell Version:" + ANSI_RESET + "  0.0.1-alpha");
        context.println("  " + ANSI_CYAN + "Platform:" + ANSI_RESET + "       Java Implementation of Linux 0.01");
        context.println("");
    }
}
