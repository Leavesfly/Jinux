package jinux.mm;

import jinux.include.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * PageTable类的单元测试
 */
public class PageTableTest {
    
    private PageTable pageTable;
    
    @BeforeEach
    void setUp() {
        pageTable = new PageTable();
    }
    
    @Test
    void testPageTableCreation() {
        assertNotNull(pageTable);
        assertEquals(0, pageTable.getMappedPageCount());
    }
    
    @Test
    void testMap() {
        int vpage = 10;
        int ppage = 100;
        int flags = PageTable.PAGE_PRESENT | PageTable.PAGE_RW;
        
        pageTable.map(vpage, ppage, flags);
        
        assertTrue(pageTable.isMapped(vpage));
        assertEquals(ppage, pageTable.getPhysicalPage(vpage));
    }
    
    @Test
    void testUnmap() {
        int vpage = 20;
        int ppage = 200;
        int flags = PageTable.PAGE_PRESENT;
        
        pageTable.map(vpage, ppage, flags);
        assertTrue(pageTable.isMapped(vpage));
        
        pageTable.unmap(vpage);
        assertFalse(pageTable.isMapped(vpage));
        assertEquals(-1, pageTable.getPhysicalPage(vpage));
    }
    
    @Test
    void testTranslate() {
        int vpage = 5;
        int ppage = 50;
        int offset = 0x123;
        int flags = PageTable.PAGE_PRESENT;
        
        pageTable.map(vpage, ppage, flags);
        
        long vaddr = ((long) vpage << Const.PAGE_SHIFT) | offset;
        long paddr = pageTable.translate(vaddr);
        
        long expectedPaddr = ((long) ppage << Const.PAGE_SHIFT) | offset;
        assertEquals(expectedPaddr, paddr);
    }
    
    @Test
    void testTranslateUnmapped() {
        long vaddr = 0x1000;
        long paddr = pageTable.translate(vaddr);
        
        assertEquals(-1, paddr);
    }
    
    @Test
    void testCheckPermission() {
        int vpage = 30;
        int ppage = 300;
        int flags = PageTable.PAGE_PRESENT | PageTable.PAGE_RW | PageTable.PAGE_USER;
        
        pageTable.map(vpage, ppage, flags);
        
        assertTrue(pageTable.checkPermission(vpage, PageTable.PAGE_PRESENT));
        assertTrue(pageTable.checkPermission(vpage, PageTable.PAGE_RW));
        assertTrue(pageTable.checkPermission(vpage, PageTable.PAGE_USER));
        assertTrue(pageTable.checkPermission(vpage, PageTable.PAGE_PRESENT | PageTable.PAGE_RW));
        
        // 检查不存在的页面
        assertFalse(pageTable.checkPermission(999, PageTable.PAGE_PRESENT));
    }
    
    @Test
    void testCopy() {
        int vpage1 = 1;
        int vpage2 = 2;
        int ppage1 = 10;
        int ppage2 = 20;
        int flags = PageTable.PAGE_PRESENT;
        
        pageTable.map(vpage1, ppage1, flags);
        pageTable.map(vpage2, ppage2, flags);
        
        PageTable copied = pageTable.copy();
        assertNotNull(copied);
        assertNotSame(pageTable, copied);
        
        assertEquals(2, copied.getMappedPageCount());
        assertTrue(copied.isMapped(vpage1));
        assertTrue(copied.isMapped(vpage2));
        assertEquals(ppage1, copied.getPhysicalPage(vpage1));
        assertEquals(ppage2, copied.getPhysicalPage(vpage2));
    }
    
    @Test
    void testClear() {
        int vpage = 40;
        int ppage = 400;
        int flags = PageTable.PAGE_PRESENT;
        
        pageTable.map(vpage, ppage, flags);
        assertEquals(1, pageTable.getMappedPageCount());
        
        pageTable.clear();
        assertEquals(0, pageTable.getMappedPageCount());
        assertFalse(pageTable.isMapped(vpage));
    }
    
    @Test
    void testMultipleMappings() {
        for (int i = 0; i < 10; i++) {
            pageTable.map(i, i * 10, PageTable.PAGE_PRESENT);
        }
        
        assertEquals(10, pageTable.getMappedPageCount());
        
        for (int i = 0; i < 10; i++) {
            assertTrue(pageTable.isMapped(i));
            assertEquals(i * 10, pageTable.getPhysicalPage(i));
        }
    }
}

