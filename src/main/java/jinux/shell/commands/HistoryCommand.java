package jinux.shell.commands;

import jinux.shell.Command;
import jinux.shell.ShellContext;

import java.util.List;

/**
 * history 命令 - 显示命令历史
 */
public class HistoryCommand implements Command {
    
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_CYAN  = "\033[36m";
    private static final String ANSI_BOLD  = "\033[1m";
    
    private final List<String> commandHistory;
    
    public HistoryCommand(List<String> commandHistory) {
        this.commandHistory = commandHistory;
    }
    
    @Override
    public String getName() {
        return "history";
    }
    
    @Override
    public String getDescription() {
        return "Show command history";
    }
    
    @Override
    public String getUsage() {
        return "history";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        context.println("");
        if (commandHistory.isEmpty()) {
            context.printWarning("No command history");
            return;
        }
        context.println(ANSI_BOLD + "Command history:" + ANSI_RESET);
        for (int i = 0; i < commandHistory.size(); i++) {
            context.println(String.format("  %s%3d%s  %s", ANSI_CYAN, i + 1, ANSI_RESET, commandHistory.get(i)));
        }
        context.println("");
    }
}
