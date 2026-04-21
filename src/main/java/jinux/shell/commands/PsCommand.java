package jinux.shell.commands;

import jinux.include.Const;
import jinux.kernel.Task;
import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * ps 命令 - 显示进程列表
 */
public class PsCommand implements Command {
    
    private static final String ANSI_RESET   = "\033[0m";
    private static final String ANSI_GREEN   = "\033[32m";
    private static final String ANSI_RED     = "\033[31m";
    private static final String ANSI_YELLOW  = "\033[33m";
    private static final String ANSI_BOLD    = "\033[1m";
    
    @Override
    public String getName() {
        return "ps";
    }
    
    @Override
    public String getDescription() {
        return "List all processes";
    }
    
    @Override
    public String getUsage() {
        return "ps";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        context.println("");
        context.println(ANSI_BOLD + "PID\tPPID\tSTATE\t\tPRIORITY\tCOUNTER" + ANSI_RESET);
        context.println("------------------------------------------------------------");
        
        boolean found = false;
        for (Task task : context.getScheduler().getTaskTable()) {
            if (task != null) {
                found = true;
                String stateColor = ANSI_GREEN;
                if (task.getState() == Const.TASK_ZOMBIE) {
                    stateColor = ANSI_RED;
                } else if (task.getState() == Const.TASK_INTERRUPTIBLE
                        || task.getState() == Const.TASK_UNINTERRUPTIBLE) {
                    stateColor = ANSI_YELLOW;
                }
                context.println(String.format("%d\t%d\t%s%-12s%s\t%d\t\t%d",
                    task.getPid(),
                    task.getPpid(),
                    stateColor,
                    task.getStateName(),
                    ANSI_RESET,
                    task.getPriority(),
                    task.getCounter()
                ));
            }
        }
        if (!found) {
            context.printWarning("No running tasks");
        }
        context.println("");
    }
}
