package jinux.include;

/**
 * Jinux 操作系统文件系统常量定义
 * <p>
 * 包含文件系统相关的常量，如文件描述符限制、inode/超级块缓存数量、
 * 块设备参数、文件类型、权限标志、打开文件标志等。
 * 这些常量对应 Linux 0.01 中 include/linux/fs.h 的相关定义。
 * </p>
 *
 * @author Jinux Project
 */
public class FileSystemConstants {

    // ==================== 文件系统资源限制 ====================

    /**
     * 每个进程打开文件的最大数量
     */
    public static final int NR_OPEN = 20;

    /**
     * 系统级打开文件的最大数量
     */
    public static final int NR_FILE = 64;

    /**
     * inode 缓存数量
     */
    public static final int NR_INODE = 32;

    /**
     * 超级块缓存数量
     */
    public static final int NR_SUPER = 8;

    // ==================== 块设备参数 ====================

    /**
     * 块大小（1KB，minix fs 标准）
     */
    public static final int BLOCK_SIZE = 1024;

    /**
     * 块位移量（log2(BLOCK_SIZE) = 10）
     */
    public static final int BLOCK_SHIFT = 10;

    /**
     * 块设备缓冲区数量
     */
    public static final int NR_BUFFERS = 128;

    /**
     * 根文件系统设备号
     * <p>
     * 对应 /dev/hda1（第一个 IDE 硬盘的第一个分区）
     * </p>
     */
    public static final int ROOT_DEV = 0x301;

    // ==================== MINIX 文件系统特定常量 ====================

    /**
     * 目录项大小（字节）
     * <p>
     * MINIX v1 文件系统中，每个目录项占用 16 字节。
     * </p>
     */
    public static final int DIR_ENTRY_SIZE = 16;

    /**
     * MINIX 文件名最大长度
     * <p>
     * MINIX v1 文件系统中，文件名最多支持 14 个字符。
     * </p>
     */
    public static final int MAX_FILE_NAME_LENGTH = 14;

    /**
     * 磁盘上 inode 结构的大小（字节）
     * <p>
     * MINIX v1 文件系统中，磁盘上的 inode 结构占用 32 字节。
     * </p>
     */
    public static final int INODE_DISK_SIZE = 32;

    /**
     * MINIX v1 直接块数量
     * <p>
     * MINIX v1 文件系统中，inode 包含 7 个直接块指针。
     * </p>
     */
    public static final int MINIX_DIRECT_BLOCKS = 7;

    // ==================== 文件类型常量 ====================

    /**
     * 文件类型掩码
     * <p>
     * 用于从 mode 中提取文件类型部分。
     * </p>
     */
    public static final int S_IFMT = 0170000;

    /**
     * 普通文件
     */
    public static final int S_IFREG = 0100000;

    /**
     * 目录
     */
    public static final int S_IFDIR = 0040000;

    /**
     * 字符设备
     */
    public static final int S_IFCHR = 0020000;

    /**
     * 块设备
     */
    public static final int S_IFBLK = 0060000;

    // ==================== 文件权限常量 ====================

    /**
     * 所有者读权限
     */
    public static final int S_IRUSR = 0000400;

    /**
     * 所有者写权限
     */
    public static final int S_IWUSR = 0000200;

    /**
     * 所有者执行权限
     */
    public static final int S_IXUSR = 0000100;

    // ==================== 文件打开标志 ====================

    /**
     * 只读模式
     */
    public static final int O_RDONLY = 0;

    /**
     * 只写模式
     */
    public static final int O_WRONLY = 1;

    /**
     * 读写模式
     */
    public static final int O_RDWR = 2;

    /**
     * 如果文件不存在则创建
     */
    public static final int O_CREAT = 0100;

    /**
     * 独占创建：如果文件已存在则失败
     */
    public static final int O_EXCL = 0200;

    /**
     * 截断文件：如果文件存在则清空内容
     */
    public static final int O_TRUNC = 01000;

    /**
     * 追加模式：写入时定位到文件末尾
     */
    public static final int O_APPEND = 02000;

    // ==================== 辅助方法 ====================

    /**
     * 判断是否是目录
     *
     * @param mode 文件模式
     * @return 如果是目录返回 true
     */
    public static boolean S_ISDIR(int mode) {
        return (mode & S_IFMT) == S_IFDIR;
    }

    /**
     * 判断是否是普通文件
     *
     * @param mode 文件模式
     * @return 如果是普通文件返回 true
     */
    public static boolean S_ISREG(int mode) {
        return (mode & S_IFMT) == S_IFREG;
    }

    /**
     * 判断是否是字符设备
     *
     * @param mode 文件模式
     * @return 如果是字符设备返回 true
     */
    public static boolean S_ISCHR(int mode) {
        return (mode & S_IFMT) == S_IFCHR;
    }

    /**
     * 判断是否是块设备
     *
     * @param mode 文件模式
     * @return 如果是块设备返回 true
     */
    public static boolean S_ISBLK(int mode) {
        return (mode & S_IFMT) == S_IFBLK;
    }
}
