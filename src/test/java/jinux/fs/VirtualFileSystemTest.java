package jinux.fs;

import jinux.include.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * VirtualFileSystem类的单元测试
 */
public class VirtualFileSystemTest {
    
    private VirtualFileSystem vfs;
    
    @BeforeEach
    void setUp() {
        vfs = new VirtualFileSystem();
        vfs.init();
    }
    
    @Test
    void testVfsCreation() {
        assertNotNull(vfs);
    }
    
    @Test
    void testInit() {
        VirtualFileSystem newVfs = new VirtualFileSystem();
        newVfs.init();
        
        assertNotNull(newVfs.getRootInode());
        assertNotNull(newVfs.getRootSuperBlock());
    }
    
    @Test
    void testGetRootInode() {
        Inode rootInode = vfs.getRootInode();
        assertNotNull(rootInode);
        assertEquals(1, rootInode.getIno());
        assertTrue((rootInode.getMode() & Inode.S_IFDIR) != 0);
    }
    
    @Test
    void testGetInode() {
        int dev = Const.ROOT_DEV;
        int ino = 10;
        
        Inode inode = vfs.getInode(dev, ino);
        assertNotNull(inode);
        assertEquals(ino, inode.getIno());
        assertEquals(dev, inode.getDev());
        
        // 应该增加引用计数
        assertTrue(inode.getRefCount() > 0);
    }
    
    @Test
    void testGetInodeCaching() {
        int dev = Const.ROOT_DEV;
        int ino = 20;
        
        Inode inode1 = vfs.getInode(dev, ino);
        Inode inode2 = vfs.getInode(dev, ino);
        
        // 应该返回同一个inode对象（缓存）
        assertSame(inode1, inode2);
    }
    
    @Test
    void testPutInode() {
        int dev = Const.ROOT_DEV;
        int ino = 30;
        
        Inode inode = vfs.getInode(dev, ino);
        int initialRefCount = inode.getRefCount();
        
        // 释放inode
        vfs.putInode(inode);
        
        // 引用计数应该减少
        assertEquals(initialRefCount - 1, inode.getRefCount());
    }
    
    @Test
    void testPutInodeNull() {
        // 释放null不应该崩溃
        vfs.putInode(null);
    }
    
    @Test
    void testGetBuffer() {
        int dev = Const.ROOT_DEV;
        int blockNo = 5;
        
        BufferCache buffer = vfs.getBuffer(dev, blockNo);
        assertNotNull(buffer);
        assertEquals(dev, buffer.getDev());
        assertEquals(blockNo, buffer.getBlockNo());
        assertTrue(buffer.getRefCount() > 0);
    }
    
    @Test
    void testGetBufferCaching() {
        int dev = Const.ROOT_DEV;
        int blockNo = 10;
        
        BufferCache buffer1 = vfs.getBuffer(dev, blockNo);
        BufferCache buffer2 = vfs.getBuffer(dev, blockNo);
        
        // 应该返回同一个缓冲区对象（缓存）
        assertSame(buffer1, buffer2);
    }
    
    @Test
    void testReleaseBuffer() {
        int dev = Const.ROOT_DEV;
        int blockNo = 15;
        
        BufferCache buffer = vfs.getBuffer(dev, blockNo);
        int initialRefCount = buffer.getRefCount();
        
        vfs.releaseBuffer(buffer);
        
        // 引用计数应该减少
        assertEquals(initialRefCount - 1, buffer.getRefCount());
    }
    
    @Test
    void testReleaseBufferNull() {
        // 释放null不应该崩溃
        vfs.releaseBuffer(null);
    }
    
    @Test
    void testSync() {
        int dev = Const.ROOT_DEV;
        int blockNo = 20;
        
        BufferCache buffer = vfs.getBuffer(dev, blockNo);
        buffer.setDirty(true);
        
        // sync应该处理脏缓冲区
        vfs.sync();
        
        // 注意：实际实现可能不会立即清除dirty标志
        // 这里主要测试方法不会崩溃
    }
    
    @Test
    void testGetRootSuperBlock() {
        SuperBlock rootSuperBlock = vfs.getRootSuperBlock();
        assertNotNull(rootSuperBlock);
        assertEquals(Const.ROOT_DEV, rootSuperBlock.getDev());
    }
    
    @Test
    void testGetFileTable() {
        File[] fileTable = vfs.getFileTable();
        assertNotNull(fileTable);
        assertEquals(Const.NR_FILE, fileTable.length);
    }
}

