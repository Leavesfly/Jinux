package jinux.include;

/**
 * 系统调用号定义
 * 对应 Linux 0.01 中的 include/unistd.h
 * 
 * @author Jinux Project
 */
public class Syscalls {
    
    // ==================== 进程管理相关系统调用 ====================
    
    /** fork - 创建子进程 */
    public static final int SYS_FORK = 2;
    
    /** execve - 执行程序 */
    public static final int SYS_EXECVE = 11;
    
    /** exit - 退出进程 */
    public static final int SYS_EXIT = 1;
    
    /** wait - 等待子进程 */
    public static final int SYS_WAIT = 7;
    
    /** getpid - 获取进程 ID */
    public static final int SYS_GETPID = 20;
    
    /** getppid - 获取父进程 ID */
    public static final int SYS_GETPPID = 64;
    
    /** pause - 暂停进程 */
    public static final int SYS_PAUSE = 29;
    
    /** signal - 设置信号处理 */
    public static final int SYS_SIGNAL = 48;
    
    /** kill - 发送信号 */
    public static final int SYS_KILL = 37;
    
    /** pipe - 创建管道 */
    public static final int SYS_PIPE = 42;
    
    
    // ==================== 文件操作相关系统调用 ====================
    
    /** open - 打开文件 */
    public static final int SYS_OPEN = 5;
    
    /** close - 关闭文件 */
    public static final int SYS_CLOSE = 6;
    
    /** read - 读取文件 */
    public static final int SYS_READ = 3;
    
    /** write - 写入文件 */
    public static final int SYS_WRITE = 4;
    
    /** lseek - 定位文件 */
    public static final int SYS_LSEEK = 19;
    
    /** creat - 创建文件 */
    public static final int SYS_CREAT = 8;
    
    /** unlink - 删除文件 */
    public static final int SYS_UNLINK = 10;
    
    /** stat - 获取文件状态 */
    public static final int SYS_STAT = 18;
    
    /** fstat - 获取文件描述符状态 */
    public static final int SYS_FSTAT = 28;
    
    
    // ==================== 目录操作相关系统调用 ====================
    
    /** chdir - 改变当前目录 */
    public static final int SYS_CHDIR = 12;
    
    /** mkdir - 创建目录 */
    public static final int SYS_MKDIR = 39;
    
    /** rmdir - 删除目录 */
    public static final int SYS_RMDIR = 40;
    
    
    // ==================== 内存管理相关系统调用 ====================
    
    /** brk - 设置数据段结束位置 */
    public static final int SYS_BRK = 45;
    
    
    // ==================== 时间相关系统调用 ====================
    
    /** time - 获取系统时间 */
    public static final int SYS_TIME = 13;
    
    /** times - 获取进程时间 */
    public static final int SYS_TIMES = 43;
    
    
    // ==================== 其他系统调用 ====================
    
    /** sync - 同步磁盘 */
    public static final int SYS_SYNC = 36;
    
    /** ioctl - 设备控制 */
    public static final int SYS_IOCTL = 54;
    
    /** dup - 复制文件描述符 */
    public static final int SYS_DUP = 41;
    
    /** dup2 - 复制文件描述符到指定编号 */
    public static final int SYS_DUP2 = 63;
    
    
    /**
     * 系统调用名称映射（用于调试）
     */
    public static String getSyscallName(int nr) {
        switch (nr) {
            case SYS_EXIT: return "exit";
            case SYS_FORK: return "fork";
            case SYS_READ: return "read";
            case SYS_WRITE: return "write";
            case SYS_OPEN: return "open";
            case SYS_CLOSE: return "close";
            case SYS_WAIT: return "wait";
            case SYS_CREAT: return "creat";
            case SYS_UNLINK: return "unlink";
            case SYS_EXECVE: return "execve";
            case SYS_CHDIR: return "chdir";
            case SYS_TIME: return "time";
            case SYS_STAT: return "stat";
            case SYS_LSEEK: return "lseek";
            case SYS_GETPID: return "getpid";
            case SYS_FSTAT: return "fstat";
            case SYS_PAUSE: return "pause";
            case SYS_MKDIR: return "mkdir";
            case SYS_RMDIR: return "rmdir";
            case SYS_TIMES: return "times";
            case SYS_BRK: return "brk";
            case SYS_SIGNAL: return "signal";
            case SYS_IOCTL: return "ioctl";
            case SYS_GETPPID: return "getppid";
            case SYS_SYNC: return "sync";
            case SYS_DUP: return "dup";
            case SYS_DUP2: return "dup2";
            default: return "unknown(" + nr + ")";
        }
    }
}
