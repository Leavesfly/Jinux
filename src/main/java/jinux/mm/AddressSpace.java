package jinux.mm;

import jinux.include.Const;

/**
 * 地址空间
 * 对应 Linux 0.01 中每个进程的虚拟地址空间
 * 
 * 包含代码段、数据段、堆、栈等区域的管理
 * 
 * @author Jinux Project
 */
public class AddressSpace {
    
    /** 页表 */
    private final PageTable pageTable;
    
    /** 内存管理器引用 */
    private final MemoryManager memoryManager;
    
    /** 代码段起始地址 */
    private long codeStart;
    
    /** 代码段结束地址 */
    private long codeEnd;
    
    /** 数据段起始地址 */
    private long dataStart;
    
    /** 数据段结束地址（堆的起始） */
    private long dataEnd;
    
    /** 堆结束地址（brk） */
    private long brk;
    
    /** 栈顶地址 */
    private long stackTop;
    
    /**
     * 构造地址空间
     * 
     * @param memoryManager 内存管理器
     */
    public AddressSpace(MemoryManager memoryManager) {
        this.pageTable = new PageTable();
        this.memoryManager = memoryManager;
        
        // 初始化为典型的用户空间布局
        // 代码段从 0x00000000 开始
        this.codeStart = 0;
        this.codeEnd = 0;
        
        // 数据段紧跟代码段
        this.dataStart = 0;
        this.dataEnd = 0;
        this.brk = 0;
        
        // 栈从高地址向下增长（64MB - 4KB）
        this.stackTop = Const.TASK_SIZE - Const.PAGE_SIZE;
    }
    
    /**
     * 分配并映射一个虚拟页
     * 
     * @param vaddr 虚拟地址
     * @param flags 页面标志
     * @return 是否成功
     */
    public boolean allocateAndMap(long vaddr, int flags) {
        int vpage = (int) (vaddr >> Const.PAGE_SHIFT);
        
        // 检查是否已映射
        if (pageTable.isMapped(vpage)) {
            return true;
        }
        
        // 分配物理页
        int ppage = memoryManager.allocatePage();
        if (ppage < 0) {
            return false; // 内存不足
        }
        
        // 建立映射
        pageTable.map(vpage, ppage, flags);
        return true;
    }
    
    /**
     * 扩展堆（brk 系统调用）
     * 
     * @param newBrk 新的堆结束地址
     * @return 实际的堆结束地址
     */
    public long expandBrk(long newBrk) {
        if (newBrk < dataEnd) {
            return brk; // 不允许缩小到数据段以下
        }
        
        // 对齐到页边界
        long alignedBrk = (newBrk + Const.PAGE_SIZE - 1) & ~(Const.PAGE_SIZE - 1);
        long oldBrk = brk;
        
        // 分配新页面
        for (long addr = oldBrk; addr < alignedBrk; addr += Const.PAGE_SIZE) {
            if (!allocateAndMap(addr, PageTable.PAGE_PRESENT | PageTable.PAGE_RW | PageTable.PAGE_USER)) {
                return brk; // 分配失败
            }
        }
        
        brk = alignedBrk;
        return brk;
    }
    
    /**
     * 读取虚拟地址的字节
     * 
     * @param vaddr 虚拟地址
     * @return 字节值
     */
    public byte readByte(long vaddr) {
        long paddr = pageTable.translate(vaddr);
        if (paddr < 0) {
            throw new PageFaultException("Page not present at vaddr: 0x" + Long.toHexString(vaddr));
        }
        
        return memoryManager.getPhysicalMemory().readByte(paddr);
    }
    
    /**
     * 写入虚拟地址的字节
     * 
     * @param vaddr 虚拟地址
     * @param value 字节值
     */
    public void writeByte(long vaddr, byte value) {
        long paddr = pageTable.translate(vaddr);
        if (paddr < 0) {
            throw new PageFaultException("Page not present at vaddr: 0x" + Long.toHexString(vaddr));
        }
        
        memoryManager.getPhysicalMemory().writeByte(paddr, value);
    }
    
