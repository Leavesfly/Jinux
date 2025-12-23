package jinux.mm;

import jinux.include.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PhysicalMemory类的单元测试
 */
public class PhysicalMemoryTest {
    
    private PhysicalMemory physicalMemory;
    
    @BeforeEach
    void setUp() {
        physicalMemory = new PhysicalMemory();
    }
    
    @Test
    void testPhysicalMemoryCreation() {
        assertNotNull(physicalMemory);
        assertTrue(physicalMemory.getTotalPages() > 0);
        assertTrue(physicalMemory.getFreePages() > 0);
    }
    
    @Test
    void testAllocPage() {
        int initialFree = physicalMemory.getFreePages();
        int pageNo = physicalMemory.allocPage();
        
        assertTrue(pageNo >= 0);
        assertEquals(initialFree - 1, physicalMemory.getFreePages());
    }
    
    @Test
    void testFreePage() {
        int pageNo = physicalMemory.allocPage();
        int freeBefore = physicalMemory.getFreePages();
        
        physicalMemory.freePage(pageNo);
        
        assertEquals(freeBefore + 1, physicalMemory.getFreePages());
    }
    
    @Test
    void testFreeInvalidPage() {
        // 释放无效的页面号不应该崩溃
        physicalMemory.freePage(-1);
        physicalMemory.freePage(999999);
    }
    
    @Test
    void testReadWriteByte() {
        int pageNo = physicalMemory.allocPage();
        long paddr = ((long) pageNo) << Const.PAGE_SHIFT;
        
        byte testValue = (byte) 0xAB;
        physicalMemory.writeByte(paddr, testValue);
        
        byte readValue = physicalMemory.readByte(paddr);
        assertEquals(testValue, readValue);
    }
    
    @Test
    void testReadWriteBytes() {
        int pageNo = physicalMemory.allocPage();
        long paddr = ((long) pageNo) << Const.PAGE_SHIFT;
        
        byte[] writeBuf = {0x01, 0x02, 0x03, 0x04, 0x05};
        physicalMemory.writeBytes(paddr, writeBuf, 0, writeBuf.length);
        
        byte[] readBuf = new byte[writeBuf.length];
        physicalMemory.readBytes(paddr, readBuf, 0, readBuf.length);
        
        assertArrayEquals(writeBuf, readBuf);
    }
    
    @Test
    void testReadWriteByteOutOfBounds() {
        long invalidAddr = Const.MEMORY_SIZE + 100;
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            physicalMemory.readByte(invalidAddr);
        });
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            physicalMemory.writeByte(invalidAddr, (byte) 0x01);
        });
    }
    
    @Test
    void testPageAllocationClearsMemory() {
        int pageNo = physicalMemory.allocPage();
        long paddr = ((long) pageNo) << Const.PAGE_SHIFT;
        
        // 写入一些数据
        physicalMemory.writeByte(paddr, (byte) 0xFF);
        
        // 释放并重新分配
        physicalMemory.freePage(pageNo);
        int newPageNo = physicalMemory.allocPage();
        long newPaddr = ((long) newPageNo) << Const.PAGE_SHIFT;
        
        // 新分配的页面应该是清零的
        // 注意：如果分配算法重用页面，这里可能失败
        // 但至少新分配的页面应该是清零的
        byte value = physicalMemory.readByte(newPaddr);
        assertEquals(0, value);
    }
    
    @Test
    void testMultipleAllocations() {
        int[] pages = new int[10];
        int initialFree = physicalMemory.getFreePages();
        
        for (int i = 0; i < 10; i++) {
            pages[i] = physicalMemory.allocPage();
            assertTrue(pages[i] >= 0);
        }
        
        assertEquals(initialFree - 10, physicalMemory.getFreePages());
        
        // 释放所有页面
        for (int page : pages) {
            physicalMemory.freePage(page);
        }
        
        assertEquals(initialFree, physicalMemory.getFreePages());
    }
}

