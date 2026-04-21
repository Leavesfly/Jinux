package jinux.shell.commands;

import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * mem 命令 - 显示内存统计
 */
public class MemCommand implements Command {
    
    @Override
    public String getName() {
        return "mem";
    }
    
    @Override
    public String getDescription() {
        return "Show memory statistics";
    }
    
    @Override
    public String getUsage() {
        return "mem";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        context.println("");
        context.getKernel().getMemoryManager().printStats();
        context.println("");
    }
}
