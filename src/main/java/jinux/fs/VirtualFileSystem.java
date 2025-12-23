package jinux.fs;

import jinux.drivers.VirtualDiskDevice;
import jinux.include.Const;
import java.util.HashMap;
import java.util.Map;

/**
 * 虚拟文件系统
 * 对应 Linux 0.01 中的 VFS 层
 * 
 * @author Jinux Project
 */
public class VirtualFileSystem {
    
    /** 超级块表（设备号 -> SuperBlock） */
    private final Map<Integer, SuperBlock> superBlocks;
    
    /** Inode 缓存（ino -> Inode） */
    private final Map<Integer, Inode> inodeCache;
    
    /** 缓冲区缓存 */
    private final Map<String, BufferCache> bufferCache;
    
    /** 系统文件表 */
    private final File[] fileTable;
    
    /** 虚拟磁盘设备 */
    private VirtualDiskDevice disk;
    
    /** 根文件系统超级块 */
    private SuperBlock rootSuperBlock;
    
    /**
     * 构造 VFS
     */
    public VirtualFileSystem() {
        this.superBlocks = new HashMap<>();
        this.inodeCache = new HashMap<>();
        this.bufferCache = new HashMap<>();
        this.fileTable = new File[Const.NR_FILE];
    }
    
    /**
     * 设置虚拟磁盘设备
     */
    public void setDisk(VirtualDiskDevice disk) {
        this.disk = disk;
    }
    
    /**
     * 初始化文件系统
     */
    public void init() {
        System.out.println("[VFS] Initializing virtual file system...");
        
        // 创建根文件系统
        int rootDev = Const.ROOT_DEV;
        rootSuperBlock = new SuperBlock(rootDev);
        rootSuperBlock.initNewFileSystem(1024, 10240); // 1024 inodes, 10240 blocks
        rootSuperBlock.setMounted(true);
        
        superBlocks.put(rootDev, rootSuperBlock);
        
        // 创建根目录 inode
        int rootIno = rootSuperBlock.allocateInode();
        if (rootIno >= 0) {
            Inode rootInode = new Inode(rootIno, rootDev);
            rootInode.setMode(Inode.S_IFDIR | 0755);
            rootInode.setNlink(2); // . 和 .. 
            rootInode.setSize(0);
            rootInode.setLoaded(true);
            
            rootSuperBlock.setRootInode(rootInode);
            inodeCache.put(rootIno, rootInode);
            
            System.out.println("[VFS] Created root directory: " + rootInode);
        }
        
        System.out.println("[VFS] Virtual file system initialized");
        System.out.println("[VFS] Root super block: " + rootSuperBlock);
    }
    
    /**
     * 获取根 inode
     */
    public Inode getRootInode() {
        if (rootSuperBlock != null) {
            return rootSuperBlock.getRootInode();
        }
        return null;
    }
    
    /**
     * 获取或创建 inode
     */
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
    
    /**
     * 释放 inode
     */
    public void putInode(Inode inode) {
        if (inode == null) {
            return;
        }
        
        inode.decrementRef();
        
        // 如果引用计数为 0，从缓存中移除
        if (inode.getRefCount() == 0) {
            if (inode.isDirty()) {
                // TODO: 写回磁盘
            }
            inodeCache.remove(inode.getIno());
        }
    }
    
    /**
     * 获取缓冲区
     */
    public BufferCache getBuffer(int dev, int blockNo) {
        String key = dev + ":" + blockNo;
        BufferCache buffer = bufferCache.get(key);
        
        if (buffer == null) {
            // 创建新缓冲区
            buffer = new BufferCache(dev, blockNo);
            bufferCache.put(key, buffer);
            
            // TODO: 从磁盘读取
        }
        
        buffer.incrementRef();
        return buffer;
    }
    
    /**
     * 释放缓冲区
     */
    public void releaseBuffer(BufferCache buffer) {
        if (buffer != null) {
            buffer.decrementRef();
            
            // TODO: 如果 dirty，写回磁盘
        }
    }
    
    /**
     * 同步所有缓冲区
     */
    public void sync() {
        System.out.println("[VFS] Syncing all buffers...");
        
        int synced = 0;
        for (BufferCache buffer : bufferCache.values()) {
            if (buffer.isDirty()) {
                // TODO: 写回磁盘
                buffer.setDirty(false);
                synced++;
            }
        }
        
        System.out.println("[VFS] Synced " + synced + " dirty buffers");
    }
    
    /**
     * 打印统计信息
     */
    public void printStats() {
        System.out.println("\n[VFS] File System Statistics:");
        System.out.println("  Mounted super blocks: " + superBlocks.size());
        System.out.println("  Cached inodes: " + inodeCache.size());
        System.out.println("  Buffer cache entries: " + bufferCache.size());
        
        int openFiles = 0;
        for (File file : fileTable) {
            if (file != null) {
                openFiles++;
            }
        }
        System.out.println("  Open files: " + openFiles + "/" + fileTable.length);
        
        if (rootSuperBlock != null) {
            System.out.println("  Root filesystem: " + rootSuperBlock);
        }
    }
    
    // ==================== Getters ====================
    
    public SuperBlock getRootSuperBlock() {
        return rootSuperBlock;
    }
    
    public File[] getFileTable() {
        return fileTable;
    }
}
