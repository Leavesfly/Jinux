package jinux.lib;

import jinux.kernel.SystemCallDispatcher;
import jinux.kernel.Task;
import jinux.include.Syscalls;

/**
 * 用户态 C 库封装
 * 对应 Linux 0.01 中的 lib/ 目录功能
 * 
 * 提供对系统调用的高级封装，隐藏底层 syscall 细节
 * 
 * @author Jinux Project
 */
public class LibC {
    
    private final SystemCallDispatcher syscallDispatcher;
    
    /**
     * 构造用户态库
     * 
     * @param syscallDispatcher 系统调用分发器
     */
    public LibC(SystemCallDispatcher syscallDispatcher) {
        this.syscallDispatcher = syscallDispatcher;
    }
    
    // ==================== 进程管理系统调用 ====================
    
    /**
     * 获取当前进程 ID
     * 
     * @return 进程 ID
     */
    public int getpid() {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_GETPID, 0, 0, 0);
    }
    
    /**
     * 获取父进程 ID
     * 
     * @return 父进程 ID
     */
    public int getppid() {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_GETPPID, 0, 0, 0);
    }
    
    /**
     * 创建子进程
     * 
     * @return 父进程返回子进程 PID，子进程返回 0，失败返回 -1
     */
    public int fork() {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_FORK, 0, 0, 0);
    }
    
    /**
     * 退出进程
     * 
     * @param status 退出状态码
     */
    public void exit(int status) {
        syscallDispatcher.dispatch(Syscalls.SYS_EXIT, status, 0, 0);
    }
    
    /**
     * 等待子进程结束
     * 
     * @return 已结束的子进程 PID，失败返回 -1
     */
    public int waitpid() {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_WAIT, 0, 0, 0);
    }
    
    /**
     * 暂停进程执行
     */
    public void pause() {
        syscallDispatcher.dispatch(Syscalls.SYS_PAUSE, 0, 0, 0);
    }
    
    /**
     * 执行新程序
     * 
     * @param path 程序路径/名称
     * @param argv 参数数组
     * @param envp 环境变量数组
     * @return 成功不返回，失败返回 -1
     */
    public int execve(String path, String[] argv, String[] envp) {
        // 简化：直接传递程序名的 hashCode
        // 实际实现中需要将字符串写入用户空间并传递指针
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_EXECVE, 
            path.hashCode(), 0, 0);
    }
    
    /**
     * 执行新程序（简化版本，只传程序名）
     * 
     * @param path 程序路径/名称
     * @return 成功不返回，失败返回 -1
     */
    public int exec(String path) {
        return execve(path, null, null);
    }
    
    // ==================== 文件操作系统调用 ====================
    
    /**
     * 打开文件
     * 
     * @param pathname 文件路径
     * @param flags 打开标志
     * @return 文件描述符，失败返回 -1
     */
    public int open(String pathname, int flags) {
        return open(pathname, flags, 0);
    }
    
    /**
     * 打开文件（带权限）
     * 
     * @param pathname 文件路径
     * @param flags 打开标志
     * @param mode 权限模式
     * @return 文件描述符，失败返回 -1
     */
    public int open(String pathname, int flags, int mode) {
        // 简化：传递路径字符串的 hashCode 作为标识
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_OPEN, 
            pathname.hashCode(), flags, mode);
    }
    
    /**
     * 关闭文件
     * 
     * @param fd 文件描述符
     * @return 成功返回 0，失败返回 -1
     */
    public int close(int fd) {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_CLOSE, fd, 0, 0);
    }
    
    /**
     * 读取文件
     * 
     * @param fd 文件描述符
     * @param buf 缓冲区
     * @param count 读取字节数
     * @return 实际读取的字节数，失败返回 -1
     */
    public int read(int fd, byte[] buf, int count) {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_READ, fd, 0, count);
    }
    
    /**
     * 写入文件
     * 
     * @param fd 文件描述符
     * @param buf 缓冲区
     * @param count 写入字节数
     * @return 实际写入的字节数，失败返回 -1
     */
    public int write(int fd, byte[] buf, int count) {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_WRITE, fd, 0, count);
    }
    
    /**
     * 写入字符串到标准输出
     * 
     * @param str 字符串
     * @return 实际写入的字节数
     */
    public int print(String str) {
        byte[] bytes = str.getBytes();
        return write(1, bytes, bytes.length);
    }
    
    /**
     * 写入字符串并换行到标准输出
     * 
     * @param str 字符串
     * @return 实际写入的字节数
     */
    public int println(String str) {
        return print(str + "\n");
    }
    
    /**
     * 创建文件
     * 
     * @param pathname 文件路径
     * @param mode 权限模式
     * @return 文件描述符，失败返回 -1
     */
    public int creat(String pathname, int mode) {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_CREAT, 
            pathname.hashCode(), mode, 0);
    }
    
    /**
     * 删除文件
     * 
     * @param pathname 文件路径
     * @return 成功返回 0，失败返回 -1
     */
    public int unlink(String pathname) {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_UNLINK, 
            pathname.hashCode(), 0, 0);
    }
    
    /**
     * 定位文件读写位置
     * 
     * @param fd 文件描述符
     * @param offset 偏移量
     * @param whence 起始位置
     * @return 新的文件位置，失败返回 -1
     */
    public long lseek(int fd, long offset, int whence) {
        return syscallDispatcher.dispatch(Syscalls.SYS_LSEEK, fd, offset, whence);
    }
    
    // ==================== 目录操作系统调用 ====================
    
    /**
     * 改变当前工作目录
     * 
     * @param path 目录路径
     * @return 成功返回 0，失败返回 -1
     */
    public int chdir(String path) {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_CHDIR, 
            path.hashCode(), 0, 0);
    }
    
    /**
     * 创建目录
     * 
     * @param pathname 目录路径
     * @param mode 权限模式
     * @return 成功返回 0，失败返回 -1
     */
    public int mkdir(String pathname, int mode) {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_MKDIR, 
            pathname.hashCode(), mode, 0);
    }
    
    /**
     * 删除目录
     * 
     * @param pathname 目录路径
     * @return 成功返回 0，失败返回 -1
     */
    public int rmdir(String pathname) {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_RMDIR, 
            pathname.hashCode(), 0, 0);
    }
    
    // ==================== 内存管理系统调用 ====================
    
    /**
     * 设置堆结束地址
     * 
     * @param addr 新的堆结束地址
     * @return 实际的堆结束地址
     */
    public long brk(long addr) {
        return syscallDispatcher.dispatch(Syscalls.SYS_BRK, addr, 0, 0);
    }
    
    /**
     * 简化的 malloc 实现（基于 brk）
     * 注意：这是一个简化版本，真实的 malloc 更复杂
     * 
     * @param size 分配大小
     * @return 分配的地址（简化返回 long）
     */
    public long malloc(long size) {
        // 简化：直接扩展 brk
        long currentBrk = brk(0); // 获取当前 brk
        long newBrk = brk(currentBrk + size);
        if (newBrk >= currentBrk + size) {
            return currentBrk;
        }
        return 0; // 分配失败
    }
    
    // ==================== 时间相关系统调用 ====================
    
    /**
     * 获取系统时间
     * 
     * @return Unix 时间戳（秒）
     */
    public long time() {
        return syscallDispatcher.dispatch(Syscalls.SYS_TIME, 0, 0, 0);
    }
    
    // ==================== 其他系统调用 ====================
    
    /**
     * 同步磁盘缓冲
     */
    public void sync() {
        syscallDispatcher.dispatch(Syscalls.SYS_SYNC, 0, 0, 0);
    }
    
    // ==================== 信号相关系统调用 ====================
    
    /**
     * 设置信号处理器
     * 
     * @param signum 信号编号
     * @param handler 处理器（SIG_DFL、SIG_IGN 或自定义）
     * @return 旧的处理器
     */
    public long signal(int signum, long handler) {
        return syscallDispatcher.dispatch(Syscalls.SYS_SIGNAL, signum, handler, 0);
    }
    
    /**
     * 发送信号到进程
     * 
     * @param pid 目标进程 PID
     * @param signum 信号编号
     * @return 0 成功，-1 失败
     */
    public int kill(int pid, int signum) {
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_KILL, pid, signum, 0);
    }
    
    // ==================== 进程间通信 ====================
    
    /**
     * 创建管道
     * 
     * @param fds 文件描述符数组，fds[0] 为读端，fds[1] 为写端
     * @return 0 成功，-1 失败
     */
    public int pipe(int[] fds) {
        // 简化：直接调用，实际应通过内存传递 fd 数组
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_PIPE, 0, 0, 0);
    }
    
    // ==================== 标准文件描述符常量 ====================
    
    /** 标准输入 */
    public static final int STDIN_FILENO = 0;
    
    /** 标准输出 */
    public static final int STDOUT_FILENO = 1;
    
    /** 标准错误输出 */
    public static final int STDERR_FILENO = 2;
}
