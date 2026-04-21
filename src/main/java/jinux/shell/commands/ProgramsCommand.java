package jinux.shell.commands;

import jinux.exec.ProgramLoader;
import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * programs 命令 - 列出可用程序
 */
public class ProgramsCommand implements Command {
    
    @Override
    public String getName() {
        return "programs";
    }
    
    @Override
    public String getDescription() {
        return "List available programs";
    }
    
    @Override
    public String getUsage() {
        return "programs";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        context.println("");
        ProgramLoader.listPrograms();
    }
}
