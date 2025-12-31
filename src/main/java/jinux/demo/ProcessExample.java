package jinux.demo;

import jinux.kernel.Kernel;
import jinux.kernel.Task;
import jinux.exec.ProgramLoader;

/**
 * 进程创建和程序执行演示
 * 
 * @author Jinux Project
 */
public class ProcessExample {
    
    /**
     * 演示进程创建和程序加载
     */
    public static void demo(Kernel kernel) {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        
        console.println("\n========== Process Creation & Execution Demo ==========\n");
        
        // 1. 列出可用程序
        console.println("1. Available programs:");
        ProgramLoader.listPrograms();
        
        // 2. 演示程序加载
        console.println("2. Demonstrating program loading:");
        
        Task currentTask = kernel.getScheduler().getCurrentTask();
        if (currentTask == null) {
            console.println("   [ERROR] No current task");
            return;
        }
        
        console.println("   Current process PID: " + currentTask.getPid());
        console.println("   Loading program 'hello'...\n");
        
        // 创建一个新进程来演示
        int childPid = kernel.getScheduler().allocatePid();
        var childAddrSpace = kernel.getMemoryManager().createAddressSpace();
        Task childTask = new Task(childPid, currentTask.getPid(), childAddrSpace);
        
        // 加载程序到新进程
        int result = ProgramLoader.loadProgram(childTask, "hello", 
            new String[]{"hello", "arg1", "arg2"}, null);
        
        if (result == 0) {
            console.println("   ✓ Program loaded successfully");
            console.println("   ✓ Child process created: PID " + childPid);
            
            // 添加到调度器
            if (kernel.getScheduler().addTask(childTask)) {
                console.println("   ✓ Process added to scheduler");
                
                // 启动程序执行
                Runnable executable = childTask.getExecutable();
                if (executable != null) {
                    console.println("   ✓ Starting program execution...\n");
                    
                    Thread thread = new Thread(executable, "demo-process-" + childPid);
                    childTask.setExecutionThread(thread);
                    thread.start();
                    
                    // 等待程序执行完成
                    try {
                        thread.join(2000); // 最多等待2秒
                        console.println("\n   ✓ Program execution completed");
                    } catch (InterruptedException e) {
                        console.println("\n   [WARN] Program execution interrupted");
                    }
                } else {
                    console.println("   [ERROR] No executable code");
                }
            } else {
                console.println("   [ERROR] Failed to add process to scheduler");
            }
        } else {
            console.println("   [ERROR] Failed to load program: " + result);
        }
        
        console.println("\n========================================\n");
    }
    
    /**
     * 演示 fork + exec 模式
     */
    public static void demoForkExec(Kernel kernel) {
        var console = kernel.getConsole();
        
        console.println("\n========== Fork + Exec Pattern Demo ==========\n");
        
        console.println("Typical Unix process creation pattern:");
        console.println("1. Parent calls fork() to create child process");
        console.println("2. Child process inherits parent's memory");
        console.println("3. Child calls execve() to load new program");
        console.println("4. New program replaces child's memory");
        console.println("5. Parent can wait() for child to complete\n");
        
        console.println("Code example:");
        console.println("  int pid = fork();");
        console.println("  if (pid == 0) {");
        console.println("    // Child process");
        console.println("    execve(\"/bin/ls\", argv, envp);");
        console.println("    // If execve succeeds, this code never executes");
        console.println("  } else {");
        console.println("    // Parent process");
        console.println("    wait(&status);");
        console.println("    printf(\"Child exited with status %d\\n\", status);");
        console.println("  }\n");
        
        console.println("========================================\n");
    }
}
