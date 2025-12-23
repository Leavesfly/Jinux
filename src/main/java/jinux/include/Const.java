package jinux.include;

/**
 * Jinux 操作系统常量定义
 * 对应 Linux 0.01 中的 include/linux/xxxx.h 定义
 * 
 * @author Jinux Project
 */
public class Const {
    
    // ==================== 系统配置常量 ====================
    
    /** 最大进程数量 (Linux 0.01 为 64) */
    public static final int NR_TASKS = 64;
    
    /** 内核代码段选择符 */
    public static final int KERNEL_CS = 0x08;
    
    /** 内核数据段选择符 */
    public static final int KERNEL_DS = 0x10;
    
    /** 用户代码段选择符 */
    public static final int USER_CS = 0x1B;
    
    /** 用户数据段选择符 */
    public static final int USER_DS = 0x23;
    
    
    // ==================== 内存管理常量 ====================
    
    /** 页面大小（4KB） */
    public static final int PAGE_SIZE = 4096;
    
    /** 页面位移量（log2(PAGE_SIZE) = 12） */
    public static final int PAGE_SHIFT = 12;
    
    /** 物理内存总大小（16MB，Linux 0.01 最大支持） */
    public static final int MEMORY_SIZE = 16 * 1024 * 1024;
    
    /** 物理页面数量 */
    public static final int NR_PAGES = MEMORY_SIZE / PAGE_SIZE;
    
    /** 内核占用的内存（低端 1MB） */
    public static final int KERNEL_MEMORY = 1 * 1024 * 1024;
    
    /** 每个进程的虚拟地址空间大小（64MB） */
    public static final int TASK_SIZE = 64 * 1024 * 1024;
    
    
    // ==================== 文件系统常量 ====================
    
    /** 每个进程打开文件的最大数量 */
    public static final int NR_OPEN = 20;
    
    /** 系统级打开文件的最大数量 */
    public static final int NR_FILE = 64;
    
    /** inode 缓存数量 */
    public static final int NR_INODE = 32;
    
    /** 超级块缓存数量 */
    public static final int NR_SUPER = 8;
    
    /** 块大小（1KB，minix fs 标准） */
    public static final int BLOCK_SIZE = 1024;
    
    /** 块位移量 */
    public static final int BLOCK_SHIFT = 10;
    
    /** 块设备缓冲区数量 */
    public static final int NR_BUFFERS = 128;
    
    /** 根文件系统设备号 */
    public static final int ROOT_DEV = 0x301; // /dev/hda1
    
    
    // ==================== 进程状态常量 ====================
    
    /** 进程状态：就绪（可运行） */
    public static final int TASK_RUNNING = 0;
    
    /** 进程状态：可中断的睡眠 */
    public static final int TASK_INTERRUPTIBLE = 1;
    
    /** 进程状态：不可中断的睡眠 */
    public static final int TASK_UNINTERRUPTIBLE = 2;
    
    /** 进程状态：僵尸（已退出，等待父进程回收） */
    public static final int TASK_ZOMBIE = 3;
    
    /** 进程状态：停止 */
    public static final int TASK_STOPPED = 4;
    
    
    // ==================== 时钟和调度常量 ====================
    
    /** 时钟中断频率（HZ，每秒 100 次） */
    public static final int HZ = 100;
    
    /** 时钟中断间隔（毫秒） */
    public static final int TICK_MS = 1000 / HZ;
    
    /** 进程默认时间片（10 个 tick） */
    public static final int DEF_COUNTER = 10;
    
    /** 进程默认优先级 */
    public static final int DEF_PRIORITY = 15;
    
    
    // ==================== 文件类型和权限 ====================
    
    /** 文件类型掩码 */
    public static final int S_IFMT = 0170000;
    
    /** 普通文件 */
    public static final int S_IFREG = 0100000;
    
    /** 目录 */
    public static final int S_IFDIR = 0040000;
    
    /** 字符设备 */
    public static final int S_IFCHR = 0020000;
    
    /** 块设备 */
    public static final int S_IFBLK = 0060000;
    
    /** 所有者读权限 */
    public static final int S_IRUSR = 0000400;
    
    /** 所有者写权限 */
    public static final int S_IWUSR = 0000200;
    
    /** 所有者执行权限 */
    public static final int S_IXUSR = 0000100;
    
    
    // ==================== 文件打开标志 ====================
    
    /** 只读 */
    public static final int O_RDONLY = 0;
    
    /** 只写 */
    public static final int O_WRONLY = 1;
    
    /** 读写 */
    public static final int O_RDWR = 2;
    
    /** 创建文件 */
    public static final int O_CREAT = 0100;
    
    /** 独占创建 */
    public static final int O_EXCL = 0200;
    
    /** 截断文件 */
    public static final int O_TRUNC = 01000;
    
    /** 追加模式 */
    public static final int O_APPEND = 02000;
    
    
    // ==================== 错误码 ====================
    
    /** 成功 */
    public static final int E_OK = 0;
    
    /** 操作不允许 */
    public static final int EPERM = 1;
    
    /** 文件或目录不存在 */
    public static final int ENOENT = 2;
    
    /** 进程不存在 */
    public static final int ESRCH = 3;
    
    /** 中断的系统调用 */
    public static final int EINTR = 4;
    
    /** I/O 错误 */
    public static final int EIO = 5;
    
    /** 内存不足 */
    public static final int ENOMEM = 12;
    
    /** 访问被拒绝 */
    public static final int EACCES = 13;
    
    /** 地址错误 */
    public static final int EFAULT = 14;
    
    /** 设备忙 */
    public static final int EBUSY = 16;
    
    /** 文件已存在 */
    public static final int EEXIST = 17;
    
    /** 不是目录 */
    public static final int ENOTDIR = 20;
    
    /** 是目录 */
    public static final int EISDIR = 21;
    
    /** 参数无效 */
    public static final int EINVAL = 22;
    
    /** 打开文件过多 */
    public static final int EMFILE = 24;
    
    /** 管道破裂 */
    public static final int EPIPE = 32;
    
    /** 文件描述符错误 */
    public static final int EBADF = 9;
    
    
    // ==================== 辅助方法 ====================
    
    /**
     * 判断是否是目录
     */
    public static boolean S_ISDIR(int mode) {
        return (mode & S_IFMT) == S_IFDIR;
    }
    
    /**
     * 判断是否是普通文件
     */
    public static boolean S_ISREG(int mode) {
        return (mode & S_IFMT) == S_IFREG;
    }
    
    /**
     * 判断是否是字符设备
     */
    public static boolean S_ISCHR(int mode) {
        return (mode & S_IFMT) == S_IFCHR;
    }
    
    /**
     * 判断是否是块设备
     */
    public static boolean S_ISBLK(int mode) {
        return (mode & S_IFMT) == S_IFBLK;
    }
    
    /**
     * 虚拟地址转页号
     */
    public static int vaddr2page(long vaddr) {
        return (int) (vaddr >> PAGE_SHIFT);
    }
    
    /**
     * 页号转虚拟地址
     */
    public static long page2vaddr(int pageNo) {
        return ((long) pageNo) << PAGE_SHIFT;
    }
}
