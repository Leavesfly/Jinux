package jinux.fs;

import jinux.drivers.VirtualDiskDevice;
import jinux.include.FileSystemConstants;

import java.util.Map;

/**
 * Inode 管理器实现类
 * 负责 Inode 的获取、释放和持久化
 */
public class InodeManagerImpl implements InodeManager {
    private final Map<Integer, Inode> inodeCache;
    private final Map<Integer, SuperBlock> superBlocks;
    private BlockBufferManager bufferManager;
    private VirtualDiskDevice disk;
    
    public InodeManagerImpl(Map<Integer, Inode> inodeCache, Map<Integer, SuperBlock> superBlocks) {
        this.inodeCache = inodeCache;
        this.superBlocks = superBlocks;
    }
    
    public void setBufferManager(BlockBufferManager bufferManager) {
        this.bufferManager = bufferManager;
    }
    
    public void setDisk(VirtualDiskDevice disk) {
        this.disk = disk;
    }
    
    @Override
    public Inode getInode(int dev, int ino) {
        // 从缓存查找
        Inode inode = inodeCache.get(ino);
        if (inode != null) {
            inode.incrementRef();
            return inode;
        }
        
        // 创建新 inode
        inode = new Inode(ino, dev);
        inodeCache.put(ino, inode);
        inode.incrementRef();
        
        return inode;
    }
    
    @Override
    public void putInode(Inode inode) {
        if (inode == null) {
            return;
        }
        
        inode.decrementRef();
        
        // 如果引用计数为 0，从缓存中移除
        if (inode.getRefCount() == 0) {
            if (inode.isDirty()) {
                writeInodeToDisk(inode);
            }
            inodeCache.remove(inode.getIno());
        }
    }
    
    /**
     * 将 inode 写回磁盘
     * 
     * @param inode inode 对象
     */
    private void writeInodeToDisk(Inode inode) {
        if (disk == null) {
            System.err.println("[VFS] No disk device for writing inode " + inode.getIno());
            return;
        }
        
        // 获取超级块
        SuperBlock sb = superBlocks.get(inode.getDev());
        if (sb == null) {
            System.err.println("[VFS] No super block for device " + inode.getDev());
            return;
        }
        
        // 计算 inode 在磁盘上的位置
        // 布局：超级块(1) | inode位图 | zone位图 | inode表 | 数据区
        // inode 表从块号 2 + imapBlocks + zmapBlocks 开始
        int inodeTableStart = 2 + sb.getImapBlocks() + sb.getZmapBlocks();
        int inodeSize = FileSystemConstants.INODE_DISK_SIZE; // Linux 0.01 inode 大小为 INODE_DISK_SIZE 字节
        int inodesPerBlock = FileSystemConstants.BLOCK_SIZE / inodeSize;
        int inodeIndex = inode.getIno() - 1; // inode 编号从 1 开始
        
        int blockNo = inodeTableStart + (inodeIndex / inodesPerBlock);
        int offsetInBlock = (inodeIndex % inodesPerBlock) * inodeSize;
        
        try {
            // 读取包含该 inode 的块
            BufferCache buffer = bufferManager.getBuffer(inode.getDev(), blockNo);
            byte[] blockData = buffer.getData();
            
            // 将 inode 数据序列化到缓冲区
            serializeInode(inode, blockData, offsetInBlock);
            
            // 标记缓冲区为脏
            buffer.markDirty();
            buffer.setDirty(true);
            
            // 写回缓冲区
            bufferManager.releaseBuffer(buffer);
            
            inode.setDirty(false);
            
        } catch (Exception e) {
            System.err.println("[VFS] Exception writing inode " + inode.getIno() + 
                ": " + e.getMessage());
        }
    }
    
    /**
     * 序列化 inode 到字节数组
     * 按照 MINIX 文件系统 inode 格式序列化所有字段
     * 
     * 布局（32 字节）：
     *   mode(2) + uid(2) + size(4) + mtime(4) + gid(1) + nlink(1)
     *   + directBlocks(7*2=14) + indirectBlock(2) + doubleIndirectBlock(2)
     * 
     * @param inode inode 对象
     * @param buf 目标缓冲区
     * @param offset 偏移量
     */
    private void serializeInode(Inode inode, byte[] buf, int offset) {
        int pos = offset;
        
        // mode (2 bytes)
        buf[pos++] = (byte) (inode.getMode() & 0xFF);
        buf[pos++] = (byte) ((inode.getMode() >> 8) & 0xFF);
        
        // uid (2 bytes)
        buf[pos++] = (byte) (inode.getUid() & 0xFF);
        buf[pos++] = (byte) ((inode.getUid() >> 8) & 0xFF);
        
        // size (4 bytes)
        long size = inode.getSize();
        buf[pos++] = (byte) (size & 0xFF);
        buf[pos++] = (byte) ((size >> 8) & 0xFF);
        buf[pos++] = (byte) ((size >> 16) & 0xFF);
        buf[pos++] = (byte) ((size >> 24) & 0xFF);
        
        // mtime (4 bytes)
        long mtime = inode.getMtime();
        buf[pos++] = (byte) (mtime & 0xFF);
        buf[pos++] = (byte) ((mtime >> 8) & 0xFF);
        buf[pos++] = (byte) ((mtime >> 16) & 0xFF);
        buf[pos++] = (byte) ((mtime >> 24) & 0xFF);
        
        // gid (1 byte)
        buf[pos++] = (byte) (inode.getGid() & 0xFF);
        
        // nlink (1 byte)
        buf[pos++] = (byte) (inode.getNlink() & 0xFF);
        
        // 直接块指针 (MINIX_DIRECT_BLOCKS * 2 = 14 bytes) — MINIX v1 使用 MINIX_DIRECT_BLOCKS 个直接块
        int[] directBlocks = inode.getDirectBlocks();
        for (int i = 0; i < FileSystemConstants.MINIX_DIRECT_BLOCKS; i++) {
            int block = (i < directBlocks.length) ? directBlocks[i] : 0;
            buf[pos++] = (byte) (block & 0xFF);
            buf[pos++] = (byte) ((block >> 8) & 0xFF);
        }
        
        // 一级间接块指针 (2 bytes)
        int indirect = inode.getIndirectBlock();
        buf[pos++] = (byte) (indirect & 0xFF);
        buf[pos++] = (byte) ((indirect >> 8) & 0xFF);
        
        // 二级间接块指针 (2 bytes)
        int doubleIndirect = inode.getDoubleIndirectBlock();
        buf[pos++] = (byte) (doubleIndirect & 0xFF);
        buf[pos++] = (byte) ((doubleIndirect >> 8) & 0xFF);
    }
}
