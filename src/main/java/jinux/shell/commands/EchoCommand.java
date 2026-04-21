package jinux.shell.commands;

import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * echo 命令 - 打印文本
 */
public class EchoCommand implements Command {
    
    @Override
    public String getName() {
        return "echo";
    }
    
    @Override
    public String getDescription() {
        return "Print text to console";
    }
    
    @Override
    public String getUsage() {
        return "echo <text>";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(" ");
            sb.append(args[i]);
        }
        context.println(sb.toString());
    }
}
