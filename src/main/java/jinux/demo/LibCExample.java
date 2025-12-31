package jinux.demo;

import jinux.kernel.Kernel;
import jinux.lib.LibC;

/**
 * LibC 使用示例
 * 演示如何使用用户态库封装的系统调用
 * 
 * @author Jinux Project
 */
public class LibCExample {
    
    /**
     * 演示 LibC 的使用
     * 
     * @param kernel 内核实例
     */
    public static void demo(Kernel kernel) {
        // 创建 LibC 实例
        LibC libc = new LibC(kernel.getSyscallDispatcher());
        
        System.out.println("\n========== LibC 使用示例 ==========\n");
        
        // 1. 进程管理
        System.out.println("1. 进程管理系统调用:");
        int pid = libc.getpid();
        int ppid = libc.getppid();
        System.out.println("   当前进程 PID: " + pid);
        System.out.println("   父进程 PPID: " + ppid);
        
        // 2. 文件 I/O（使用封装后的 API）
        System.out.println("\n2. 文件 I/O 系统调用:");
        libc.println("   使用 libc.println() 输出到标准输出");
        libc.print("   使用 libc.print() 输出");
        System.out.println(" (无换行)");
        
        // 3. 内存管理
        System.out.println("\n3. 内存管理系统调用:");
        long addr = 0x20000;
        long newBrk = libc.brk(addr);
        System.out.println("   brk(0x" + Long.toHexString(addr) + 
            ") = 0x" + Long.toHexString(newBrk));
        
        // 4. 简化的 malloc
        long allocAddr = libc.malloc(4096);
        System.out.println("   malloc(4096) = 0x" + Long.toHexString(allocAddr));
        
        // 5. 时间
        System.out.println("\n4. 时间系统调用:");
        long timestamp = libc.time();
        System.out.println("   time() = " + timestamp + " (Unix 时间戳)");
        
        System.out.println("\n=====================================\n");
    }
    
    /**
     * 对比直接调用和 LibC 封装
     */
    public static void compareWithDirectCall(Kernel kernel) {
        LibC libc = new LibC(kernel.getSyscallDispatcher());
        
        System.out.println("\n========== 调用方式对比 ==========\n");
        
        // 方式1：直接调用 SystemCallDispatcher（内核态接口）
        System.out.println("方式1 - 直接调用 SystemCallDispatcher:");
        long result1 = kernel.getSyscallDispatcher().dispatch(
            jinux.include.Syscalls.SYS_GETPID, 0, 0, 0);
        System.out.println("  syscall.dispatch(SYS_GETPID) = " + result1);
        
        // 方式2：通过 LibC 调用（用户态接口）
        System.out.println("\n方式2 - 通过 LibC 封装调用:");
        int result2 = libc.getpid();
        System.out.println("  libc.getpid() = " + result2);
        
        System.out.println("\n结论：LibC 封装隐藏了系统调用的底层细节，");
        System.out.println("      提供了更易用的 API 接口。");
        
        System.out.println("\n==================================\n");
    }
}

