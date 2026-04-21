package jinux.drivers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * 控制台设备
 * 对应 Linux 0.01 中的 tty (drivers/chr_drv/console.c)
 * 
 * 使用 Java 标准输入输出模拟控制台
 * 
 * @author Jinux Project
 */
public class ConsoleDevice extends CharDevice {
    
    /** 标准输入 */
    private final BufferedReader input;
    
    /** 标准输出 */
    private final PrintStream output;
    
    /** 输入缓冲 */
    private final StringBuilder inputBuffer;
    
    /** 输入缓冲区最大容量（64KB），防止恶意输入导致 OOM */
    private static final int MAX_INPUT_BUFFER_SIZE = 64 * 1024;
    
    /** 输入缓冲锁（保护 inputBuffer 的并发读写） */
    private final Object inputLock = new Object();
    
    /** 输出锁（保证输出顺序不交错） */
    private final Object outputLock = new Object();
    
    /**
     * 构造控制台设备
     */
    public ConsoleDevice() {
        super("console", 4, 0); // 主设备号 4，次设备号 0
        this.input = new BufferedReader(new InputStreamReader(System.in));
        this.output = System.out;
        this.inputBuffer = new StringBuilder();
    }
    
    @Override
    public void init() {
        System.out.println("[CONSOLE] Console device initialized: " + this);
    }
    
    @Override
    public int read(byte[] buf, int offset, int len) {
        synchronized (inputLock) {
            try {
                // 如果缓冲区为空，读取一行
                if (inputBuffer.length() == 0) {
                    String line = input.readLine();
                    if (line == null) {
                        return 0; // EOF
                    }
                    // 限制缓冲区大小，防止 OOM
                    if (inputBuffer.length() + line.length() + 1 > MAX_INPUT_BUFFER_SIZE) {
                        System.err.println("[CONSOLE] Input buffer overflow, truncating");
                        inputBuffer.setLength(0);
                    }
                    inputBuffer.append(line).append('\n');
                }
                
                // 从缓冲区复制数据
                int count = Math.min(len, inputBuffer.length());
                for (int i = 0; i < count; i++) {
                    buf[offset + i] = (byte) inputBuffer.charAt(i);
                }
                
                // 删除已读取的数据
                inputBuffer.delete(0, count);
                
                return count;
                
            } catch (Exception e) {
                System.err.println("[CONSOLE] Read error: " + e.getMessage());
                return -1;
            }
        }
    }
    
    @Override
    public int write(byte[] buf, int offset, int len) {
        synchronized (outputLock) {
            try {
                String text = new String(buf, offset, len);
                output.print(text);
                output.flush();
                return len;
                
            } catch (Exception e) {
                System.err.println("[CONSOLE] Write error: " + e.getMessage());
                return -1;
            }
        }
    }
    
    /**
     * 写入字符串到控制台（线程安全）
     * 
     * @param str 字符串
     */
    public void print(String str) {
        synchronized (outputLock) {
            output.print(str);
            output.flush();
        }
    }
    
    /**
     * 写入字符串并换行（线程安全）
     * 
     * @param str 字符串
     */
    public void println(String str) {
        synchronized (outputLock) {
            output.println(str);
            output.flush();
        }
    }
}
