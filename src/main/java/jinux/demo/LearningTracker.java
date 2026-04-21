package jinux.demo;

import jinux.kernel.Kernel;
import jinux.drivers.ConsoleDevice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Jinux 学习进度追踪器
 * 记录学生的学习情况并提供个性化建议
 * 
 * @author Jinux Project
 */
public class LearningTracker {
    
    // ANSI 颜色常量
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_YELLOW = "\033[33m";
    private static final String ANSI_BLUE = "\033[34m";
    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_BOLD = "\033[1m";
    private static final String ANSI_GRAY = "\033[90m";
    
    // 单例实例
    private static LearningTracker instance;
    
    // 数据字段
    private Map<String, int[]> moduleScores;
    private Set<String> completedLabs;
    private Set<String> viewedConcepts;
    private Set<String> usedVisualizations;
    private long startTime;
    private int totalCorrect;
    private int totalQuestions;
    private int consecutiveCorrect;
    private int maxConsecutiveCorrect;
    
    /**
     * 私有构造器 - 单例模式
     */
    private LearningTracker() {
        this.moduleScores = new HashMap<>();
        this.completedLabs = new HashSet<>();
        this.viewedConcepts = new HashSet<>();
        this.usedVisualizations = new HashSet<>();
        this.startTime = System.currentTimeMillis();
        this.totalCorrect = 0;
        this.totalQuestions = 0;
        this.consecutiveCorrect = 0;
        this.maxConsecutiveCorrect = 0;
        
        // 初始化模块统计
        String[] modules = {"PROCESS", "MEMORY", "FILESYSTEM", "SIGNAL", "IPC"};
        for (String module : modules) {
            moduleScores.put(module, new int[]{0, 0});
        }
    }
    
    /**
     * 获取单例实例
     */
    public static LearningTracker getInstance() {
        if (instance == null) {
            instance = new LearningTracker();
        }
        return instance;
    }
    
    /**
     * 记录答题结果
     * @param module 模块名称
     * @param correct 是否正确
     */
    public void recordQuizResult(String module, boolean correct) {
        int[] scores = moduleScores.get(module);
        if (scores == null) {
            scores = new int[]{0, 0};
            moduleScores.put(module, scores);
        }
        
        scores[1]++; // 总数增加
        if (correct) {
            scores[0]++; // 正确数增加
            totalCorrect++;
            consecutiveCorrect++;
            if (consecutiveCorrect > maxConsecutiveCorrect) {
                maxConsecutiveCorrect = consecutiveCorrect;
            }
        } else {
            consecutiveCorrect = 0; // 重置连续正确
        }
        totalQuestions++;
    }
    
    /**
     * 记录实验完成
     * @param labName 实验名称
     */
    public void recordLabCompletion(String labName) {
        completedLabs.add(labName);
    }
    
    /**
     * 记录概念查看
     * @param concept 概念名称
     */
    public void recordConceptViewed(String concept) {
        viewedConcepts.add(concept);
    }
    
    /**
     * 记录可视化使用
     * @param vizName 可视化工具名称
     */
    public void recordVisualizationUsed(String vizName) {
        usedVisualizations.add(vizName);
    }
    
