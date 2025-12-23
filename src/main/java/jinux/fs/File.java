package jinux.fs;

/**
 * 文件对象
 * 对应 Linux 0.01 中的 struct file
 * 
 * 表示一个打开的文件
 * 
 * @author Jinux Project
 */
public class File {
    
    /** 文件打开模式 */
    private int mode;
    
    /** 文件位置指针 */
    private long position;
    
    /** 对应的 inode */
    private Inode inode;
    
    /** 引用计数 */
    private int refCount;
    
    // ==================== 打开模式常量 ====================
    
    /** 只读 */
    public static final int O_RDONLY = 0;
    
    /** 只写 */
    public static final int O_WRONLY = 1;
    
    /** 读写 */
    public static final int O_RDWR = 2;
    
    /** 创建 */
    public static final int O_CREAT = 0100;
    
    /** 截断 */
    public static final int O_TRUNC = 01000;
    
    /** 追加 */
    public static final int O_APPEND = 02000;
    
    /**
     * 构造文件对象
     */
    public File(Inode inode, int mode) {
        this.inode = inode;
        this.mode = mode;
        this.position = 0;
        this.refCount = 1;
        
        if (inode != null) {
            inode.incrementRef();
        }
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
        
        // 引用计数为 0 时释放 inode 引用
        if (refCount == 0 && inode != null) {
            inode.decrementRef();
        }
    }
    
    /**
     * 读取数据
     */
    public int read(byte[] buf, int count) {
        // TODO: 实现实际读取
        return 0;
    }
    
    /**
     * 写入数据
     */
    public int write(byte[] buf, int count) {
        // TODO: 实现实际写入
        return 0;
    }
    
    /**
     * 设置文件位置
     */
    public long lseek(long offset, int whence) {
        switch (whence) {
            case 0: // SEEK_SET
                position = offset;
                break;
            case 1: // SEEK_CUR
                position += offset;
                break;
            case 2: // SEEK_END
                if (inode != null) {
                    position = inode.getSize() + offset;
                }
                break;
        }
        return position;
    }
    
    @Override
    public String toString() {
        return String.format("File[mode=%d, pos=%d, inode=%s, ref=%d]",
            mode, position, inode != null ? inode.getIno() : -1, refCount);
    }
    
    // ==================== Getters and Setters ====================
    
    public int getMode() {
        return mode;
    }
    
    public long getPosition() {
        return position;
    }
    
    public void setPosition(long position) {
        this.position = position;
    }
    
    public Inode getInode() {
        return inode;
    }
    
    public int getRefCount() {
        return refCount;
    }
}
