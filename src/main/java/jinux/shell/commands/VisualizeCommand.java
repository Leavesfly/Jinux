package jinux.shell.commands;

import jinux.demo.Visualizer;
import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * visualize 命令 - ASCII 可视化工具
 */
public class VisualizeCommand implements Command {
    
    @Override
    public String getName() {
        return "visualize";
    }
    
    @Override
    public String getDescription() {
        return "ASCII visualization of OS internals";
    }
    
    @Override
    public String getUsage() {
        return "visualize";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        Visualizer.showVisualizationMenu(context.getKernel());
    }
}
