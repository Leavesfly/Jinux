package jinux.mm;

import jinux.include.MemoryConstants;

/**
 * 内存访问器
 * 
 * 提供对虚拟地址空间的读写操作。负责将虚拟地址翻译为物理地址，
 * 并通过物理内存进行实际的字节读写操作。
 * 
 * @author Jinux Project
 */
public class MemoryAccessor {
    
    /** 页表引用 */
    private final IPageTable pageTable;
    
    /** 物理内存引用 */
    private final IPhysicalMemory physicalMemory;
    
    /**
     * 构造内存访问器
     * 
     * @param pageTable 页表
     * @param physicalMemory 物理内存
     */
    public MemoryAccessor(IPageTable pageTable, IPhysicalMemory physicalMemory) {
        this.pageTable = pageTable;
        this.physicalMemory = physicalMemory;
    }
    
    /**
     * 读取虚拟地址的字节
     * 
     * @param vaddr 虚拟地址
     * @return 字节值
     * @throws AddressSpace.PageFaultException 如果页面不存在
     */
    public byte readByte(long vaddr) {
        long paddr = pageTable.translate(vaddr);
        if (paddr < 0) {
            throw new AddressSpace.PageFaultException("Page not present at vaddr: 0x" + Long.toHexString(vaddr));
        }
        
        return physicalMemory.readByte(paddr);
    }
    
    /**
     * 写入虚拟地址的字节
     * 
     * @param vaddr 虚拟地址
     * @param value 字节值
     * @throws AddressSpace.PageFaultException 如果页面不存在
     */
    public void writeByte(long vaddr, byte value) {
        long paddr = pageTable.translate(vaddr);
        if (paddr < 0) {
            throw new AddressSpace.PageFaultException("Page not present at vaddr: 0x" + Long.toHexString(vaddr));
        }
        
        physicalMemory.writeByte(paddr, value);
    }
    
    /**
     * 批量读取多个字节
     * 按页边界分段，每段直接调用物理内存的批量拷贝，避免逐字节操作
     * 
     * @param vaddr 虚拟地址
     * @param buf 目标缓冲区
     * @param offset 缓冲区偏移
     * @param len 读取长度
     * @throws AddressSpace.PageFaultException 如果页面不存在
     */
    public void readBytes(long vaddr, byte[] buf, int offset, int len) {
        int remaining = len;
        long currentVaddr = vaddr;
        int currentOffset = offset;
        
        while (remaining > 0) {
            // 计算当前页内可读的字节数
            int pageOffset = (int) (currentVaddr & (MemoryConstants.PAGE_SIZE - 1));
            int chunkSize = Math.min(remaining, MemoryConstants.PAGE_SIZE - pageOffset);
            
            long paddr = pageTable.translate(currentVaddr);
            if (paddr < 0) {
                throw new AddressSpace.PageFaultException("Page not present at vaddr: 0x" + Long.toHexString(currentVaddr));
            }
            
            physicalMemory.readBytes(paddr, buf, currentOffset, chunkSize);
            
            currentVaddr += chunkSize;
            currentOffset += chunkSize;
            remaining -= chunkSize;
        }
    }
    
    /**
     * 批量写入多个字节
     * 按页边界分段，直接调用物理内存的批量拷贝
     * 
     * @param vaddr 虚拟地址
     * @param buf 源缓冲区
     * @param offset 缓冲区偏移
     * @param len 写入长度
     * @throws AddressSpace.PageFaultException 如果页面不存在
     */
    public void writeBytes(long vaddr, byte[] buf, int offset, int len) {
        int remaining = len;
        long currentVaddr = vaddr;
        int currentOffset = offset;
        
        while (remaining > 0) {
            int pageOffset = (int) (currentVaddr & (MemoryConstants.PAGE_SIZE - 1));
            int chunkSize = Math.min(remaining, MemoryConstants.PAGE_SIZE - pageOffset);
            
            long paddr = pageTable.translate(currentVaddr);
            if (paddr < 0) {
                throw new AddressSpace.PageFaultException("Page not present at vaddr: 0x" + Long.toHexString(currentVaddr));
            }
            
            physicalMemory.writeBytes(paddr, buf, currentOffset, chunkSize);
            
            currentVaddr += chunkSize;
            currentOffset += chunkSize;
            remaining -= chunkSize;
        }
    }
}
