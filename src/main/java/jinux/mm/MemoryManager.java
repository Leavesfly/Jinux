package jinux.mm;

import jinux.include.Const;

/**
 * 内存管理器
 * 对应 Linux 0.01 中的 mm/memory.c
 * 
 * 协调物理内存分配、虚拟地址空间管理等
 * 
 * @author Jinux Project
 */
public class MemoryManager {
    
    /** 物理内存管理器 */
    private final PhysicalMemory physicalMemory;
    
    /**
     * 构造内存管理器
     */
    public MemoryManager() {
        this.physicalMemory = new PhysicalMemory();
    }
    
    /**
     * 分配一个物理页面
     * 
     * @return 页面号，失败返回 -1
     */
    public int allocatePage() {
        return physicalMemory.allocPage();
    }
    
    /**
     * 释放一个物理页面
     * 
     * @param pageNo 页面号
     */
    public void freePage(int pageNo) {
        physicalMemory.freePage(pageNo);
    }
    
    /**
     * 创建新的地址空间
     * 
     * @return 新的地址空间
     */
    public AddressSpace createAddressSpace() {
        return new AddressSpace(this);
    }
    
    /**
     * 获取物理内存管理器
     */
    public PhysicalMemory getPhysicalMemory() {
        return physicalMemory;
    }
    
    /**
     * 打印内存统计信息
     */
    public void printStats() {
        physicalMemory.printStats();
    }
}
