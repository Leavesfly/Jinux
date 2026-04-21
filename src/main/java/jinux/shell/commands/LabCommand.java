package jinux.shell.commands;

import jinux.demo.LabSimulator;
import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * lab 命令 - 交互式模拟实验
 */
public class LabCommand implements Command {
    
    @Override
    public String getName() {
        return "lab";
    }
    
    @Override
    public String getDescription() {
        return "Hands-on simulation experiments";
    }
    
    @Override
    public String getUsage() {
        return "lab";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        LabSimulator.runLab(context.getKernel());
    }
}
