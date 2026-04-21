package jinux.shell.commands;

import jinux.lib.LibC;
import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * time 命令 - 显示系统时间
 */
public class TimeCommand implements Command {
    
    @Override
    public String getName() {
        return "time";
    }
    
    @Override
    public String getDescription() {
        return "Show current system time";
    }
    
    @Override
    public String getUsage() {
        return "time";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        LibC libc = new LibC(context.getSyscallDispatcher());
        long time = libc.time();
        context.println("\nSystem time: " + time + " seconds since epoch");
        context.println("Date: " + new java.util.Date(time * 1000));
        context.println("");
    }
}
