package jinux.mm;

import jinux.include.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AddressSpace类的单元测试
 */
public class AddressSpaceTest {
    
    private MemoryManager memoryManager;
    private AddressSpace addressSpace;
    
    @BeforeEach
    void setUp() {
        memoryManager = new MemoryManager();
        addressSpace = memoryManager.createAddressSpace();
    }
    
    @Test
    void testAddressSpaceCreation() {
        assertNotNull(addressSpace);
        assertNotNull(addressSpace.getPageTable());
        assertEquals(0, addressSpace.getCodeStart());
        assertEquals(0, addressSpace.getCodeEnd());
        assertTrue(addressSpace.getStackTop() > 0);
    }
    
    @Test
    void testAllocateAndMap() {
        long vaddr = 0x1000;
        int flags = PageTable.PAGE_PRESENT | PageTable.PAGE_RW | PageTable.PAGE_USER;
        
        boolean result = addressSpace.allocateAndMap(vaddr, flags);
        assertTrue(result);
        
        // 检查是否已映射
        int vpage = (int) (vaddr >> Const.PAGE_SHIFT);
        assertTrue(addressSpace.getPageTable().isMapped(vpage));
    }
    
    @Test
    void testExpandBrk() {
        long initialBrk = addressSpace.getBrk();
        
        // 扩展堆
        long newBrk = addressSpace.expandBrk(initialBrk + Const.PAGE_SIZE);
        assertTrue(newBrk >= initialBrk);
        
        // 再次扩展
        long newBrk2 = addressSpace.expandBrk(newBrk + Const.PAGE_SIZE);
        assertTrue(newBrk2 > newBrk);
    }
    
    @Test
    void testExpandBrkAlignment() {
        long initialBrk = addressSpace.getBrk();
        
        // 尝试扩展到一个未对齐的地址
        long unalignedBrk = initialBrk + 100;
        long alignedBrk = addressSpace.expandBrk(unalignedBrk);
        
        // 结果应该是对齐到页边界的
        assertEquals(0, alignedBrk % Const.PAGE_SIZE);
    }
    
    @Test
    void testExpandBrkCannotShrink() {
        long initialBrk = addressSpace.getBrk();
        
        // 尝试缩小堆（应该失败）
        long smallerBrk = initialBrk - Const.PAGE_SIZE;
        long result = addressSpace.expandBrk(smallerBrk);
        
        // 应该保持原来的brk
        assertEquals(initialBrk, result);
    }
    
    @Test
    void testReadWriteByte() {
        long vaddr = 0x1000;
        int flags = PageTable.PAGE_PRESENT | PageTable.PAGE_RW | PageTable.PAGE_USER;
        
        // 分配并映射页面
        assertTrue(addressSpace.allocateAndMap(vaddr, flags));
        
        // 写入字节
        byte testValue = (byte) 0xAB;
        addressSpace.writeByte(vaddr, testValue);
        
        // 读取字节
        byte readValue = addressSpace.readByte(vaddr);
        assertEquals(testValue, readValue);
    }
    
    @Test
    void testReadWriteBytes() {
        long vaddr = 0x2000;
        int flags = PageTable.PAGE_PRESENT | PageTable.PAGE_RW | PageTable.PAGE_USER;
        
        assertTrue(addressSpace.allocateAndMap(vaddr, flags));
        
        byte[] writeBuf = {0x01, 0x02, 0x03, 0x04, 0x05};
        addressSpace.writeBytes(vaddr, writeBuf, 0, writeBuf.length);
        
        byte[] readBuf = new byte[writeBuf.length];
        addressSpace.readBytes(vaddr, readBuf, 0, readBuf.length);
        
        assertArrayEquals(writeBuf, readBuf);
    }
    
    @Test
    void testPageFaultException() {
        long vaddr = 0x3000;
        
        // 尝试读取未映射的地址应该抛出异常
        assertThrows(AddressSpace.PageFaultException.class, () -> {
            addressSpace.readByte(vaddr);
        });
        
        assertThrows(AddressSpace.PageFaultException.class, () -> {
            addressSpace.writeByte(vaddr, (byte) 0x01);
        });
    }
    
    @Test
    void testCopyAddressSpace() {
        long vaddr = 0x4000;
        int flags = PageTable.PAGE_PRESENT | PageTable.PAGE_RW | PageTable.PAGE_USER;
        
        // 分配并写入数据
        assertTrue(addressSpace.allocateAndMap(vaddr, flags));
        addressSpace.writeByte(vaddr, (byte) 0x42);
        
        // 复制地址空间
        AddressSpace copied = addressSpace.copy();
        assertNotNull(copied);
        assertNotSame(addressSpace, copied);
        
        // 复制的地址空间应该包含相同的数据
        byte value = copied.readByte(vaddr);
        assertEquals((byte) 0x42, value);
    }
    
    @Test
    void testFree() {
        long vaddr = 0x5000;
        int flags = PageTable.PAGE_PRESENT | PageTable.PAGE_RW | PageTable.PAGE_USER;
        
        assertTrue(addressSpace.allocateAndMap(vaddr, flags));
        assertTrue(addressSpace.getPageTable().isMapped((int) (vaddr >> Const.PAGE_SHIFT)));
        
        // 释放地址空间
        addressSpace.free();
        
        // 页表应该被清空
        assertEquals(0, addressSpace.getPageTable().getMappedPageCount());
    }
    
    @Test
    void testSegmentGettersAndSetters() {
        addressSpace.setCodeStart(0x1000);
        addressSpace.setCodeEnd(0x2000);
        addressSpace.setDataStart(0x3000);
        addressSpace.setDataEnd(0x4000);
        
        assertEquals(0x1000, addressSpace.getCodeStart());
        assertEquals(0x2000, addressSpace.getCodeEnd());
        assertEquals(0x3000, addressSpace.getDataStart());
        assertEquals(0x4000, addressSpace.getDataEnd());
        
        // setDataEnd应该同时设置brk
        assertEquals(0x4000, addressSpace.getBrk());
    }
}

