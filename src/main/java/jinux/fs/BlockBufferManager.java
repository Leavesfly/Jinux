package jinux.fs;

/**
 * 块缓冲区管理器接口
 * 负责块设备缓冲区的获取、释放和同步
 */
public interface BlockBufferManager {
    /**
     * 获取指定设备和块号的缓冲区
     * 
     * @param dev 设备号
     * @param blockNo 块号
     * @return 获取到的 BufferCache
     */
    BufferCache getBuffer(int dev, int blockNo);

    /**
     * 释放缓冲区引用
     * 当引用计数降为 0 且缓冲区为脏时，会将数据写回磁盘
     * 
     * @param buffer 要释放的 BufferCache
     */
    void releaseBuffer(BufferCache buffer);

    /**
     * 同步所有脏缓冲区到磁盘
     */
    void sync();
}
