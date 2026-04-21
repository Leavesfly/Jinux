package jinux.shell.commands;

import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * uptime 命令 - 显示系统运行时间
 */
public class UptimeCommand implements Command {
    
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_BOLD  = "\033[1m";
    
    @Override
    public String getName() {
        return "uptime";
    }
    
    @Override
    public String getDescription() {
        return "Show system uptime";
    }
    
    @Override
    public String getUsage() {
        return "uptime";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        long jiffies = context.getScheduler().getJiffies();
        long uptimeSeconds = jiffies / 100;
        
        context.println("");
        context.println(ANSI_BOLD + "System uptime:" + ANSI_RESET + " " + uptimeSeconds + " seconds (" + jiffies + " jiffies)");
        context.println("  " + (uptimeSeconds / 3600) + " hours, " + 
                       ((uptimeSeconds % 3600) / 60) + " minutes, " + 
                       (uptimeSeconds % 60) + " seconds");
        context.println("");
    }
}
