package jinux.fs;

import jinux.include.Const;

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
    }
    
    /**
     * 初始化一个新的文件系统
     */
    public void initNewFileSystem(int ninodes, int nzones) {
        this.ninodes = ninodes;
        this.nzones = nzones;
        
        // 计算位图大小（每个块可以表示 BLOCK_SIZE * 8 个 bit）
        int bitsPerBlock = Const.BLOCK_SIZE * 8;
        this.imapBlocks = (ninodes + bitsPerBlock - 1) / bitsPerBlock;
        this.zmapBlocks = (nzones + bitsPerBlock - 1) / bitsPerBlock;
        
        // 第一个数据块在所有元数据之后
        // 布局：超级块 | inode位图 | zone位图 | inode表 | 数据区
        int inodeBlocks = (ninodes * 32 + Const.BLOCK_SIZE - 1) / Const.BLOCK_SIZE;
        this.firstDataZone = 2 + imapBlocks + zmapBlocks + inodeBlocks;
        
        this.logZoneSize = 0; // zone_size = block_size
        this.maxSize = 7 * 1024 + 512 * 1024 + 512 * 512 * 1024; // 简化计算
        
        // 初始化位图
        int inodeBitmapSize = imapBlocks * Const.BLOCK_SIZE;
        int zoneBitmapSize = zmapBlocks * Const.BLOCK_SIZE;
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
     * 分配一个 inode
     */
    public int allocateInode() {
        if (inodeBitmap == null) {
            return -1;
        }
        
        // 查找第一个空闲 inode
        for (int i = 0; i < ninodes; i++) {
            int byteIdx = i / 8;
            int bitIdx = i % 8;
            
            if ((inodeBitmap[byteIdx] & (1 << bitIdx)) == 0) {
                // 找到空闲 inode，标记为已使用
                inodeBitmap[byteIdx] |= (1 << bitIdx);
                dirty = true;
                System.out.println("[FS] Allocated inode: " + i);
                return i;
            }
        }
        
        System.err.println("[FS] No free inodes available");
        return -1;
    }
    
    /**
     * 释放一个 inode
     */
    public void freeInode(int ino) {
        if (inodeBitmap == null || ino < 0 || ino >= ninodes) {
            return;
        }
        
        int byteIdx = ino / 8;
        int bitIdx = ino % 8;
        inodeBitmap[byteIdx] &= ~(1 << bitIdx);
        dirty = true;
        
        System.out.println("[FS] Freed inode: " + ino);
    }
    
    /**
     * 分配一个数据块
     */
    public int allocateBlock() {
        if (zoneBitmap == null) {
            return -1;
        }
        
        // 查找第一个空闲块
        for (int i = firstDataZone; i < nzones; i++) {
            int byteIdx = i / 8;
            int bitIdx = i % 8;
            
            if (byteIdx < zoneBitmap.length && 
                (zoneBitmap[byteIdx] & (1 << bitIdx)) == 0) {
                // 找到空闲块，标记为已使用
                zoneBitmap[byteIdx] |= (1 << bitIdx);
                dirty = true;
                System.out.println("[FS] Allocated block: " + i);
                return i;
            }
        }
        
        System.err.println("[FS] No free blocks available");
        return -1;
    }
    
    /**
     * 释放一个数据块
     */
    public void freeBlock(int blockNo) {
        if (zoneBitmap == null || blockNo < firstDataZone || blockNo >= nzones) {
            return;
        }
        
        int byteIdx = blockNo / 8;
        int bitIdx = blockNo % 8;
        if (byteIdx < zoneBitmap.length) {
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
