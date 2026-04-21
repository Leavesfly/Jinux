package jinux.shell.commands;

import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * help 命令 - 显示帮助信息
 */
public class HelpCommand implements Command {
    
    private static final String ANSI_RESET   = "\033[0m";
    private static final String ANSI_GREEN   = "\033[32m";
    private static final String ANSI_CYAN    = "\033[36m";
    private static final String ANSI_BOLD    = "\033[1m";
    
    @Override
    public String getName() {
        return "help";
    }
    
    @Override
    public String getDescription() {
        return "Show this help message";
    }
    
    @Override
    public String getUsage() {
        return "help";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        context.println("");
        context.println(ANSI_BOLD + "Available commands:" + ANSI_RESET);
        context.println(ANSI_GREEN + "  help" + ANSI_RESET + "          - Show this help message");
        context.println(ANSI_GREEN + "  ps" + ANSI_RESET + "            - List all processes");
        context.println(ANSI_GREEN + "  mem" + ANSI_RESET + "           - Show memory statistics");
        context.println(ANSI_GREEN + "  time" + ANSI_RESET + "          - Show current system time");
        context.println(ANSI_GREEN + "  signal" + ANSI_RESET + " <pid> <signum> - Send signal to process");
        context.println(ANSI_GREEN + "  kill" + ANSI_RESET + " <pid> <signum>   - Alias for signal");
        context.println(ANSI_GREEN + "  demo" + ANSI_RESET + " [type]   - Run demonstrations");
        context.println("                  Types: signal, pipe, libc, system, interactive, all");
        context.println(ANSI_GREEN + "  run" + ANSI_RESET + " <prog>    - Run a program (demo)");
        context.println(ANSI_GREEN + "  programs" + ANSI_RESET + "      - List available programs");
        context.println(ANSI_GREEN + "  echo" + ANSI_RESET + " <text>   - Print text to console");
        context.println(ANSI_GREEN + "  clear" + ANSI_RESET + "         - Clear screen");
        context.println(ANSI_GREEN + "  uptime" + ANSI_RESET + "        - Show system uptime");
        context.println(ANSI_GREEN + "  version" + ANSI_RESET + "       - Show Jinux version");
        context.println(ANSI_GREEN + "  history" + ANSI_RESET + "       - Show command history");
        context.println("");
        context.println(ANSI_BOLD + ANSI_CYAN + "Learning & Teaching:" + ANSI_RESET);
        context.println(ANSI_GREEN + "  quiz" + ANSI_RESET + "          - Interactive knowledge quiz (125 questions)");
        context.println(ANSI_GREEN + "  lab" + ANSI_RESET + "           - Hands-on simulation experiments");
        context.println(ANSI_GREEN + "  explain" + ANSI_RESET + " [topic] - Explain OS concepts with analogies");
        context.println(ANSI_GREEN + "  visualize" + ANSI_RESET + "     - ASCII visualization of OS internals");
        context.println(ANSI_GREEN + "  progress" + ANSI_RESET + "      - View learning progress & achievements");
        context.println("");
        context.println(ANSI_GREEN + "  exit" + ANSI_RESET + "          - Exit shell");
        context.println("");
    }
}
