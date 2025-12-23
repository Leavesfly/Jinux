package jinux.mm;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoryManager类的单元测试
 */
public class MemoryManagerTest {
    
    private MemoryManager memoryManager;
    
    @BeforeEach
    void setUp() {
        memoryManager = new MemoryManager();
    }
    
    @Test
    void testMemoryManagerCreation() {
        assertNotNull(memoryManager);
        assertNotNull(memoryManager.getPhysicalMemory());
    }
    
    @Test
    void testAllocatePage() {
        int pageNo = memoryManager.allocatePage();
        assertTrue(pageNo >= 0);
        
        // 应该能分配多个页面
        int pageNo2 = memoryManager.allocatePage();
        assertNotEquals(pageNo, pageNo2);
    }
    
    @Test
    void testFreePage() {
        int pageNo = memoryManager.allocatePage();
        assertTrue(pageNo >= 0);
        
        int freeBefore = memoryManager.getPhysicalMemory().getFreePages();
        
        // 释放页面
        memoryManager.freePage(pageNo);
        
        // 空闲页面应该增加
        int freeAfter = memoryManager.getPhysicalMemory().getFreePages();
        assertEquals(freeBefore + 1, freeAfter);
    }
    
    @Test
    void testCreateAddressSpace() {
        AddressSpace addressSpace = memoryManager.createAddressSpace();
        assertNotNull(addressSpace);
        assertNotNull(addressSpace.getPageTable());
    }
    
    @Test
    void testMultipleAddressSpaces() {
        AddressSpace space1 = memoryManager.createAddressSpace();
        AddressSpace space2 = memoryManager.createAddressSpace();
        
        assertNotNull(space1);
        assertNotNull(space2);
        assertNotSame(space1, space2);
    }
}

