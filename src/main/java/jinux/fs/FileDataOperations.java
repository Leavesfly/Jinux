package jinux.fs;

/**
 * 文件数据读写操作接口
 * 负责文件内容的读取和写入
 */
public interface FileDataOperations {
    /**
     * 从文件中读取数据
     * 
     * @param inode 文件 Inode
     * @param position 读取起始位置
     * @param buf 目标缓冲区
     * @param offset 目标缓冲区偏移量
     * @param count 要读取的字节数
     * @return 实际读取的字节数，如果读取失败则返回负数
     */
    int readFileData(Inode inode, long position, byte[] buf, int offset, int count);

    /**
     * 向文件中写入数据
     * 
     * @param inode 文件 Inode
     * @param position 写入起始位置
     * @param buf 源缓冲区
     * @param offset 源缓冲区偏移量
     * @param count 要写入的字节数
     * @return 实际写入的字节数，如果写入失败则返回负数
     */
    int writeFileData(Inode inode, long position, byte[] buf, int offset, int count);
}
