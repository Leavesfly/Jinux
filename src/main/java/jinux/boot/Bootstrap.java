package jinux.boot;

import jinux.kernel.Kernel;

/**
 * Jinux 启动引导
 * 对应 Linux 0.01 中的 boot/bootsect.s 和 boot/setup.s
 * 
 * Java 版本简化为直接从 main 方法启动
 * 
 * @author Jinux Project
 */
public class Bootstrap {
    
    /**
     * 主入口
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        // 打印启动信息
        printBanner();
        
        // 创建并初始化内核
        Kernel kernel = new Kernel();
        kernel.init();
        
        // 创建 init 进程
        kernel.createInitProcess();
        
        // 启动系统
        kernel.start();
        
        // 系统退出
        System.out.println("\nGoodbye!");
        System.exit(0);
    }
    
    /**
     * 打印启动横幅
     */
    private static void printBanner() {
        System.out.println("\n" +
            "     _ _                  \n" +
            "    | (_)                 \n" +
            "    | |_ _ __  _   ___  __\n" +
            " _  | | | '_ \\| | | \\ \\/ /\n" +
            "| |_| | | | | | |_| |>  < \n" +
            " \\___/|_|_| |_|\\__,_/_/\\_\\\n" +
            "\n" +
            "Jinux - A Java Implementation of Linux 0.01\n" +
            "Version 0.01-alpha\n" +
            "Copyright (c) 2024 Jinux Project\n" +
            "\nBooting...\n");
    }
}
