package jinux.mm;

/**
 * 地址空间接口
 * 
 * 定义进程虚拟地址空间的管理操作，包括内存映射、读写和 brk 扩展。
 * 遵循依赖倒置原则（DIP），使上层模块（如 syscall）不直接依赖具体实现。
 *
 * @author Jinux Project
 */
public interface IAddressSpace {

    /**
     * 分配并映射一个虚拟页
     *
     * @param virtualAddress 虚拟地址
     * @param flags 页面标志
     * @return 是否成功
     */
    boolean allocateAndMap(long virtualAddress, int flags);

    /**
     * 扩展堆（brk 系统调用）
     *
     * @param newBrk 新的堆结束地址
     * @return 实际的堆结束地址
     */
    long expandBrk(long newBrk);

    /**
     * 读取虚拟地址的字节
     *
     * @param virtualAddress 虚拟地址
     * @return 字节值
     */
    byte readByte(long virtualAddress);

    /**
     * 写入虚拟地址的字节
     *
     * @param virtualAddress 虚拟地址
     * @param value 字节值
     */
    void writeByte(long virtualAddress, byte value);

    /**
     * 批量读取多个字节
     *
     * @param virtualAddress 虚拟地址
     * @param buffer 目标缓冲区
     * @param offset 缓冲区偏移
     * @param length 读取长度
     */
    void readBytes(long virtualAddress, byte[] buffer, int offset, int length);

    /**
     * 批量写入多个字节
     *
     * @param virtualAddress 虚拟地址
     * @param buffer 源缓冲区
     * @param offset 缓冲区偏移
     * @param length 写入长度
     */
    void writeBytes(long virtualAddress, byte[] buffer, int offset, int length);

    /**
     * 复制地址空间（用于 fork，实现 COW）
     *
     * @return 新的地址空间副本
     */
    IAddressSpace copy();

    /**
     * 释放地址空间
     */
    void free();

    /**
     * 获取页表
     *
     * @return 页表
     */
    IPageTable getPageTable();

    // 段信息访问器

    long getCodeStart();
    void setCodeStart(long codeStart);

    long getCodeEnd();
    void setCodeEnd(long codeEnd);

    long getDataStart();
    void setDataStart(long dataStart);

    long getDataEnd();
    void setDataEnd(long dataEnd);

    long getBrk();

    long getStackTop();
}
