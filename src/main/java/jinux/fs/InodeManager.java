package jinux.fs;

/**
 * Inode 管理器接口
 * 负责 Inode 的获取、释放和持久化
 */
public interface InodeManager {
    /**
     * 获取指定设备和 inode 号的 Inode
     * 
     * @param dev 设备号
     * @param ino inode 号
     * @return 获取到的 Inode
     */
    Inode getInode(int dev, int ino);

    /**
     * 释放 Inode 引用
     * 当引用计数降为 0 时，会将脏 Inode 写回磁盘并从缓存中移除
     * 
     * @param inode 要释放的 Inode
     */
    void putInode(Inode inode);
}
