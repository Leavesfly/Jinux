package jinux.mm;

/**
 * 物理内存管理接口
 * 
 * 定义物理内存的页面分配/释放和字节级读写操作。
 * 遵循依赖倒置原则（DIP），上层模块应依赖此接口而非具体实现。
 *
 * @author Jinux Project
 */
public interface IPhysicalMemory {

    /**
     * 分配一个物理页面
     *
     * @return 页面号，如果失败返回 -1
     */
    int allocPage();

    /**
     * 释放一个物理页面
     *
     * @param pageNo 页面号
     */
    void freePage(int pageNo);

    /**
     * 增加页面引用计数（用于 COW）
     *
     * @param pageNo 页面号
     */
    void incrementPageRef(int pageNo);

    /**
     * 获取页面引用计数
     *
     * @param pageNo 页面号
     * @return 引用计数
     */
    int getPageRefCount(int pageNo);

    /**
     * 读取物理内存中的一个字节
     *
     * @param physicalAddress 物理地址
     * @return 字节值
     */
    byte readByte(long physicalAddress);

    /**
     * 写入物理内存中的一个字节
     *
     * @param physicalAddress 物理地址
     * @param value 字节值
     */
    void writeByte(long physicalAddress, byte value);

    /**
     * 批量读取物理内存
     *
     * @param physicalAddress 物理地址
     * @param buffer 目标缓冲区
     * @param offset 缓冲区偏移
     * @param length 读取长度
     */
    void readBytes(long physicalAddress, byte[] buffer, int offset, int length);

    /**
     * 批量写入物理内存
     *
     * @param physicalAddress 物理地址
     * @param buffer 源缓冲区
     * @param offset 缓冲区偏移
     * @param length 写入长度
     */
    void writeBytes(long physicalAddress, byte[] buffer, int offset, int length);

    /**
     * 获取空闲页面数
     *
     * @return 空闲页面数
     */
    int getFreePages();

    /**
     * 获取总页面数
     *
     * @return 总页面数
     */
    int getTotalPages();

    /**
     * 打印内存使用统计
     */
    void printStats();
}
