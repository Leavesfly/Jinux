package jinux.demo;

import jinux.kernel.Kernel;
import jinux.kernel.Task;
import jinux.kernel.Scheduler;
import jinux.mm.AddressSpace;
import jinux.ipc.Pipe;
import jinux.kernel.Signal;
import jinux.lib.LibC;

/**
 * 交互式演示程序
 * 展示系统交互能力
 * 
 * @author Jinux Project
 */
public class InteractiveDemo {
    
    /**
     * 运行交互式演示菜单
     */
    public static void runInteractiveDemo(Kernel kernel) {
        var console = kernel.getConsole();
        
        console.println("\n");
        console.println("╔════════════════════════════════════════════════════════════╗");
        console.println("║              Interactive Demonstration Menu               ║");
        console.println("╚════════════════════════════════════════════════════════════╝");
        console.println("");
        
        // 1. 实时进程监控
        demonstrateProcessMonitoring(kernel);
        
        // 2. 内存分配演示
        demonstrateMemoryAllocation(kernel);
        
        // 3. 管道通信演示
        demonstratePipeCommunication(kernel);
        
        // 4. 系统调用链演示
        demonstrateSyscallChain(kernel);
        
        console.println("\n[DEMO] Interactive demonstration completed!\n");
    }
    
    /**
     * 演示进程监控
     */
    private static void demonstrateProcessMonitoring(Kernel kernel) {
        var console = kernel.getConsole();
        Scheduler scheduler = kernel.getScheduler();
        
        console.println("\n[DEMO] Process Monitoring Simulation:");
        console.println("  Simulating process state changes over time...\n");
        
        Task currentTask = scheduler.getCurrentTask();
        if (currentTask != null) {
            // 模拟进程状态变化
            for (int i = 0; i < 5; i++) {
                int oldCounter = currentTask.getCounter();
                currentTask.setCounter(Math.max(0, oldCounter - 2));
                
                console.println("  Tick " + (i + 1) + ":");
                console.println("    PID:     " + currentTask.getPid());
                console.println("    State:   " + currentTask.getStateName());
                console.println("    Counter: " + oldCounter + " -> " + currentTask.getCounter());
                
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            // 恢复
            currentTask.setCounter(15);
            console.println("  (Process counter restored)");
        }
        console.println("");
    }
    
    /**
     * 演示内存分配
     */
    private static void demonstrateMemoryAllocation(Kernel kernel) {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        LibC libc = new LibC(syscall);
        Task currentTask = kernel.getScheduler().getCurrentTask();
        
        console.println("\n[DEMO] Memory Allocation Pattern:");
        console.println("  Simulating typical memory allocation sequence...\n");
        
        if (currentTask != null) {
            AddressSpace addrSpace = currentTask.getAddressSpace();
            long initialBrk = addrSpace.getBrk();
            
            console.println("  Initial heap: 0x" + Long.toHexString(initialBrk));
            
            // 模拟多次分配
            long[] allocations = new long[5];
            for (int i = 0; i < 5; i++) {
                int size = (i + 1) * 512; // 512, 1024, 1536, 2048, 2560
                long addr = libc.malloc(size);
                allocations[i] = addr;
                console.println("  Allocation " + (i + 1) + ": malloc(" + size + ") = 0x" + Long.toHexString(addr));
            }
            
            console.println("\n  Memory layout after allocations:");
            console.println("    Code:  0x" + Long.toHexString(addrSpace.getCodeStart()) + 
                          " - 0x" + Long.toHexString(addrSpace.getCodeEnd()));
            console.println("    Data:  0x" + Long.toHexString(addrSpace.getDataStart()) + 
                          " - 0x" + Long.toHexString(addrSpace.getDataEnd()));
            console.println("    Heap:  0x" + Long.toHexString(initialBrk) + 
                          " - 0x" + Long.toHexString(addrSpace.getBrk()));
            console.println("    Stack: 0x" + Long.toHexString(addrSpace.getStackTop()));
            
            // 恢复 brk
            libc.brk(initialBrk);
            console.println("\n  (Heap restored)");
        }
        console.println("");
    }
    
    /**
     * 演示管道通信
     */
    private static void demonstratePipeCommunication(Kernel kernel) {
        var console = kernel.getConsole();
        
        console.println("\n[DEMO] Pipe Communication Pattern:");
        console.println("  Simulating producer-consumer communication...\n");
        
        Pipe pipe = new Pipe();
        console.println("  ✓ Pipe created");
        
        // 生产者写入
        console.println("\n  [Producer] Writing data:");
        String[] data = {
            "Data chunk 1",
            "Data chunk 2",
            "Data chunk 3"
        };
        
        for (String chunk : data) {
            byte[] bytes = (chunk + "\n").getBytes();
            int written = pipe.write(bytes, bytes.length);
            console.println("    → Wrote: \"" + chunk + "\" (" + written + " bytes)");
            console.println("      Pipe status: " + pipe.available() + " bytes available");
        }
        
        // 消费者读取
        console.println("\n  [Consumer] Reading data:");
        byte[] buffer = new byte[256];
        int totalRead = 0;
        
        while (pipe.available() > 0) {
            int read = pipe.read(buffer, buffer.length);
            if (read > 0) {
                totalRead += read;
                String content = new String(buffer, 0, read).trim();
                console.println("    ← Read: \"" + content + "\" (" + read + " bytes)");
            } else {
                break;
            }
        }
        
        console.println("\n  Communication summary:");
        console.println("    Total bytes transferred: " + totalRead);
        console.println("    Pipe buffer size: " + Pipe.PIPE_BUF);
        console.println("    Communication pattern: Producer-Consumer");
        
        console.println("");
    }
    
    /**
     * 演示系统调用链
     */
    private static void demonstrateSyscallChain(Kernel kernel) {
        var console = kernel.getConsole();
        var syscall = kernel.getSyscallDispatcher();
        LibC libc = new LibC(syscall);
        
        console.println("\n[DEMO] System Call Chain:");
        console.println("  Demonstrating typical system call sequences...\n");
        
        // 序列1: 进程信息查询
        console.println("  Sequence 1: Process Information Query");
        int pid = libc.getpid();
        int ppid = libc.getppid();
        long time = libc.time();
        console.println("    getpid()  → " + pid);
        console.println("    getppid() → " + ppid);
        console.println("    time()    → " + time);
        
        // 序列2: 内存管理
        console.println("\n  Sequence 2: Memory Management");
        long brk1 = libc.brk(0x20000);
        long brk2 = libc.brk(0x21000);
        long alloc = libc.malloc(1024);
        console.println("    brk(0x20000)    → 0x" + Long.toHexString(brk1));
        console.println("    brk(0x21000)    → 0x" + Long.toHexString(brk2));
        console.println("    malloc(1024)    → 0x" + Long.toHexString(alloc));
        
        // 序列3: 信号处理
        console.println("\n  Sequence 3: Signal Handling");
        long handler = libc.signal(Signal.SIGTERM, Signal.SIG_IGN);
        int ret = libc.kill(pid, Signal.SIGTERM);
        console.println("    signal(SIGTERM, SIG_IGN) → " + handler);
        console.println("    kill(pid, SIGTERM)       → " + ret);
        
        console.println("\n  System call chains demonstrate:");
        console.println("    ✓ Sequential system call execution");
        console.println("    ✓ State preservation between calls");
        console.println("    ✓ Error handling and return values");
        
        console.println("");
    }
}

