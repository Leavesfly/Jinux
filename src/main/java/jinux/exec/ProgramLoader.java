package jinux.exec;

import jinux.kernel.Task;
import jinux.mm.AddressSpace;
import jinux.include.Const;

import java.util.HashMap;
import java.util.Map;

/**
 * 程序加载器
 * 对应 Linux 0.01 中的 fs/exec.c
 * 
 * 负责加载和执行程序
 * 
 * @author Jinux Project
 */
public class ProgramLoader {
    
    /** 已注册的程序表（模拟可执行文件） */
    private static final Map<String, ExecutableProgram> programs = new HashMap<>();
    
    static {
        // 注册内置程序
        registerProgram("hello", new HelloProgram());
        registerProgram("echo", new EchoProgram());
        registerProgram("loop", new LoopProgram());
        registerProgram("fork_test", new ForkTestProgram());
    }
    
    /**
     * 注册可执行程序
     */
    public static void registerProgram(String name, ExecutableProgram program) {
        programs.put(name, program);
        System.out.println("[EXEC] Registered program: " + name);
    }
    
    /**
     * 加载并执行程序
     * 
     * @param task 目标进程
     * @param pathname 程序路径
     * @param argv 参数数组
     * @param envp 环境变量数组
     * @return 0 成功，负数表示错误
     */
    public static int loadProgram(Task task, String pathname, String[] argv, String[] envp) {
        System.out.println("[EXEC] Loading program: " + pathname + " for pid=" + task.getPid());
        
        // 查找程序
        ExecutableProgram program = programs.get(pathname);
        if (program == null) {
            System.err.println("[EXEC] Program not found: " + pathname);
            return -Const.ENOENT;
        }
        
        // 清理当前地址空间
        AddressSpace addrSpace = task.getAddressSpace();
        if (addrSpace != null) {
            // 在实际实现中应该释放旧的内存
            System.out.println("[EXEC] Cleaning old address space");
        }
        
        // 创建新的可执行代码
        Runnable executable = program.createExecutable(task, argv, envp);
        
        // 设置进程的可执行代码
        task.setExecutable(executable);
        
        // 重置进程状态
        task.resetForExec();
        
        System.out.println("[EXEC] Program loaded successfully: " + pathname);
        
        return 0;
    }
    
    /**
     * 可执行程序接口
     */
    public interface ExecutableProgram {
        /**
         * 创建可执行代码
         * 
         * @param task 目标进程
         * @param argv 参数数组
         * @param envp 环境变量
         * @return Runnable 对象
         */
        Runnable createExecutable(Task task, String[] argv, String[] envp);
    }
    
    /**
     * Hello World 程序
     */
    static class HelloProgram implements ExecutableProgram {
        @Override
        public Runnable createExecutable(Task task, String[] argv, String[] envp) {
            return () -> {
                System.out.println("[PID " + task.getPid() + "] Hello from Jinux!");
                System.out.println("[PID " + task.getPid() + "] This is a loaded program");
                
                // 显示参数
                if (argv != null && argv.length > 0) {
                    System.out.print("[PID " + task.getPid() + "] Arguments: ");
                    for (String arg : argv) {
                        System.out.print(arg + " ");
                    }
                    System.out.println();
                }
                
                // 模拟执行
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                System.out.println("[PID " + task.getPid() + "] Hello program completed");
                task.exit(0);
            };
        }
    }
    
    /**
     * Echo 程序 - 回显参数
     */
    static class EchoProgram implements ExecutableProgram {
        @Override
        public Runnable createExecutable(Task task, String[] argv, String[] envp) {
            return () -> {
                if (argv != null && argv.length > 1) {
                    System.out.print("[PID " + task.getPid() + "] ");
                    for (int i = 1; i < argv.length; i++) {
                        System.out.print(argv[i]);
                        if (i < argv.length - 1) {
                            System.out.print(" ");
                        }
                    }
                    System.out.println();
                } else {
                    System.out.println("[PID " + task.getPid() + "] ");
                }
                
                task.exit(0);
            };
        }
    }
    
    /**
     * 循环程序 - 演示长时间运行
     */
    static class LoopProgram implements ExecutableProgram {
        @Override
        public Runnable createExecutable(Task task, String[] argv, String[] envp) {
            return () -> {
                int count = 5;
                if (argv != null && argv.length > 1) {
                    try {
                        count = Integer.parseInt(argv[1]);
                    } catch (NumberFormatException e) {
                        count = 5;
                    }
                }
                
                System.out.println("[PID " + task.getPid() + "] Loop program starting, count=" + count);
                
                for (int i = 1; i <= count; i++) {
                    System.out.println("[PID " + task.getPid() + "] Iteration " + i + "/" + count);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                
                System.out.println("[PID " + task.getPid() + "] Loop program completed");
                task.exit(0);
            };
        }
    }
    
    /**
     * Fork 测试程序 - 演示进程创建
     */
    static class ForkTestProgram implements ExecutableProgram {
        @Override
        public Runnable createExecutable(Task task, String[] argv, String[] envp) {
            return () -> {
                System.out.println("[PID " + task.getPid() + "] Fork test program started");
                System.out.println("[PID " + task.getPid() + "] This program demonstrates process creation");
                
                // 在实际实现中会调用 fork()
                System.out.println("[PID " + task.getPid() + "] In a real implementation, this would:");
                System.out.println("[PID " + task.getPid() + "] 1. Call fork() to create child process");
                System.out.println("[PID " + task.getPid() + "] 2. Parent and child execute different code");
                System.out.println("[PID " + task.getPid() + "] 3. Parent waits for child to complete");
                
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                
                System.out.println("[PID " + task.getPid() + "] Fork test completed");
                task.exit(0);
            };
        }
    }
    
    /**
     * 列出所有可用程序
     */
    public static void listPrograms() {
        System.out.println("\n========== Available Programs ==========");
        for (String name : programs.keySet()) {
            System.out.println("  " + name);
        }
        System.out.println("========================================\n");
    }
}
