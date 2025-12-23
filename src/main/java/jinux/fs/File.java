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
        
        // 检查读权限
        if ((mode & O_RDONLY) == 0 && (mode & O_RDWR) == 0) {
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
        
        // 从 VFS 读取文件数据（需要 VFS 引用）
        // 注意：这里简化处理，实际应该通过 VFS 读取
        // 由于 File 类没有 VFS 引用，我们使用 inode 的数据块直接读取
        // 这是一个简化实现，完整实现需要 File 持有 VFS 引用
        
        // 简化：如果文件有数据块，尝试读取
        int[] directBlocks = inode.getDirectBlocks();
        if (directBlocks[0] != 0 && position < inode.getSize()) {
            // 有数据块，但需要 VFS 来读取
            // 这里先返回 0，表示需要外部调用 VFS.readFileData
            // 实际使用时，应该在 SystemCallDispatcher 中调用 VFS.readFileData
        }
        
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
        
        // 检查写权限
        if ((mode & O_WRONLY) == 0 && (mode & O_RDWR) == 0) {
            return -13; // EACCES
        }
        
        int toWrite = Math.min(count, buf.length);
        
        // 如果是追加模式，移动到文件末尾
        if ((mode & O_APPEND) != 0) {
            position = inode.getSize();
        }
        
        // 简化实现：写入到inode的数据块
        // 实际实现需要分配数据块并写入磁盘
        // 这里先返回写入的字节数，表示需要VFS支持
        // TODO: 需要VFS提供writeInodeData方法
        
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
