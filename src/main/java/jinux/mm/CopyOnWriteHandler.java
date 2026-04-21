package jinux.mm;

import jinux.include.Const;

/**
 * 写时复制（Copy-On-Write）处理器
 * 
 * 专门负责处理 COW 页面的复制逻辑。当进程尝试写入共享页面时，
 * 如果页面被多个进程共享（引用计数 > 1），则复制页面；
 * 如果只有当前进程使用（引用计数 = 1），则直接修改。
 * 
 * @author Jinux Project
 */
public class CopyOnWriteHandler {
    
    /** 内存管理器引用 */
    private final MemoryManager memoryManager;
    
    /** 页表引用 */
    private final PageTable pageTable;
    
    /**
     * 构造 COW 处理器
     * 
     * @param memoryManager 内存管理器
     * @param pageTable 页表
     */
    public CopyOnWriteHandler(MemoryManager memoryManager, PageTable pageTable) {
        this.memoryManager = memoryManager;
        this.pageTable = pageTable;
    }
    
    /**
     * 处理写时复制：当写入 COW 页面时，复制页面
     * 
     * 这是 COW 机制的核心：当进程尝试写入共享页面时，
     * 如果页面被多个进程共享（引用计数 > 1），则复制页面。
     * 如果只有当前进程使用（引用计数 = 1），则直接修改。
     * 
     * @param vpage 虚拟页号
     * @return 新的物理页号，如果失败返回 -1
     */
    public int handleCopyOnWrite(int vpage) {
        Integer oldFlags = pageTable.getFlags(vpage);
        if (oldFlags == null || (oldFlags & PageTable.PAGE_COW) == 0) {
            return -1; // 不是 COW 页面
        }
        
        int oldPpage = pageTable.getPhysicalPage(vpage);
        if (oldPpage < 0) {
            return -1;
        }
        
        PhysicalMemory pm = memoryManager.getPhysicalMemory();
        
        // 如果引用计数为 1，可以直接修改，不需要复制
        if (pm.getPageRefCount(oldPpage) == 1) {
            // 清除 COW 标记，恢复写权限
            int newFlags = (oldFlags & ~PageTable.PAGE_COW) | PageTable.PAGE_RW;
            pageTable.setFlags(vpage, newFlags);
            return oldPpage;
        }
        
        // 需要复制页面
        int newPpage = memoryManager.allocatePage();
        if (newPpage < 0) {
            return -1; // 内存不足
        }
        
        // 复制页面内容
        long srcAddr = ((long) oldPpage) << Const.PAGE_SHIFT;
        long dstAddr = ((long) newPpage) << Const.PAGE_SHIFT;
        
        byte[] tempBuf = new byte[Const.PAGE_SIZE];
        pm.readBytes(srcAddr, tempBuf, 0, Const.PAGE_SIZE);
        pm.writeBytes(dstAddr, tempBuf, 0, Const.PAGE_SIZE);
        
        // 减少旧页面的引用计数
        memoryManager.freePage(oldPpage);
        
        // 更新页表映射，清除 COW 标记，恢复写权限
        int newFlags = (oldFlags & ~PageTable.PAGE_COW) | PageTable.PAGE_RW;
        pageTable.map(vpage, newPpage, newFlags);
        
        return newPpage;
    }
}
