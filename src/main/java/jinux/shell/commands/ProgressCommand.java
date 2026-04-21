package jinux.shell.commands;

import jinux.demo.LearningTracker;
import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * progress 命令 - 学习进度追踪
 */
public class ProgressCommand implements Command {
    
    @Override
    public String getName() {
        return "progress";
    }
    
    @Override
    public String getDescription() {
        return "View learning progress & achievements";
    }
    
    @Override
    public String getUsage() {
        return "progress";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        LearningTracker.getInstance().showMenu(context.getKernel());
    }
}
