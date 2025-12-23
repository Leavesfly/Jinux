package jinux.fs;

import jinux.include.Const;

/**
 * Inode（索引节点）
 * 对应 Linux 0.01 中的 struct inode
 * 
 * 表示文件系统中的一个文件或目录
 * 
 * @author Jinux Project
 */
public class Inode {
    
    /** inode 编号 */
    private int ino;
    
    /** 设备号 */
    private int dev;
    
    /** 文件类型和权限 */
    private int mode;
    
    /** 硬链接数 */
    private int nlink;
    
    /** 所有者用户 ID */
    private int uid;
    
    /** 所有者组 ID */
    private int gid;
    
    /** 文件大小（字节） */
    private long size;
    
    /** 访问时间 */
    private long atime;
    
    /** 修改时间 */
    private long mtime;
    
    /** 创建时间 */
    private long ctime;
    
    /** 直接块指针（指向数据块号）*/
    private int[] directBlocks;
    
    /** 一级间接块指针 */
    private int indirectBlock;
    
    /** 二级间接块指针 */
    private int doubleIndirectBlock;
    
    /** 引用计数（内存中的引用） */
    private int refCount;
    
    /** 是否被修改（需要写回磁盘） */
    private boolean dirty;
    
    /** 是否已加载 */
    private boolean loaded;
    
    // ==================== 文件类型常量 ====================
    
    /** 普通文件 */
    public static final int S_IFREG = 0100000;
    
    /** 目录 */
    public static final int S_IFDIR = 0040000;
    
    /** 字符设备 */
    public static final int S_IFCHR = 0020000;
    
    /** 块设备 */
    public static final int S_IFBLK = 0060000;
    
    /** 文件类型掩码 */
    public static final int S_IFMT = 0170000;
    
    // ==================== 权限常量 ====================
    
    /** 用户读权限 */
    public static final int S_IRUSR = 0000400;
    
    /** 用户写权限 */
    public static final int S_IWUSR = 0000200;
    
    /** 用户执行权限 */
    public static final int S_IXUSR = 0000100;
    
    /** 组读权限 */
    public static final int S_IRGRP = 0000040;
    
    /** 组写权限 */
    public static final int S_IWGRP = 0000020;
    
    /** 组执行权限 */
    public static final int S_IXGRP = 0000010;
    
    /** 其他读权限 */
    public static final int S_IROTH = 0000004;
    
    /** 其他写权限 */
    public static final int S_IWOTH = 0000002;
    
    /** 其他执行权限 */
    public static final int S_IXOTH = 0000001;
    
    /**
     * 构造 Inode
     */
    public Inode(int ino, int dev) {
        this.ino = ino;
        this.dev = dev;
        this.mode = 0;
        this.nlink = 0;
        this.uid = 0;
        this.gid = 0;
        this.size = 0;
        this.directBlocks = new int[10]; // Linux 0.01 使用 10 个直接块
        this.indirectBlock = 0;
        this.doubleIndirectBlock = 0;
        this.refCount = 0;
        this.dirty = false;
        this.loaded = false;
        
        long now = System.currentTimeMillis() / 1000;
        this.atime = now;
        this.mtime = now;
        this.ctime = now;
    }
    
    /**
     * 检查是否为目录
     */
    public boolean isDirectory() {
        return (mode & S_IFMT) == S_IFDIR;
    }
    
    /**
     * 检查是否为普通文件
     */
    public boolean isRegularFile() {
        return (mode & S_IFMT) == S_IFREG;
    }
    
    /**
     * 检查是否为字符设备
     */
    public boolean isCharDevice() {
        return (mode & S_IFMT) == S_IFCHR;
    }
    
    /**
     * 检查是否为块设备
     */
    public boolean isBlockDevice() {
        return (mode & S_IFMT) == S_IFBLK;
    }
    
    /**
     * 增加引用计数
     */
    public void incrementRef() {
        refCount++;
    }
    
    /**
     * 减少引用计数
     */
    public void decrementRef() {
        if (refCount > 0) {
            refCount--;
        }
    }
    
    /**
     * 标记为已修改
     */
    public void markDirty() {
        this.dirty = true;
    }
    
    @Override
    public String toString() {
        String type = isDirectory() ? "DIR" : 
                     isRegularFile() ? "FILE" : 
                     isCharDevice() ? "CHR" : 
                     isBlockDevice() ? "BLK" : "?";
        
        return String.format("Inode[ino=%d, type=%s, size=%d, nlink=%d, ref=%d]",
            ino, type, size, nlink, refCount);
    }
    
    // ==================== Getters and Setters ====================
    
    public int getIno() {
        return ino;
    }
    
    public int getDev() {
        return dev;
    }
    
    public int getMode() {
        return mode;
    }
    
    public void setMode(int mode) {
        this.mode = mode;
        markDirty();
    }
    
    public int getNlink() {
        return nlink;
    }
    
    public void setNlink(int nlink) {
        this.nlink = nlink;
        markDirty();
    }
    
    public int getUid() {
        return uid;
    }
    
    public void setUid(int uid) {
        this.uid = uid;
        markDirty();
    }
    
    public int getGid() {
        return gid;
    }
    
    public void setGid(int gid) {
        this.gid = gid;
        markDirty();
    }
    
    public long getSize() {
        return size;
    }
    
    public void setSize(long size) {
        this.size = size;
        markDirty();
    }
    
    public long getAtime() {
        return atime;
    }
    
    public void setAtime(long atime) {
        this.atime = atime;
        markDirty();
    }
    
    public long getMtime() {
        return mtime;
    }
    
    public void setMtime(long mtime) {
        this.mtime = mtime;
        markDirty();
    }
    
    public long getCtime() {
        return ctime;
    }
    
    public void setCtime(long ctime) {
        this.ctime = ctime;
        markDirty();
    }
    
    public int[] getDirectBlocks() {
        return directBlocks;
    }
    
    public int getIndirectBlock() {
        return indirectBlock;
    }
    
    public void setIndirectBlock(int indirectBlock) {
        this.indirectBlock = indirectBlock;
        markDirty();
    }
    
    public int getDoubleIndirectBlock() {
        return doubleIndirectBlock;
    }
    
    public void setDoubleIndirectBlock(int doubleIndirectBlock) {
        this.doubleIndirectBlock = doubleIndirectBlock;
        markDirty();
    }
    
    public int getRefCount() {
        return refCount;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    public boolean isLoaded() {
        return loaded;
    }
    
    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
}
