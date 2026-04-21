package jinux.fs;

import jinux.include.FileSystemConstants;

/**
 * 超级块
 * 对应 Linux 0.01 中的 struct super_block
 * 
 * 描述文件系统的全局信息
 * 
 * @author Jinux Project
 */
public class SuperBlock {
    
    /** 设备号 */
    private int dev;
    
    /** inode 总数 */
    private int ninodes;
    
    /** 数据块总数 */
    private int nzones;
    
    /** inode 位图块数 */
    private int imapBlocks;
    
    /** 数据块位图块数 */
    private int zmapBlocks;
    
    /** 第一个数据块号 */
    private int firstDataZone;
    
    /** log2(zone_size / block_size) */
    private int logZoneSize;
    
    /** 最大文件大小 */
    private long maxSize;
    
    /** 魔数（标识文件系统类型） */
    private int magic;
    
    /** 根目录 inode */
    private Inode rootInode;
    
    /** inode 位图（简化：用数组表示） */
    private byte[] inodeBitmap;
    
    /** 数据块位图 */
    private byte[] zoneBitmap;
    
    /** 是否已挂载 */
    private boolean mounted;
    
    /** 是否被修改 */
    private boolean dirty;
    
    /** 上次分配的 inode 位置（next-fit 优化） */
    private int lastAllocatedInode;
    
    /** 上次分配的 block 位置（next-fit 优化） */
    private int lastAllocatedBlock;
    
    // ==================== 文件系统魔数 ====================
    
    /** MINIX 文件系统魔数 */
    public static final int MINIX_SUPER_MAGIC = 0x137F;
    
    /** MINIX V2 文件系统魔数 */
    public static final int MINIX2_SUPER_MAGIC = 0x2468;
    
    /**
     * 构造超级块
     */
    public SuperBlock(int dev) {
        this.dev = dev;
        this.ninodes = 0;
        this.nzones = 0;
        this.imapBlocks = 0;
        this.zmapBlocks = 0;
        this.firstDataZone = 0;
        this.logZoneSize = 0;
        this.maxSize = 0;
        this.magic = MINIX_SUPER_MAGIC;
        this.rootInode = null;
        this.mounted = false;
        this.dirty = false;
        this.lastAllocatedInode = 0;
        this.lastAllocatedBlock = 0;
    }
    
    /**
     * 初始化一个新的文件系统
     */
    public void initNewFileSystem(int ninodes, int nzones) {
        this.ninodes = ninodes;
        this.nzones = nzones;
        
        // 计算位图大小（每个块可以表示 BLOCK_SIZE * 8 个 bit）
        int bitsPerBlock = FileSystemConstants.BLOCK_SIZE * 8;
        this.imapBlocks = (ninodes + bitsPerBlock - 1) / bitsPerBlock;
        this.zmapBlocks = (nzones + bitsPerBlock - 1) / bitsPerBlock;
        
        // 第一个数据块在所有元数据之后
        // 布局：超级块 | inode位图 | zone位图 | inode表 | 数据区
        int inodeBlocks = (ninodes * 32 + FileSystemConstants.BLOCK_SIZE - 1) / FileSystemConstants.BLOCK_SIZE;
        this.firstDataZone = 2 + imapBlocks + zmapBlocks + inodeBlocks;
        
        this.logZoneSize = 0; // zone_size = block_size
        this.maxSize = 7 * 1024 + 512 * 1024 + 512 * 512 * 1024; // 简化计算
        
        // 初始化位图
        int inodeBitmapSize = imapBlocks * FileSystemConstants.BLOCK_SIZE;
        int zoneBitmapSize = zmapBlocks * FileSystemConstants.BLOCK_SIZE;
        this.inodeBitmap = new byte[inodeBitmapSize];
        this.zoneBitmap = new byte[zoneBitmapSize];
        
        // 位图的第 0 位总是被占用（保留）
        inodeBitmap[0] = 1;
        zoneBitmap[0] = 1;
        
        this.dirty = true;
        
        System.out.println("[FS] Initialized filesystem: " + 
            ninodes + " inodes, " + nzones + " zones");
    }
    
