package jinux.demo;

import jinux.kernel.Kernel;
import jinux.ipc.Pipe;
import jinux.lib.LibC;

/**
 * 管道机制演示
 * 
 * @author Jinux Project
 */
public class PipeExample {
    
    /**
     * 演示管道的基本使用
     */
    public static void demo(Kernel kernel) {
        var console = kernel.getConsole();
        
        console.println("\n========== Pipe Mechanism Demo ==========\n");
        
        // 创建管道
        console.println("[DEMO] Creating a pipe...");
        Pipe pipe = new Pipe();
        console.println("[DEMO] Pipe created: " + pipe);
        
        // 写入数据
        console.println("\n[DEMO] Writing data to pipe...");
        String message = "Hello from pipe!";
        byte[] writeData = message.getBytes();
        int bytesWritten = pipe.write(writeData, writeData.length);
        console.println("[DEMO] Wrote " + bytesWritten + " bytes: \"" + message + "\"");
        console.println("[DEMO] Pipe status: " + pipe);
        
        // 读取数据
        console.println("\n[DEMO] Reading data from pipe...");
        byte[] readData = new byte[100];
        int bytesRead = pipe.read(readData, readData.length);
        if (bytesRead > 0) {
            String readMessage = new String(readData, 0, bytesRead);
            console.println("[DEMO] Read " + bytesRead + " bytes: \"" + readMessage + "\"");
        }
        console.println("[DEMO] Pipe status: " + pipe);
        
        // 测试管道容量
        console.println("\n[DEMO] Testing pipe capacity...");
        console.println("[DEMO] Pipe buffer size: " + Pipe.PIPE_BUF + " bytes");
        
        // 写入大量数据测试
        byte[] largeData = new byte[2048];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) ('A' + (i % 26));
        }
        
        console.println("[DEMO] Writing 2048 bytes to pipe...");
        int written = pipe.write(largeData, largeData.length);
        console.println("[DEMO] Actually wrote: " + written + " bytes");
        console.println("[DEMO] Pipe available: " + pipe.available() + " bytes");
        
        // 分批读取
        console.println("\n[DEMO] Reading back in chunks...");
        byte[] chunk = new byte[512];
        int totalRead = 0;
        while (pipe.available() > 0) {
            int n = pipe.read(chunk, chunk.length);
            if (n > 0) {
                totalRead += n;
                console.println("[DEMO]   Read chunk: " + n + " bytes");
            } else {
                break;
            }
        }
        console.println("[DEMO] Total read: " + totalRead + " bytes");
        
        // 测试关闭
        console.println("\n[DEMO] Testing pipe close...");
        console.println("[DEMO] Closing write end...");
        pipe.closeWrite();
        console.println("[DEMO] Pipe status: " + pipe);
        
        console.println("[DEMO] Attempting to read (should return 0/EOF)...");
        int eofRead = pipe.read(readData, 10);
        console.println("[DEMO] Read returned: " + eofRead + " (0 = EOF)");
        
        console.println("\n========== Pipe Demo Complete ==========\n");
    }
    
    /**
     * 演示使用 LibC 的 pipe 系统调用
     */
    public static void demoWithLibC(Kernel kernel) {
        var console = kernel.getConsole();
        LibC libc = new LibC(kernel.getSyscallDispatcher());
        
        console.println("\n========== LibC Pipe System Call Demo ==========\n");
        
        int[] fds = new int[2];
        console.println("[DEMO] Calling libc.pipe(fds)...");
        int ret = libc.pipe(fds);
        
        if (ret == 0) {
            console.println("[DEMO] pipe() succeeded");
            console.println("[DEMO] Note: Actual fd allocation not yet implemented");
            console.println("[DEMO] In full implementation:");
            console.println("[DEMO]   fds[0] would be read end (e.g., fd=3)");
            console.println("[DEMO]   fds[1] would be write end (e.g., fd=4)");
        } else {
            console.println("[DEMO] pipe() failed with error: " + ret);
        }
        
        console.println("\n========== LibC Pipe Demo Complete ==========\n");
    }
    
    /**
     * 显示管道的典型使用场景
     */
    public static void showUseCases(Kernel kernel) {
        var console = kernel.getConsole();
        
        console.println("\n========== Pipe Use Cases ==========\n");
        
        console.println("1. 父子进程通信:");
        console.println("   - 父进程创建 pipe()");
        console.println("   - fork() 创建子进程");
        console.println("   - 父进程关闭读端，子进程关闭写端");
        console.println("   - 父进程写数据，子进程读数据");
        
        console.println("\n2. Shell 命令管道:");
        console.println("   - 实现 'ls | grep txt' 这样的命令");
        console.println("   - ls 的输出连接到 grep 的输入");
        
        console.println("\n3. 生产者-消费者模式:");
        console.println("   - 一个进程产生数据写入管道");
        console.println("   - 另一个进程从管道读取并处理数据");
        
        console.println("\n========================================\n");
    }
}