    /**
     * 显示学习进度总览
     * @param kernel 内核实例
     */
    public void showProgress(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        
        long elapsedMinutes = (System.currentTimeMillis() - startTime) / 60000;
        double accuracy = totalQuestions > 0 ? (double) totalCorrect / totalQuestions * 100 : 0;
        
        console.println("");
        console.println(ANSI_BOLD + ANSI_CYAN + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "║           Jinux 学习进度总览                           ║" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        console.println("");
        
        // 学习时长
        console.println("  " + ANSI_BLUE + "⏱ 学习时长:" + ANSI_RESET + " " + elapsedMinutes + " 分钟");
        console.println("");
        
        // 总答题数和正确率
        console.println("  " + ANSI_BLUE + "📊 总体表现:" + ANSI_RESET);
        console.println("     总答题数: " + totalQuestions);
        console.println("     正确数: " + totalCorrect);
        console.println("     正确率: " + String.format("%.1f%%", accuracy));
        console.println("");
        
        // 每个模块的掌握度
        console.println("  " + ANSI_BLUE + "📚 模块掌握度:" + ANSI_RESET);
        for (Map.Entry<String, int[]> entry : moduleScores.entrySet()) {
            String moduleName = entry.getKey();
            int[] scores = entry.getValue();
            int correct = scores[0];
            int total = scores[1];
            
            double moduleAccuracy = total > 0 ? (double) correct / total * 100 : 0;
            int progressBars = (int) (moduleAccuracy / 10);
            
            StringBuilder bar = new StringBuilder();
            for (int i = 0; i < 10; i++) {
                if (i < progressBars) {
                    bar.append("█");
                } else {
                    bar.append("░");
                }
            }
            
            int stars = (int) (moduleAccuracy / 20);
            if (stars > 5) stars = 5;
            StringBuilder starStr = new StringBuilder();
            for (int i = 0; i < stars; i++) {
                starStr.append("★");
            }
            for (int i = stars; i < 5; i++) {
                starStr.append("☆");
            }
            
            String color = moduleAccuracy >= 80 ? ANSI_GREEN : (moduleAccuracy >= 60 ? ANSI_YELLOW : ANSI_RED);
            console.println("     " + moduleName + ": " + color + bar.toString() + ANSI_RESET + " " + String.format("%.0f%%", moduleAccuracy) + " " + starStr);
        }
        console.println("");
        
        // 其他统计
        console.println("  " + ANSI_BLUE + "🎯 学习进度:" + ANSI_RESET);
        console.println("     实验完成: " + completedLabs.size() + "/5");
        console.println("     概念学习: " + viewedConcepts.size() + "/12");
        console.println("     可视化使用: " + usedVisualizations.size() + "/8");
        console.println("");
    }
    
    /**
     * 显示详细报告
     * @param kernel 内核实例
     */
    public void showDetailedReport(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        
        long elapsedMinutes = (System.currentTimeMillis() - startTime) / 60000;
        
        console.println("");
        console.println(ANSI_BOLD + ANSI_CYAN + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "║           Jinux 学习详细报告                           ║" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        console.println("");
        
        // 每个模块的详细统计
        console.println("  " + ANSI_BOLD + "📊 模块详细统计:" + ANSI_RESET);
        console.println("");
        
        String strongestModule = "";
        double strongestAccuracy = -1;
        String weakestModule = "";
        double weakestAccuracy = 101;
        
        for (Map.Entry<String, int[]> entry : moduleScores.entrySet()) {
            String moduleName = entry.getKey();
            int[] scores = entry.getValue();
            int correct = scores[0];
            int total = scores[1];
            
            double accuracy = total > 0 ? (double) correct / total * 100 : 0;
            
            console.println("  " + ANSI_BLUE + moduleName + ":" + ANSI_RESET);
            console.println("     正确/总数: " + correct + "/" + total);
            console.println("     正确率: " + String.format("%.1f%%", accuracy));
            console.println("");
            
            if (total > 0) {
                if (accuracy > strongestAccuracy) {
                    strongestAccuracy = accuracy;
                    strongestModule = moduleName;
                }
                if (accuracy < weakestAccuracy) {
                    weakestAccuracy = accuracy;
                    weakestModule = moduleName;
                }
            }
        }
        
        // 最强和最弱模块
        if (!strongestModule.isEmpty()) {
            console.println("  " + ANSI_GREEN + "💪 最强模块: " + strongestModule + " (" + String.format("%.1f%%", strongestAccuracy) + ")" + ANSI_RESET);
        }
        if (!weakestModule.isEmpty()) {
            console.println("  " + ANSI_RED + "📉 最弱模块: " + weakestModule + " (" + String.format("%.1f%%", weakestAccuracy) + ")" + ANSI_RESET);
        }
        console.println("");
        
        // 学习时间分析
        console.println("  " + ANSI_BLUE + "⏱ 学习时间分析:" + ANSI_RESET);
        console.println("     总学习时长: " + elapsedMinutes + " 分钟");
        if (totalQuestions > 0) {
            double avgTimePerQuestion = (double) elapsedMinutes / totalQuestions;
            console.println("     平均每题耗时: " + String.format("%.1f", avgTimePerQuestion) + " 分钟");
        }
        console.println("");
    }
    
    /**
     * 显示个性化学习建议
     * @param kernel 内核实例
     */
    public void showRecommendations(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        
        console.println("");
        console.println(ANSI_BOLD + ANSI_CYAN + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "║           Jinux 个性化学习建议                         ║" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        console.println("");
        
        List<String> recommendations = new ArrayList<>();
        
        // 检查每个模块的正确率
        boolean allModulesStarted = true;
        boolean allModulesHighAccuracy = true;
        
        for (Map.Entry<String, int[]> entry : moduleScores.entrySet()) {
            String moduleName = entry.getKey();
            int[] scores = entry.getValue();
            int correct = scores[0];
            int total = scores[1];
            
            if (total == 0) {
                allModulesStarted = false;
                recommendations.add("[" + moduleName + "] 建议从 quiz 命令开始学习该模块");
            } else {
                double accuracy = (double) correct / total * 100;
                if (accuracy < 50) {
                    recommendations.add("[" + moduleName + "] 建议先使用 explain <topic> 查看概念解释，再做题巩固");
                }
                if (accuracy <= 80) {
                    allModulesHighAccuracy = false;
                }
            }
        }
        
        // 如果所有模块都 > 80%
        if (allModulesHighAccuracy && allModulesStarted) {
            recommendations.add("恭喜！建议尝试 lab 命令进行高级实验");
        }
        
        // 如果实验完成数 < 3
        if (completedLabs.size() < 3) {
            recommendations.add("建议使用 lab 命令完成更多实验（当前完成: " + completedLabs.size() + "/5）");
        }
        
        // 输出建议
        if (recommendations.isEmpty()) {
            console.println("  " + ANSI_GREEN + "✓ 你做得很好！继续保持学习节奏。" + ANSI_RESET);
        } else {
            console.println("  " + ANSI_YELLOW + "💡 学习建议:" + ANSI_RESET);
            console.println("");
            for (int i = 0; i < recommendations.size(); i++) {
                console.println("     " + (i + 1) + ". " + recommendations.get(i));
            }
        }
        console.println("");
        
        // 推荐下一步学习内容
        console.println("  " + ANSI_BLUE + "🎯 推荐下一步:" + ANSI_RESET);
        String nextStep = recommendNextStep();
        console.println("     " + nextStep);
        console.println("");
    }
    
    /**
     * 推荐下一步学习内容
     */
    private String recommendNextStep() {
        // 找出正确率最低的模块
        String weakestModule = "";
        double weakestAccuracy = 101;
        int weakestTotal = 0;
        
        for (Map.Entry<String, int[]> entry : moduleScores.entrySet()) {
            int[] scores = entry.getValue();
            int total = scores[1];
            
            if (total == 0) {
                return "开始学习新模块: " + entry.getKey() + " (使用 quiz " + entry.getKey().toLowerCase() + ")";
            }
            
            double accuracy = (double) scores[0] / total * 100;
            if (accuracy < weakestAccuracy) {
                weakestAccuracy = accuracy;
                weakestModule = entry.getKey();
                weakestTotal = total;
            }
        }
        
        if (weakestAccuracy < 60) {
            return "加强薄弱环节: " + weakestModule + " (正确率 " + String.format("%.0f%%", weakestAccuracy) + "，建议复习概念)";
        } else if (completedLabs.size() < 5) {
            return "完成实验: 还有 " + (5 - completedLabs.size()) + " 个实验未完成";
        } else if (viewedConcepts.size() < 12) {
            return "学习概念: 还有 " + (12 - viewedConcepts.size()) + " 个概念未查看";
        } else {
            return "挑战成就系统: 尝试解锁更多成就！";
        }
    }
    
    /**
     * 显示成就系统
     * @param kernel 内核实例
     */
    public void showAchievements(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        
        console.println("");
        console.println(ANSI_BOLD + ANSI_CYAN + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "║           Jinux 成就系统                               ║" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
        console.println("");
        
        // 定义成就
        Achievement[] achievements = {
            new Achievement("🌱 初学者", "完成第一道题", totalQuestions >= 1),
            new Achievement("📚 好学生", "答对 10 道题", totalCorrect >= 10),
            new Achievement("🎯 神枪手", "连续答对 5 道题", maxConsecutiveCorrect >= 5),
            new Achievement("💯 满分王", "某个模块正确率 100% 且至少答了5题", checkPerfectScore()),
            new Achievement("🧪 实验家", "完成第一个实验", completedLabs.size() >= 1),
            new Achievement("🔬 科学家", "完成所有 5 个实验", completedLabs.size() >= 5),
            new Achievement("📖 知识渊博", "查看所有 12 个概念解释", viewedConcepts.size() >= 12),
            new Achievement("🎨 可视化达人", "使用所有 8 个可视化工具", usedVisualizations.size() >= 8),
            new Achievement("🌟 全能选手", "所有模块正确率 > 60% 且每个模块至少答了3题", checkAllAbove(60, 3)),
            new Achievement("🏆 操作系统大师", "所有模块正确率 > 80% 且完成所有实验且每个模块至少答了5题", checkMaster())
        };
        
        int unlockedCount = 0;
        for (Achievement achievement : achievements) {
            String color = achievement.unlocked ? ANSI_GREEN : ANSI_GRAY;
            String status = achievement.unlocked ? "✓ 已解锁" : "○ 未解锁";
            console.println("  " + color + achievement.icon + " " + achievement.name + ANSI_RESET);
            console.println("     " + achievement.description);
            console.println("     " + color + status + ANSI_RESET);
            console.println("");
            
            if (achievement.unlocked) {
                unlockedCount++;
            }
        }
        
        console.println("  " + ANSI_BLUE + "总计: " + unlockedCount + "/" + achievements.length + " 个成就已解锁" + ANSI_RESET);
        console.println("");
    }
    
    /**
     * 检查是否有模块达到完美分数
     */
    private boolean checkPerfectScore() {
        for (int[] scores : moduleScores.values()) {
            if (scores[1] >= 5 && scores[0] == scores[1]) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 检查所有模块是否都超过指定正确率且至少答了指定题数
     */
    private boolean checkAllAbove(double minAccuracy, int minQuestions) {
        for (Map.Entry<String, int[]> entry : moduleScores.entrySet()) {
            int[] scores = entry.getValue();
            if (scores[1] < minQuestions) {
                return false;
            }
            double accuracy = (double) scores[0] / scores[1] * 100;
            if (accuracy <= minAccuracy) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 检查是否达到大师级别
     */
    private boolean checkMaster() {
        if (completedLabs.size() < 5) {
            return false;
        }
        return checkAllAbove(80, 5);
    }
    
    /**
     * 显示进度菜单
     * @param kernel 内核实例
     */
    public void showMenu(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        while (true) {
            console.println("");
            console.println(ANSI_BOLD + ANSI_CYAN + "╔══════════════════════════════════════════════════════════╗" + ANSI_RESET);
            console.println(ANSI_BOLD + ANSI_CYAN + "║           Jinux 学习进度菜单                           ║" + ANSI_RESET);
            console.println(ANSI_BOLD + ANSI_CYAN + "╚══════════════════════════════════════════════════════════╝" + ANSI_RESET);
            console.println("");
            console.println("  1. 学习进度总览");
            console.println("  2. 详细报告");
            console.println("  3. 学习建议");
            console.println("  4. 成就系统");
            console.println("  5. 返回");
            console.println("");
            console.print("  请选择 (1-5): ");
            
            try {
                String choice = reader.readLine();
                if (choice == null) {
                    break;
                }
                
                switch (choice.trim()) {
                    case "1":
                        showProgress(kernel);
                        break;
                    case "2":
                        showDetailedReport(kernel);
                        break;
                    case "3":
                        showRecommendations(kernel);
                        break;
                    case "4":
                        showAchievements(kernel);
                        break;
                    case "5":
                        return;
                    default:
                        console.println(ANSI_RED + "  无效选择，请重新输入" + ANSI_RESET);
                }
            } catch (Exception e) {
                console.println(ANSI_RED + "  读取输入失败: " + e.getMessage() + ANSI_RESET);
                break;
            }
        }
    }
    
    /**
     * 成就内部类
     */
    private static class Achievement {
        String icon;
        String name;
        String description;
        boolean unlocked;
        
        Achievement(String icon, String name, String description, boolean unlocked) {
            this.icon = icon;
            this.name = name;
            this.description = description;
            this.unlocked = unlocked;
        }
        
        Achievement(String icon, String name, boolean unlocked) {
            this(icon, name, "", unlocked);
        }
    }
}
