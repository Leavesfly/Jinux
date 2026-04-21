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
    
    /** COW 处理器 */
    private final CopyOnWriteHandler cowHandler;
    
    /** 内存访问器 */
    private final MemoryAccessor memoryAccessor;
    
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
    
    /** 用户空间栈顶地址常量 */
    private static final long USER_STACK_TOP = Const.TASK_SIZE - Const.PAGE_SIZE;
    
    /** 默认页面标志：存在、可读写、用户态可访问 */
    private static final int DEFAULT_PAGE_FLAGS = PageTable.PAGE_PRESENT | PageTable.PAGE_RW | PageTable.PAGE_USER;
    
    /**
     * 构造地址空间
     * 
     * @param memoryManager 内存管理器
     */
    public AddressSpace(MemoryManager memoryManager) {
        this.pageTable = new PageTable();
        this.memoryManager = memoryManager;
        this.cowHandler = new CopyOnWriteHandler(memoryManager, pageTable);
        this.memoryAccessor = new MemoryAccessor(pageTable, memoryManager.getPhysicalMemory());
        
        // 初始化为典型的用户空间布局
        // 代码段从 0x00000000 开始
        this.codeStart = 0;
        this.codeEnd = 0;
        
        // 数据段紧跟代码段
        this.dataStart = 0;
        this.dataEnd = 0;
        this.brk = 0;
        
        // 栈从高地址向下增长（64MB - 4KB）
        this.stackTop = USER_STACK_TOP;
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
        
        // 分配新页面，失败时回滚已分配的页面
        for (long addr = oldBrk; addr < alignedBrk; addr += Const.PAGE_SIZE) {
            if (!allocateAndMap(addr, DEFAULT_PAGE_FLAGS)) {
                // 回滚：释放本次已分配的所有页面
                for (long rollbackAddr = oldBrk; rollbackAddr < addr; rollbackAddr += Const.PAGE_SIZE) {
                    int vpage = (int) (rollbackAddr >> Const.PAGE_SHIFT);
                    int ppage = pageTable.getPhysicalPage(vpage);
                    if (ppage >= 0) {
                        pageTable.unmap(vpage);
                        memoryManager.freePage(ppage);
                    }
                }
                return brk; // 分配失败，返回原始 brk
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
        return memoryAccessor.readByte(vaddr);
    }
    
    /**
     * 写入虚拟地址的字节
     * 使用 synchronized 保护 COW 处理，防止竞态条件
     * 
     * @param vaddr 虚拟地址
     * @param value 字节值
     */
    public synchronized void writeByte(long vaddr, byte value) {
        int vpage = (int) (vaddr >> Const.PAGE_SHIFT);
        
        // 检查页面权限
        Integer flags = pageTable.getFlags(vpage);
        if (flags == null) {
            throw new PageFaultException("Page not present at vaddr: 0x" + Long.toHexString(vaddr));
        }
        
        // 检查是否为COW页面（在同一把锁内完成检查和处理，避免竞态）
        if ((flags & PageTable.PAGE_COW) != 0) {
            int newPpage = cowHandler.handleCopyOnWrite(vpage);
            if (newPpage < 0) {
                throw new PageFaultException("Copy-on-write failed at vaddr: 0x" + Long.toHexString(vaddr));
            }
        } else if ((flags & PageTable.PAGE_RW) == 0) {
            throw new PageFaultException("Page is read-only at vaddr: 0x" + Long.toHexString(vaddr));
        }
        
        // 委托给 MemoryAccessor 进行实际写入
        memoryAccessor.writeByte(vaddr, value);
    }
    
    /**
     * 批量读取多个字节
     * 按页边界分段，每段直接调用物理内存的批量拷贝，避免逐字节操作
     * 
     * @param vaddr 虚拟地址
     * @param buf 目标缓冲区
     * @param offset 缓冲区偏移
     * @param len 读取长度
     */
    public void readBytes(long vaddr, byte[] buf, int offset, int len) {
        memoryAccessor.readBytes(vaddr, buf, offset, len);
    }
    
    /**
     * 批量写入多个字节
     * 按页边界分段，处理 COW 后直接调用物理内存的批量拷贝
     * 
     * @param vaddr 虚拟地址
     * @param buf 源缓冲区
     * @param offset 缓冲区偏移
     * @param len 写入长度
     */
    public synchronized void writeBytes(long vaddr, byte[] buf, int offset, int len) {
        int remaining = len;
        long currentVaddr = vaddr;
        int currentOffset = offset;
        
        while (remaining > 0) {
            int vpage = (int) (currentVaddr >> Const.PAGE_SHIFT);
            int pageOffset = (int) (currentVaddr & (Const.PAGE_SIZE - 1));
            int chunkSize = Math.min(remaining, Const.PAGE_SIZE - pageOffset);
            
            // 检查页面权限并处理 COW
            Integer pageFlags = pageTable.getFlags(vpage);
            if (pageFlags == null) {
                throw new PageFaultException("Page not present at vaddr: 0x" + Long.toHexString(currentVaddr));
            }
            if ((pageFlags & PageTable.PAGE_COW) != 0) {
                int newPpage = cowHandler.handleCopyOnWrite(vpage);
                if (newPpage < 0) {
                    throw new PageFaultException("Copy-on-write failed at vaddr: 0x" + Long.toHexString(currentVaddr));
                }
            } else if ((pageFlags & PageTable.PAGE_RW) == 0) {
                throw new PageFaultException("Page is read-only at vaddr: 0x" + Long.toHexString(currentVaddr));
            }
            
            // 委托给 MemoryAccessor 进行实际写入
            memoryAccessor.writeBytes(currentVaddr, buf, currentOffset, chunkSize);
            
            currentVaddr += chunkSize;
            currentOffset += chunkSize;
            remaining -= chunkSize;
        }
    }
    
    /**
     * 复制地址空间（用于 fork）
     * 实现写时复制（Copy-On-Write）优化
     * 
     * @return 新的地址空间副本
     */
    public synchronized AddressSpace copy() {
        AddressSpace newSpace = new AddressSpace(memoryManager);
        
        // 复制页表（使用写时复制优化）
        // synchronized 保护整个 copy 过程，防止 copy 期间页表被并发修改
        PageTable oldTable = this.pageTable;
        PageTable newTable = newSpace.pageTable;
        PhysicalMemory pm = memoryManager.getPhysicalMemory();
        
        for (int vpage = 0; vpage < Const.TASK_SIZE >> Const.PAGE_SHIFT; vpage++) {
            if (oldTable.isMapped(vpage)) {
                int oldPpage = oldTable.getPhysicalPage(vpage);
                Integer oldFlags = oldTable.getFlags(vpage);
                
                if (oldPpage >= 0 && oldFlags != null) {
                    pm.incrementPageRef(oldPpage);
                    
                    int newFlags = (oldFlags & ~PageTable.PAGE_RW) | PageTable.PAGE_COW;
                    newTable.map(vpage, oldPpage, newFlags);
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