package jinux.shell;

/**
 * Shell 命令接口
 * 所有 Shell 内置命令都应实现此接口，遵循命令模式（Command Pattern）
 */
public interface Command {
    /**
     * 获取命令名称
     * @return 命令名称（如 "help", "ps", "exit"）
     */
    String getName();
    
    /**
     * 获取命令描述
     * @return 命令的简短描述
     */
    String getDescription();
    
    /**
     * 获取命令用法
     * @return 命令的使用方法说明
     */
    String getUsage();
    
    /**
     * 执行命令
     * @param args 命令参数（不包含命令名本身）
     * @param context 命令执行上下文
     */
    void execute(String[] args, ShellContext context);
}
