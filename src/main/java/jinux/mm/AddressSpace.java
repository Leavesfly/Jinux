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
        int vpage = (int) (vaddr >> Const.PAGE_SHIFT);
        
        // 检查页面权限
        Integer flags = pageTable.getFlags(vpage);
        if (flags == null) {
            throw new PageFaultException("Page not present at vaddr: 0x" + Long.toHexString(vaddr));
        }
        
        // 检查是否为COW页面
        if ((flags & PageTable.PAGE_COW) != 0) {
            // 处理写时复制
            int newPpage = handleCopyOnWrite(vpage);
            if (newPpage < 0) {
                throw new PageFaultException("Copy-on-write failed at vaddr: 0x" + Long.toHexString(vaddr));
            }
        } else if ((flags & PageTable.PAGE_RW) == 0) {
            // 页面是只读的
            throw new PageFaultException("Page is read-only at vaddr: 0x" + Long.toHexString(vaddr));
        }
        
        // 重新翻译地址（可能已经改变）
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
     * 实现写时复制（Copy-On-Write）优化
     * 
     * @return 新的地址空间副本
     */
    public AddressSpace copy() {
        AddressSpace newSpace = new AddressSpace(memoryManager);
        
        // 复制页表（使用写时复制优化）
        PageTable oldTable = this.pageTable;
        PageTable newTable = newSpace.pageTable;
        PhysicalMemory pm = memoryManager.getPhysicalMemory();
        
        for (int vpage = 0; vpage < Const.TASK_SIZE >> Const.PAGE_SHIFT; vpage++) {
            if (oldTable.isMapped(vpage)) {
                int oldPpage = oldTable.getPhysicalPage(vpage);
                Integer oldFlags = oldTable.getFlags(vpage);
                
                if (oldPpage >= 0 && oldFlags != null) {
                    // 增加物理页面的引用计数
                    pm.incrementPageRef(oldPpage);
                    
                    // 将页面标记为只读和COW
                    // 新页表和旧页表都指向同一个物理页面，但标记为只读
                    int newFlags = (oldFlags & ~PageTable.PAGE_RW) | PageTable.PAGE_COW;
                    newTable.map(vpage, oldPpage, newFlags);
                    
                    // 同时将旧页表的页面也标记为COW和只读
                    oldTable.setFlags(vpage, newFlags);
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
     * 处理写时复制：当写入COW页面时，复制页面
     * 
     * 这是 COW 机制的核心：当进程尝试写入共享页面时，
     * 如果页面被多个进程共享（引用计数 > 1），则复制页面。
     * 如果只有当前进程使用（引用计数 = 1），则直接修改。
     * 
     * @param vpage 虚拟页号
     * @return 新的物理页号，如果失败返回-1
     */
    private int handleCopyOnWrite(int vpage) {
        Integer oldFlags = pageTable.getFlags(vpage);
        if (oldFlags == null || (oldFlags & PageTable.PAGE_COW) == 0) {
            return -1; // 不是COW页面
        }
        
        int oldPpage = pageTable.getPhysicalPage(vpage);
        if (oldPpage < 0) {
            return -1;
        }
        
        PhysicalMemory pm = memoryManager.getPhysicalMemory();
        
        // 如果引用计数为1，可以直接修改，不需要复制
        if (pm.getPageRefCount(oldPpage) == 1) {
            // 清除COW标记，恢复写权限
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
        
        // 更新页表映射，清除COW标记，恢复写权限
        int newFlags = (oldFlags & ~PageTable.PAGE_COW) | PageTable.PAGE_RW;
        pageTable.map(vpage, newPpage, newFlags);
        
        return newPpage;
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