    /**
     * 读取多个字节
     * 
     * @param vaddr 虚拟地址
     * @param buf 目标缓冲区
     * @param offset 缓冲区偏移
     * @param len 读取长度
     */
    public void readBytes(long vaddr, byte[] buf, int offset, int len) {
        for (int i = 0; i < len; i++) {
            buf[offset + i] = readByte(vaddr + i);
        }
    }
    
    /**
     * 写入多个字节
     * 
     * @param vaddr 虚拟地址
     * @param buf 源缓冲区
     * @param offset 缓冲区偏移
     * @param len 写入长度
     */
    public void writeBytes(long vaddr, byte[] buf, int offset, int len) {
        for (int i = 0; i < len; i++) {
            writeByte(vaddr + i, buf[offset + i]);
        }
    }
    
    /**
     * 复制地址空间（用于 fork）
     * 
     * @return 新的地址空间副本
     */
    public AddressSpace copy() {
        AddressSpace newSpace = new AddressSpace(memoryManager);
        
        // 复制页表（写时复制优化可以在这里实现）
        // 简化实现：直接复制所有页面
        PageTable oldTable = this.pageTable;
        PageTable newTable = newSpace.pageTable;
        
        // 这里应该实现写时复制（COW），但为简化先直接复制
        // TODO: 实现 COW
        for (int vpage = 0; vpage < Const.TASK_SIZE >> Const.PAGE_SHIFT; vpage++) {
            if (oldTable.isMapped(vpage)) {
                int oldPpage = oldTable.getPhysicalPage(vpage);
                int newPpage = memoryManager.allocatePage();
                
                if (newPpage >= 0) {
                    // 复制页面内容
                    long srcAddr = ((long) oldPpage) << Const.PAGE_SHIFT;
                    long dstAddr = ((long) newPpage) << Const.PAGE_SHIFT;
                    
                    PhysicalMemory pm = memoryManager.getPhysicalMemory();
                    byte[] tempBuf = new byte[Const.PAGE_SIZE];
                    pm.readBytes(srcAddr, tempBuf, 0, Const.PAGE_SIZE);
                    pm.writeBytes(dstAddr, tempBuf, 0, Const.PAGE_SIZE);
                    
                    // 映射到新页表
                    newTable.map(vpage, newPpage, PageTable.PAGE_PRESENT | PageTable.PAGE_RW | PageTable.PAGE_USER);
                }
            }
        }
        
        // 复制段信息
        newSpace.codeStart = this.codeStart;
        newSpace.codeEnd = this.codeEnd;
        newSpace.dataStart = this.dataStart;
        newSpace.dataEnd = this.dataEnd;
        newSpace.brk = this.brk;
        newSpace.stackTop = this.stackTop;
        
        return newSpace;
    }
    
    /**
     * 释放地址空间
     */
    public void free() {
        // 释放所有映射的物理页
        for (int vpage = 0; vpage < Const.TASK_SIZE >> Const.PAGE_SHIFT; vpage++) {
            if (pageTable.isMapped(vpage)) {
                int ppage = pageTable.getPhysicalPage(vpage);
                memoryManager.freePage(ppage);
            }
        }
        
        pageTable.clear();
    }
    
    // Getters and setters
    
    public PageTable getPageTable() {
        return pageTable;
    }
    
    public long getCodeStart() {
        return codeStart;
    }
    
    public void setCodeStart(long codeStart) {
        this.codeStart = codeStart;
    }
    
    public long getCodeEnd() {
        return codeEnd;
    }
    
    public void setCodeEnd(long codeEnd) {
        this.codeEnd = codeEnd;
    }
    
    public long getDataStart() {
        return dataStart;
    }
    
    public void setDataStart(long dataStart) {
        this.dataStart = dataStart;
    }
    
    public long getDataEnd() {
        return dataEnd;
    }
    
    public void setDataEnd(long dataEnd) {
        this.dataEnd = dataEnd;
        this.brk = dataEnd;
    }
    
    public long getBrk() {
        return brk;
    }
    
    public long getStackTop() {
        return stackTop;
    }
    
    /**
     * 页面错误异常
     */
    public static class PageFaultException extends RuntimeException {
        public PageFaultException(String message) {
            super(message);
        }
    }
}
