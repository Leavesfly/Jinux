package jinux.fs;

import jinux.include.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * FileDescriptorTable类的单元测试
 */
public class FileDescriptorTableTest {
    
    private FileDescriptorTable fdTable;
    private Inode testInode;
    
    @BeforeEach
    void setUp() {
        fdTable = new FileDescriptorTable();
        testInode = new Inode(1, Const.ROOT_DEV);
    }
    
    @Test
    void testFileDescriptorTableCreation() {
        assertNotNull(fdTable);
    }
    
    @Test
    void testAllocate() {
        File file = new File(testInode, File.O_RDONLY);
        int fd = fdTable.allocate(file);
        
        assertTrue(fd >= 0);
        assertSame(file, fdTable.get(fd));
    }
    
    @Test
    void testAllocateMultiple() {
        File file1 = new File(testInode, File.O_RDONLY);
        File file2 = new File(testInode, File.O_WRONLY);
        
        int fd1 = fdTable.allocate(file1);
        int fd2 = fdTable.allocate(file2);
        
        assertNotEquals(fd1, fd2);
        assertSame(file1, fdTable.get(fd1));
        assertSame(file2, fdTable.get(fd2));
    }
    
    @Test
    void testGet() {
        File file = new File(testInode, File.O_RDONLY);
        int fd = fdTable.allocate(file);
        
        assertSame(file, fdTable.get(fd));
        assertNull(fdTable.get(-1));
        assertNull(fdTable.get(Const.NR_OPEN));
    }
    
    @Test
    void testClose() {
        File file = new File(testInode, File.O_RDONLY);
        int fd = fdTable.allocate(file);
        
        assertNotNull(fdTable.get(fd));
        
        fdTable.close(fd);
        assertNull(fdTable.get(fd));
    }
    
    @Test
    void testCloseInvalidFd() {
        // 关闭无效的文件描述符不应该崩溃
        fdTable.close(-1);
        fdTable.close(Const.NR_OPEN);
    }
    
    @Test
    void testCloseAll() {
        File file1 = new File(testInode, File.O_RDONLY);
        File file2 = new File(testInode, File.O_WRONLY);
        
        int fd1 = fdTable.allocate(file1);
        int fd2 = fdTable.allocate(file2);
        
        assertNotNull(fdTable.get(fd1));
        assertNotNull(fdTable.get(fd2));
        
        fdTable.closeAll();
        
        assertNull(fdTable.get(fd1));
        assertNull(fdTable.get(fd2));
    }
    
    @Test
    void testCopy() {
        File file1 = new File(testInode, File.O_RDONLY);
        File file2 = new File(testInode, File.O_WRONLY);
        
        int fd1 = fdTable.allocate(file1);
        int fd2 = fdTable.allocate(file2);
        
        FileDescriptorTable copied = fdTable.copy();
        assertNotNull(copied);
        assertNotSame(fdTable, copied);
        
        // 复制的表应该包含相同的文件引用
        assertSame(file1, copied.get(fd1));
        assertSame(file2, copied.get(fd2));
    }
    
    @Test
    void testAllocateFull() {
        // 分配所有可用的文件描述符
        for (int i = 0; i < Const.NR_OPEN; i++) {
            File file = new File(testInode, File.O_RDONLY);
            int fd = fdTable.allocate(file);
            assertTrue(fd >= 0);
        }
        
        // 再分配一个应该失败
        File file = new File(testInode, File.O_RDONLY);
        int fd = fdTable.allocate(file);
        assertEquals(-1, fd);
    }
    
    @Test
    void testReuseFdAfterClose() {
        File file1 = new File(testInode, File.O_RDONLY);
        int fd1 = fdTable.allocate(file1);
        
        fdTable.close(fd1);
        
        // 应该能重用文件描述符
        File file2 = new File(testInode, File.O_WRONLY);
        int fd2 = fdTable.allocate(file2);
        
        assertEquals(fd1, fd2);
        assertSame(file2, fdTable.get(fd2));
    }
}

