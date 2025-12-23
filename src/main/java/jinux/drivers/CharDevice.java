package jinux.drivers;

/**
 * 字符设备
 * 对应 Linux 0.01 中的字符设备（如控制台、串口）
 * 
 * @author Jinux Project
 */
public abstract class CharDevice extends Device {
    
    public CharDevice(String name, int major, int minor) {
        super(name, major, minor);
    }
    
    /**
     * 读取字符
     * 
     * @param buf 缓冲区
     * @param offset 偏移
     * @param len 长度
     * @return 实际读取的字节数
     */
    public abstract int read(byte[] buf, int offset, int len);
    
    /**
     * 写入字符
     * 
     * @param buf 缓冲区
     * @param offset 偏移
     * @param len 长度
     * @return 实际写入的字节数
     */
    public abstract int write(byte[] buf, int offset, int len);
}
