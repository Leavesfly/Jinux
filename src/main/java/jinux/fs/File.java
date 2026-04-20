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
     * 
     * @param buf 目标缓冲区
     * @param count 要读取的字节数
     * @return 实际读取的字节数，-1 表示错误
     */
    public int read(byte[] buf, int count) {
        if (inode == null) {
            return -9; // EBADF
        }
        
        if (buf == null || count <= 0) {
            return -22; // EINVAL
        }
        
        // 检查读权限：O_RDONLY=0 表示只读，O_WRONLY=1 表示只写
        // 只有 O_WRONLY 模式才不允许读取
        int accessMode = mode & 3; // 取低 2 位
        if (accessMode == O_WRONLY) {
            return -13; // EACCES
        }
        
        // 计算实际可读取的字节数
        long remaining = inode.getSize() - position;
        if (remaining <= 0) {
            return 0; // EOF
        }
        
        int toRead = (int) Math.min(count, remaining);
        if (toRead > buf.length) {
            toRead = buf.length;
        }
        
        // 注意：File 本身不持有 VFS 引用，实际的文件数据读取
        // 由 FileSyscalls.sysRead() 通过 VFS.readFileData() 完成。
        // 此方法仅用于管道等特殊文件的读取（通过子类重写）。
        // 对于普通文件，FileSyscalls 会直接调用 VFS.readFileData 而非此方法。
        
        // 更新位置和访问时间
        position += toRead;
        inode.setAtime(System.currentTimeMillis() / 1000);
        
        return toRead;
    }
    
    /**
     * 写入数据
     * 
     * @param buf 源缓冲区
     * @param count 要写入的字节数
     * @return 实际写入的字节数，-1 表示错误
     */
    public int write(byte[] buf, int count) {
        if (inode == null) {
            return -9; // EBADF
        }
        
        if (buf == null || count <= 0) {
            return -22; // EINVAL
        }
        
        // 检查写权限：O_RDONLY=0 表示只读，不允许写入
        int accessMode = mode & 3; // 取低 2 位
        if (accessMode == O_RDONLY) {
            return -13; // EACCES
        }
        
        int toWrite = Math.min(count, buf.length);
        
        // 如果是追加模式，移动到文件末尾
        if ((mode & O_APPEND) != 0) {
            position = inode.getSize();
        }
        
        // 注意：File 本身不持有 VFS 引用，实际的文件数据写入
        // 由 FileSyscalls.sysWrite() 通过 VFS.writeFileData() 完成。
        // 此方法仅用于管道等特殊文件的写入（通过子类重写）。
        
        // 更新文件大小和位置
        long newSize = position + toWrite;
        if (newSize > inode.getSize()) {
            inode.setSize(newSize);
        }
        position += toWrite;
        
        // 更新修改时间
        inode.setMtime(System.currentTimeMillis() / 1000);
        inode.markDirty();
        
        return toWrite;
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
