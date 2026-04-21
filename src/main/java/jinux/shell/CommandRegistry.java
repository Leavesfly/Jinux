package jinux.shell;

import java.util.*;

/**
 * 命令注册表
 * 管理所有已注册的 Shell 命令，支持按名称查找和模糊匹配建议
 */
public class CommandRegistry {
    private final Map<String, Command> commands = new LinkedHashMap<>();
    
    /**
     * 注册命令
     * @param command 要注册的命令实例
     */
    public void register(Command command) {
        commands.put(command.getName(), command);
    }
    
    /**
     * 根据名称获取命令
     * @param name 命令名称
     * @return 命令实例，如果不存在则返回 null
     */
    public Command getCommand(String name) {
        return commands.get(name);
    }
    
    /**
     * 获取所有已注册的命令
     * @return 不可修改的命令集合
     */
    public Collection<Command> getAllCommands() {
        return Collections.unmodifiableCollection(commands.values());
    }
    
    /**
     * 检查是否已注册指定名称的命令
     * @param name 命令名称
     * @return 如果已注册返回 true，否则返回 false
     */
    public boolean hasCommand(String name) {
        return commands.containsKey(name);
    }
    
    /**
     * 根据输入查找最相似的命令名（用于 "Did you mean?" 提示）
     * 使用 Levenshtein 距离算法
     * @param input 用户输入的命令名
     * @return 最相似的命令名，如果没有足够相似的则返回 null
     */
    public String findSimilarCommand(String input) {
        String bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;
        int threshold = 3;
        
        for (String cmdName : commands.keySet()) {
            int distance = levenshteinDistance(input, cmdName);
            if (distance < bestDistance && distance <= threshold) {
                bestDistance = distance;
                bestMatch = cmdName;
            }
        }
        return bestMatch;
    }
    
    /**
     * 计算两个字符串的 Levenshtein 编辑距离
     * @param source 源字符串
     * @param target 目标字符串
     * @return 编辑距离
     */
    private int levenshteinDistance(String source, String target) {
        int sourceLength = source.length();
        int targetLength = target.length();
        int[][] dp = new int[sourceLength + 1][targetLength + 1];
        
        for (int i = 0; i <= sourceLength; i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= targetLength; j++) {
            dp[0][j] = j;
        }
        
        for (int i = 1; i <= sourceLength; i++) {
            for (int j = 1; j <= targetLength; j++) {
                int cost = (source.charAt(i - 1) == target.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(
                    Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                );
            }
        }
        return dp[sourceLength][targetLength];
    }
}
