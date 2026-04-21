package jinux.mm;

/**
 * 内存管理器接口
 * 
 * 作为内存子系统的 Facade，协调物理内存分配和地址空间管理。
 * 遵循依赖倒置原则（DIP），上层模块应依赖此接口。
 *
 * @author Jinux Project
 */
public interface IMemoryManager {

    /**
     * 分配一个物理页面
     *
     * @return 页面号，失败返回 -1
     */
    int allocatePage();

    /**
     * 释放一个物理页面
     *
     * @param pageNo 页面号
     */
    void freePage(int pageNo);

    /**
     * 创建新的地址空间
     *
     * @return 新的地址空间
     */
    IAddressSpace createAddressSpace();

    /**
     * 获取物理内存管理器
     *
     * @return 物理内存管理器
     */
    IPhysicalMemory getPhysicalMemory();

    /**
     * 打印内存统计信息
     */
    void printStats();
}
