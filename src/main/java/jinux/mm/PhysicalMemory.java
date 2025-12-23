package jinux.mm;

import jinux.include.Const;

/**
 * 物理内存管理器
 * 对应 Linux 0.01 中的 mm/memory.c 部分功能
 * 
 * 管理物理页面的分配和释放，采用简单的位图管理方式
 * 
 * @author Jinux Project
 */
public class PhysicalMemory {
    
    /** 物理内存数据（模拟） */
    private final byte[] memory;
    
    /** 页面使用位图：0 表示空闲，1 表示已使用 */
    private final byte[] pageMap;
    
    /** 页面引用计数（用于COW） */
    private final int[] pageRefCount;
    
    /** 总页面数 */
    private final int totalPages;
    
    /** 空闲页面数 */
    private int freePages;
    
    /**
     * 构造物理内存管理器
     */
    public PhysicalMemory() {
        this.memory = new byte[Const.MEMORY_SIZE];
        this.totalPages = Const.NR_PAGES;
        this.pageMap = new byte[totalPages];
        this.pageRefCount = new int[totalPages];
        
        // 初始化：低端 1MB（内核占用）标记为已使用
        int kernelPages = Const.KERNEL_MEMORY / Const.PAGE_SIZE;
        for (int i = 0; i < kernelPages; i++) {
            pageMap[i] = 1;
            pageRefCount[i] = 1; // 内核页面引用计数为1
        }
        
        this.freePages = totalPages - kernelPages;
        
        System.out.println("[MM] Physical memory initialized: " + 
            (Const.MEMORY_SIZE / 1024 / 1024) + "MB, " +
            freePages + " pages free");
    }
    
    /**
     * 分配一个物理页面
     * 
     * @return 页面号，如果失败返回 -1
     */
    public synchronized int allocPage() {
        // 简单的线性查找空闲页
        for (int i = 0; i < totalPages; i++) {
            if (pageMap[i] == 0) {
                pageMap[i] = 1;
                pageRefCount[i] = 1; // 新分配的页面引用计数为1
                freePages--;
                
                // 清零页面内容
                int offset = i * Const.PAGE_SIZE;
                for (int j = 0; j < Const.PAGE_SIZE; j++) {
                    memory[offset + j] = 0;
                }
                
                return i;
            }
        }
        
        System.err.println("[MM] ERROR: Out of memory! No free pages.");
        return -1;
    }
    
    /**
     * 释放一个物理页面
     * 
     * @param pageNo 页面号
     */
    public synchronized void freePage(int pageNo) {
        if (pageNo < 0 || pageNo >= totalPages) {
            System.err.println("[MM] ERROR: Invalid page number: " + pageNo);
            return;
        }
        
        if (pageMap[pageNo] == 0) {
            System.err.println("[MM] WARNING: Freeing already free page: " + pageNo);
            return;
        }
        
        // 减少引用计数
        pageRefCount[pageNo]--;
        
        // 只有当引用计数为0时才真正释放
        if (pageRefCount[pageNo] <= 0) {
            pageMap[pageNo] = 0;
            pageRefCount[pageNo] = 0;
            freePages++;
        }
    }
    
    /**
     * 增加页面引用计数（用于COW）
     * 
     * @param pageNo 页面号
     */
    public synchronized void incrementPageRef(int pageNo) {
        if (pageNo >= 0 && pageNo < totalPages && pageMap[pageNo] != 0) {
            pageRefCount[pageNo]++;
        }
    }
    
    /**
     * 获取页面引用计数
     * 
     * @param pageNo 页面号
     * @return 引用计数
     */
    public synchronized int getPageRefCount(int pageNo) {
        if (pageNo >= 0 && pageNo < totalPages) {
            return pageRefCount[pageNo];
        }
        return 0;
    }
    
    /**
     * 读取物理内存
     * 
     * @param paddr 物理地址
     * @return 字节值
     */
    public byte readByte(long paddr) {
        if (paddr < 0 || paddr >= memory.length) {
            throw new IndexOutOfBoundsException("Physical address out of range: 0x" + 
                Long.toHexString(paddr));
        }
        return memory[(int) paddr];
    }
    
    /**
     * 写入物理内存
     * 
     * @param paddr 物理地址
     * @param value 字节值
     */
    public void writeByte(long paddr, byte value) {
        if (paddr < 0 || paddr >= memory.length) {
            throw new IndexOutOfBoundsException("Physical address out of range: 0x" + 
                Long.toHexString(paddr));
        }
        memory[(int) paddr] = value;
    }
    
    /**
     * 读取多个字节
     * 
     * @param paddr 物理地址
     * @param buf 目标缓冲区
     * @param offset 缓冲区偏移
     * @param len 读取长度
     */
    public void readBytes(long paddr, byte[] buf, int offset, int len) {
        if (paddr < 0 || paddr + len > memory.length) {
            throw new IndexOutOfBoundsException("Physical address range out of bounds");
        }
        System.arraycopy(memory, (int) paddr, buf, offset, len);
    }
    
    /**
     * 写入多个字节
     * 
     * @param paddr 物理地址
     * @param buf 源缓冲区
     * @param offset 缓冲区偏移
     * @param len 写入长度
     */
    public void writeBytes(long paddr, byte[] buf, int offset, int len) {
        if (paddr < 0 || paddr + len > memory.length) {
            throw new IndexOutOfBoundsException("Physical address range out of bounds");
        }
        System.arraycopy(buf, offset, memory, (int) paddr, len);
    }
    
    /**
     * 获取空闲页面数
     */
    public int getFreePages() {
        return freePages;
    }
    
    /**
     * 获取总页面数
     */
    public int getTotalPages() {
        return totalPages;
    }
    
    /**
     * 打印内存使用统计
     */
    public void printStats() {
        int usedPages = totalPages - freePages;
        int usedMB = (usedPages * Const.PAGE_SIZE) / 1024 / 1024;
        int freeMB = (freePages * Const.PAGE_SIZE) / 1024 / 1024;
        
        System.out.println("[MM] Memory: " + usedPages + "/" + totalPages + " pages used, " +
            usedMB + "MB/" + freeMB + "MB free");
    }
}
