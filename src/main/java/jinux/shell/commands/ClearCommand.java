package jinux.shell.commands;

import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * clear 命令 - 清屏
 */
public class ClearCommand implements Command {
    
    @Override
    public String getName() {
        return "clear";
    }
    
    @Override
    public String getDescription() {
        return "Clear screen";
    }
    
    @Override
    public String getUsage() {
        return "clear";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        // 简单实现：打印多个换行
        for (int i = 0; i < 50; i++) {
            context.println("");
        }
    }
}
