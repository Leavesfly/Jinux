package jinux.lib;

import jinux.kernel.SystemCallDispatcher;
import jinux.kernel.Scheduler;

/**
 * 用户态 C 库封装（Facade 门面类）
 * 对应 Linux 0.01 中的 lib/ 目录功能
 * 
 * 提供对系统调用的高级封装，隐藏底层 syscall 细节。
 * 此类作为门面，将所有调用委托给对应的子模块。
 * 
 * @author Jinux Project
 */
public class LibC {
    
    /** 进程管理子模块 */
    private final ProcessLib processLib;
    
    /** 文件操作子模块 */
    private final FileLib fileLib;
    
    /** 内存管理子模块 */
    private final MemoryLib memoryLib;
    
    /** 信号管理子模块 */
    private final SignalLib signalLib;
    
    /** 进程间通信子模块 */
    private final IpcLib ipcLib;
    
    /** 时间管理子模块 */
    private final TimeLib timeLib;
    
    /**
     * 构造用户态库
     * 
     * @param syscallDispatcher 系统调用分发器
     * @param scheduler 调度器（用于获取当前进程）
     */
    public LibC(SystemCallDispatcher syscallDispatcher, Scheduler scheduler) {
        UserSpaceBufferManager bufferManager = new UserSpaceBufferManager(scheduler);
        this.processLib = new ProcessLib(syscallDispatcher, scheduler, bufferManager);
        this.fileLib = new FileLib(syscallDispatcher, scheduler, bufferManager);
        this.memoryLib = new MemoryLib(syscallDispatcher);
        this.signalLib = new SignalLib(syscallDispatcher);
        this.ipcLib = new IpcLib(syscallDispatcher, scheduler, bufferManager);
        this.timeLib = new TimeLib(syscallDispatcher);
    }
    
    /**
     * 兼容旧构造函数
     */
    public LibC(SystemCallDispatcher syscallDispatcher) {
        this(syscallDispatcher, null);
    }
    
    // ==================== 进程管理系统调用 ====================
    
    /**
     * 获取当前进程 ID
     * 
     * @return 进程 ID
     */
    public int getpid() {
        return processLib.getpid();
    }
    
    /**
     * 获取父进程 ID
     * 
     * @return 父进程 ID
     */
    public int getppid() {
        return processLib.getppid();
    }
    
    /**
     * 创建子进程
     * 
     * @return 父进程返回子进程 PID，子进程返回 0，失败返回 -1
     */
    public int fork() {
        return processLib.fork();
    }
    
    /**
     * 退出进程
     * 
     * @param status 退出状态码
     */
    public void exit(int status) {
        processLib.exit(status);
    }
    
    /**
     * 等待子进程结束
     * 
     * @return 已结束的子进程 PID，失败返回 -1
     */
    public int waitpid() {
        return processLib.waitpid();
    }
    
    /**
     * 暂停进程执行
     */
    public void pause() {
        processLib.pause();
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
        return processLib.execve(path, argv, envp);
    }
    
    /**
     * 执行新程序（简化版本，只传程序名）
     * 
     * @param path 程序路径/名称
     * @return 成功不返回，失败返回 -1
     */
    public int exec(String path) {
        return processLib.exec(path);
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
        return fileLib.open(pathname, flags);
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
        return fileLib.open(pathname, flags, mode);
    }
    
    /**
     * 关闭文件
     * 
     * @param fd 文件描述符
     * @return 成功返回 0，失败返回 -1
     */
    public int close(int fd) {
        return fileLib.close(fd);
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
        return fileLib.read(fd, buf, count);
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
        return fileLib.write(fd, buf, count);
    }
    
    /**
     * 写入字符串到标准输出
     * 
     * @param str 字符串
     * @return 实际写入的字节数
     */
    public int print(String str) {
        return fileLib.print(str);
    }
    
    /**
     * 写入字符串并换行到标准输出
     * 
     * @param str 字符串
     * @return 实际写入的字节数
     */
    public int println(String str) {
        return fileLib.println(str);
    }
    
    /**
     * 创建文件
     * 
     * @param pathname 文件路径
     * @param mode 权限模式
     * @return 文件描述符，失败返回 -1
     */
    public int creat(String pathname, int mode) {
        return fileLib.creat(pathname, mode);
    }
    
    /**
     * 删除文件
     * 
     * @param pathname 文件路径
     * @return 成功返回 0，失败返回 -1
     */
    public int unlink(String pathname) {
        return fileLib.unlink(pathname);
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
        return fileLib.lseek(fd, offset, whence);
    }
    
    // ==================== 目录操作系统调用 ====================
    
    /**
     * 改变当前工作目录
     * 
     * @param path 目录路径
     * @return 成功返回 0，失败返回 -1
     */
    public int chdir(String path) {
        return fileLib.chdir(path);
    }
    
    /**
     * 创建目录
     * 
     * @param pathname 目录路径
     * @param mode 权限模式
     * @return 成功返回 0，失败返回 -1
     */
    public int mkdir(String pathname, int mode) {
        return fileLib.mkdir(pathname, mode);
    }
    
    /**
     * 删除目录
     * 
     * @param pathname 目录路径
     * @return 成功返回 0，失败返回 -1
     */
    public int rmdir(String pathname) {
        return fileLib.rmdir(pathname);
    }
    
    // ==================== 内存管理系统调用 ====================
    
    /**
     * 设置堆结束地址
     * 
     * @param addr 新的堆结束地址
     * @return 实际的堆结束地址
     */
    public long brk(long addr) {
        return memoryLib.brk(addr);
    }
    
    /**
     * 简化的 malloc 实现（基于 brk）
     * 注意：这是一个简化版本，真实的 malloc 更复杂
     * 
     * @param size 分配大小
     * @return 分配的地址（简化返回 long）
     */
    public long malloc(long size) {
        return memoryLib.malloc(size);
    }
    
    // ==================== 时间相关系统调用 ====================
    
    /**
     * 获取系统时间
     * 
     * @return Unix 时间戳（秒）
     */
    public long time() {
        return timeLib.time();
    }
    
    // ==================== 其他系统调用 ====================
    
    /**
     * 同步磁盘缓冲
     */
    public void sync() {
        fileLib.sync();
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
        return signalLib.signal(signum, handler);
    }
    
    /**
     * 发送信号到进程
     * 
     * @param pid 目标进程 PID
     * @param signum 信号编号
     * @return 0 成功，-1 失败
     */
    public int kill(int pid, int signum) {
        return signalLib.kill(pid, signum);
    }
    
    // ==================== 进程间通信 ====================
    
    /**
     * 创建管道
     * 
     * @param fds 文件描述符数组，fds[0] 为读端，fds[1] 为写端
     * @return 0 成功，-1 失败
     */
    public int pipe(int[] fds) {
        return ipcLib.pipe(fds);
    }
    
    // ==================== 标准文件描述符常量 ====================
    
    /** 标准输入 */
    public static final int STDIN_FILENO = 0;
    
    /** 标准输出 */
    public static final int STDOUT_FILENO = 1;
    
    /** 标准错误输出 */
    public static final int STDERR_FILENO = 2;
}
