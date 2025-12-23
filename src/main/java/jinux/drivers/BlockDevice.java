package jinux.drivers;

import jinux.include.Const;

/**
 * 块设备
 * 对应 Linux 0.01 中的块设备（如硬盘、软盘）
 * 
 * @author Jinux Project
 */
public abstract class BlockDevice extends Device {
    
    /** 块大小 */
    protected final int blockSize;
    
    public BlockDevice(String name, int major, int minor) {
        super(name, major, minor);
        this.blockSize = Const.BLOCK_SIZE;
    }
    
    /**
     * 读取块
     * 
     * @param blockNo 块号
     * @param buf 缓冲区
     * @param offset 缓冲区偏移
     * @return 实际读取的字节数
     */
    public abstract int readBlock(int blockNo, byte[] buf, int offset);
    
    /**
     * 写入块
     * 
     * @param blockNo 块号
     * @param buf 缓冲区
     * @param offset 缓冲区偏移
     * @return 实际写入的字节数
     */
    public abstract int writeBlock(int blockNo, byte[] buf, int offset);
    
    /**
     * 同步（刷新缓冲）
     */
    public abstract void sync();
    
    /**
     * 获取块大小
     */
    public int getBlockSize() {
        return blockSize;
    }
}