    /**
     * 分配一个 inode（线程安全）
     * 
     * @return 分配的 inode 号，失败返回 -1
     */
    public synchronized int allocateInode() {
        if (inodeBitmap == null) {
            return -1;
        }
        
        // next-fit：从上次分配位置开始搜索，绕回一圈
        for (int count = 0; count < ninodes; count++) {
            int i = (lastAllocatedInode + count) % ninodes;
            int byteIdx = i / 8;
            int bitIdx = i % 8;
            
            if ((inodeBitmap[byteIdx] & (1 << bitIdx)) == 0) {
                inodeBitmap[byteIdx] |= (1 << bitIdx);
                dirty = true;
                lastAllocatedInode = (i + 1) % ninodes;
                System.out.println("[FS] Allocated inode: " + i);
                return i;
            }
        }
        
        System.err.println("[FS] No free inodes available");
        return -1;
    }
    
    /**
     * 释放一个 inode（线程安全）
     * 
     * @param ino 要释放的 inode 号
     */
    public synchronized void freeInode(int ino) {
        if (inodeBitmap == null || ino < 0 || ino >= ninodes) {
            return;
        }
        
        int byteIdx = ino / 8;
        int bitIdx = ino % 8;
        
        // 防止重复释放
        if ((inodeBitmap[byteIdx] & (1 << bitIdx)) == 0) {
            System.err.println("[FS] Warning: inode " + ino + " is already free");
            return;
        }
        
        inodeBitmap[byteIdx] &= ~(1 << bitIdx);
        dirty = true;
        
        System.out.println("[FS] Freed inode: " + ino);
    }
    
    /**
     * 分配一个数据块（线程安全）
     * 
     * @return 分配的块号，失败返回 -1
     */
    public synchronized int allocateBlock() {
        if (zoneBitmap == null) {
            return -1;
        }
        
        // next-fit：从上次分配位置开始搜索，绕回一圈
        int searchRange = nzones - firstDataZone;
        int startPos = Math.max(lastAllocatedBlock, firstDataZone);
        
        for (int count = 0; count < searchRange; count++) {
            int i = firstDataZone + ((startPos - firstDataZone + count) % searchRange);
            int byteIdx = i / 8;
            int bitIdx = i % 8;
            
            if (byteIdx < zoneBitmap.length && 
                (zoneBitmap[byteIdx] & (1 << bitIdx)) == 0) {
                zoneBitmap[byteIdx] |= (1 << bitIdx);
                dirty = true;
                lastAllocatedBlock = i + 1;
                System.out.println("[FS] Allocated block: " + i);
                return i;
            }
        }
        
        System.err.println("[FS] No free blocks available");
        return -1;
    }
    
    /**
     * 释放一个数据块（线程安全）
     * 
     * @param blockNo 要释放的块号
     */
    public synchronized void freeBlock(int blockNo) {
        if (zoneBitmap == null || blockNo < firstDataZone || blockNo >= nzones) {
            return;
        }
        
        int byteIdx = blockNo / 8;
        int bitIdx = blockNo % 8;
        if (byteIdx < zoneBitmap.length) {
            // 防止重复释放
            if ((zoneBitmap[byteIdx] & (1 << bitIdx)) == 0) {
                System.err.println("[FS] Warning: block " + blockNo + " is already free");
                return;
            }
            zoneBitmap[byteIdx] &= ~(1 << bitIdx);
            dirty = true;
            System.out.println("[FS] Freed block: " + blockNo);
        }
    }
    
    @Override
    public String toString() {
        return String.format("SuperBlock[dev=%d, inodes=%d, zones=%d, magic=0x%x, mounted=%b]",
            dev, ninodes, nzones, magic, mounted);
    }
    
    // ==================== Getters and Setters ====================
    
    public int getDev() {
        return dev;
    }
    
    public int getNinodes() {
        return ninodes;
    }
    
    public int getNzones() {
        return nzones;
    }
    
    public int getImapBlocks() {
        return imapBlocks;
    }
    
    public int getZmapBlocks() {
        return zmapBlocks;
    }
    
    public int getFirstDataZone() {
        return firstDataZone;
    }
    
    public int getLogZoneSize() {
        return logZoneSize;
    }
    
    public long getMaxSize() {
        return maxSize;
    }
    
    public int getMagic() {
        return magic;
    }
    
    public Inode getRootInode() {
        return rootInode;
    }
    
    public void setRootInode(Inode rootInode) {
        this.rootInode = rootInode;
    }
    
    public boolean isMounted() {
        return mounted;
    }
    
    public void setMounted(boolean mounted) {
        this.mounted = mounted;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
}
