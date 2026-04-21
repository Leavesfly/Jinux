package jinux.fs;

/**
 * 字节操作工具类
 * 提供小端序读写 short 和 int 的工具方法
 */
public final class ByteUtils {
    
    private ByteUtils() {
        // 防止实例化
    }
    
    /**
     * 从字节数组中读取小端序 short（2字节）
     * 
     * @param data 字节数组
     * @param offset 偏移量
     * @return short 值
     */
    public static int readLittleEndianShort(byte[] data, int offset) {
        return (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
    }
    
    /**
     * 将 short 值以小端序写入字节数组
     * 
     * @param data 字节数组
     * @param offset 偏移量
     * @param value 要写入的值
     */
    public static void writeLittleEndianShort(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }
    
    /**
     * 将 int 值以小端序写入字节数组（4字节）
     * 
     * @param data 字节数组
     * @param offset 偏移量
     * @param value 要写入的值
     */
    public static void writeLittleEndianInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value & 0xFF);
        data[offset + 1] = (byte) ((value >> 8) & 0xFF);
        data[offset + 2] = (byte) ((value >> 16) & 0xFF);
        data[offset + 3] = (byte) ((value >> 24) & 0xFF);
    }
}
