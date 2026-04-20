package jinux.lib;

import jinux.kernel.SystemCallDispatcher;
import jinux.kernel.Task;
import jinux.kernel.Scheduler;
import jinux.include.Syscalls;

/**
 * 用户态 C 库封装
 * 对应 Linux 0.01 中的 lib/ 目录功能
 * 
 * 提供对系统调用的高级封装，隐藏底层 syscall 细节。
 * 字符串参数通过写入进程用户空间内存后传递虚拟地址指针给系统调用。
 * 
 * @author Jinux Project
 */
public class LibC {
    
    private final SystemCallDispatcher syscallDispatcher;
    private final Scheduler scheduler;
    
    /** 用户空间临时缓冲区的基地址（位于栈下方的保留区域） */
    private static final long USER_BUF_BASE = 0x03F00000L; // 63MB 处
    
    /** 用户空间临时缓冲区大小 */
    private static final int USER_BUF_SIZE = 0x10000; // 64KB
    
    /** 当前缓冲区偏移（每次系统调用前重置） */
    private long userBufOffset;
    
    /**
     * 构造用户态库
     * 
     * @param syscallDispatcher 系统调用分发器
     * @param scheduler 调度器（用于获取当前进程）
     */
    public LibC(SystemCallDispatcher syscallDispatcher, Scheduler scheduler) {
        this.syscallDispatcher = syscallDispatcher;
        this.scheduler = scheduler;
        this.userBufOffset = 0;
    }
    
    /**
     * 兼容旧构造函数
     */
    public LibC(SystemCallDispatcher syscallDispatcher) {
        this(syscallDispatcher, null);
    }
    
    /**
     * 将字符串写入当前进程的用户空间内存，返回虚拟地址指针。
     * 字符串以 null 结尾（C 风格）。
     * 
     * @param str 要写入的字符串
     * @return 用户空间虚拟地址，失败返回 0
     */
    private long writeStringToUserSpace(String str) {
        if (str == null || scheduler == null) {
            return 0;
        }
        
        Task currentTask = scheduler.getCurrentTask();
        if (currentTask == null) {
            return 0;
        }
        
        try {
            byte[] bytes = str.getBytes("UTF-8");
            int totalLen = bytes.length + 1; // +1 for null terminator
            
            if (userBufOffset + totalLen > USER_BUF_SIZE) {
                return 0; // 缓冲区空间不足
            }
            
            long vaddr = USER_BUF_BASE + userBufOffset;
            
            // 确保内存页已映射
            currentTask.getAddressSpace().allocateAndMap(vaddr, 7); // PRESENT|RW|USER
            if (totalLen > 4096) {
                // 跨页时映射下一页
                currentTask.getAddressSpace().allocateAndMap(vaddr + 4096, 7);
            }
            
            // 写入字符串内容
            currentTask.getAddressSpace().writeBytes(vaddr, bytes, 0, bytes.length);
            // 写入 null 终止符
            currentTask.getAddressSpace().writeByte(vaddr + bytes.length, (byte) 0);
            
            userBufOffset += totalLen;
            // 对齐到 8 字节边界
            userBufOffset = (userBufOffset + 7) & ~7;
            
            return vaddr;
        } catch (Exception e) {
            System.err.println("[LibC] Failed to write string to user space: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 将字符串数组写入用户空间内存，构建 C 风格的 char** 指针数组。
     * 数组以 NULL 指针结尾。
     * 
     * @param array 字符串数组
     * @return 用户空间指针数组的虚拟地址，失败或 array 为 null 时返回 0
     */
    private long writeStringArrayToUserSpace(String[] array) {
        if (array == null || array.length == 0 || scheduler == null) {
            return 0;
        }
        
        Task currentTask = scheduler.getCurrentTask();
        if (currentTask == null) {
            return 0;
        }
        
        try {
            // 先写入所有字符串，收集各字符串的虚拟地址
            long[] stringAddrs = new long[array.length];
            for (int i = 0; i < array.length; i++) {
                stringAddrs[i] = writeStringToUserSpace(array[i]);
                if (stringAddrs[i] == 0) {
                    return 0;
                }
            }
            
            // 构建指针数组（每个指针 8 字节，末尾 NULL）
            int pointerArraySize = (array.length + 1) * 8;
            if (userBufOffset + pointerArraySize > USER_BUF_SIZE) {
                return 0;
            }
            
            long arrayAddr = USER_BUF_BASE + userBufOffset;
            currentTask.getAddressSpace().allocateAndMap(arrayAddr, 7);
            if (pointerArraySize > 4096) {
                currentTask.getAddressSpace().allocateAndMap(arrayAddr + 4096, 7);
            }
            
            // 写入各字符串指针（小端序）
            for (int i = 0; i < array.length; i++) {
                byte[] ptrBytes = longToLittleEndianBytes(stringAddrs[i]);
                currentTask.getAddressSpace().writeBytes(arrayAddr + i * 8, ptrBytes, 0, 8);
            }
            // 写入末尾 NULL 指针
            byte[] nullPtr = new byte[8];
            currentTask.getAddressSpace().writeBytes(arrayAddr + array.length * 8, nullPtr, 0, 8);
            
            userBufOffset += pointerArraySize;
            userBufOffset = (userBufOffset + 7) & ~7;
            
            return arrayAddr;
        } catch (Exception e) {
            System.err.println("[LibC] Failed to write string array to user space: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 将 long 值转换为小端序字节数组
     */
    private byte[] longToLittleEndianBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (value >>> (i * 8));
        }
        return bytes;
    }
    
    /**
     * 重置用户空间临时缓冲区（每次系统调用前调用）
     */
    private void resetUserBuffer() {
        userBufOffset = 0;
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
        resetUserBuffer();
        long pathPtr = writeStringToUserSpace(path);
        if (pathPtr == 0) {
            return -1;
        }
        
        long argvPtr = writeStringArrayToUserSpace(argv);
        long envpPtr = writeStringArrayToUserSpace(envp);
        
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_EXECVE, pathPtr, argvPtr, envpPtr);
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
        resetUserBuffer();
        long pathPtr = writeStringToUserSpace(pathname);
        if (pathPtr == 0) {
            return -1;
        }
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_OPEN, pathPtr, flags, mode);
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
        resetUserBuffer();
        int readCount = Math.min(count, buf != null ? buf.length : 0);
        
        // 分配用户空间缓冲区用于接收数据
        long bufPtr = USER_BUF_BASE + userBufOffset;
        Task currentTask = scheduler != null ? scheduler.getCurrentTask() : null;
        if (currentTask != null && readCount > 0) {
            currentTask.getAddressSpace().allocateAndMap(bufPtr, 7);
            for (int page = 4096; page < readCount; page += 4096) {
                currentTask.getAddressSpace().allocateAndMap(bufPtr + page, 7);
            }
        }
        
        int result = (int) syscallDispatcher.dispatch(Syscalls.SYS_READ, fd, bufPtr, readCount);
        
        // 将数据从用户空间拷贝回 Java 缓冲区
        if (result > 0 && currentTask != null && buf != null) {
            currentTask.getAddressSpace().readBytes(bufPtr, buf, 0, result);
        }
        
        return result;
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
        resetUserBuffer();
        int writeCount = Math.min(count, buf != null ? buf.length : 0);
        
        // 将数据写入用户空间缓冲区
        long bufPtr = USER_BUF_BASE + userBufOffset;
        Task currentTask = scheduler != null ? scheduler.getCurrentTask() : null;
        if (currentTask != null && writeCount > 0 && buf != null) {
            currentTask.getAddressSpace().allocateAndMap(bufPtr, 7);
            for (int page = 4096; page < writeCount; page += 4096) {
                currentTask.getAddressSpace().allocateAndMap(bufPtr + page, 7);
            }
            currentTask.getAddressSpace().writeBytes(bufPtr, buf, 0, writeCount);
        }
        
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_WRITE, fd, bufPtr, writeCount);
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
        resetUserBuffer();
        long pathPtr = writeStringToUserSpace(pathname);
        if (pathPtr == 0) {
            return -1;
        }
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_CREAT, pathPtr, mode, 0);
    }
    
    /**
     * 删除文件
     * 
     * @param pathname 文件路径
     * @return 成功返回 0，失败返回 -1
     */
    public int unlink(String pathname) {
        resetUserBuffer();
        long pathPtr = writeStringToUserSpace(pathname);
        if (pathPtr == 0) {
            return -1;
        }
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_UNLINK, pathPtr, 0, 0);
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
        resetUserBuffer();
        long pathPtr = writeStringToUserSpace(path);
        if (pathPtr == 0) {
            return -1;
        }
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_CHDIR, pathPtr, 0, 0);
    }
    
    /**
     * 创建目录
     * 
     * @param pathname 目录路径
     * @param mode 权限模式
     * @return 成功返回 0，失败返回 -1
     */
    public int mkdir(String pathname, int mode) {
        resetUserBuffer();
        long pathPtr = writeStringToUserSpace(pathname);
        if (pathPtr == 0) {
            return -1;
        }
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_MKDIR, pathPtr, mode, 0);
    }
    
    /**
     * 删除目录
     * 
     * @param pathname 目录路径
     * @return 成功返回 0，失败返回 -1
     */
    public int rmdir(String pathname) {
        resetUserBuffer();
        long pathPtr = writeStringToUserSpace(pathname);
        if (pathPtr == 0) {
            return -1;
        }
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_RMDIR, pathPtr, 0, 0);
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
        resetUserBuffer();
        
        // 分配用户空间缓冲区存放 2 个 int（8 字节）
        long fdArrayPtr = USER_BUF_BASE + userBufOffset;
        Task currentTask = scheduler != null ? scheduler.getCurrentTask() : null;
        if (currentTask != null) {
            currentTask.getAddressSpace().allocateAndMap(fdArrayPtr, 7);
        }
        
        int result = (int) syscallDispatcher.dispatch(Syscalls.SYS_PIPE, fdArrayPtr, 0, 0);
        
        // 从用户空间读回 fd 数组
        if (result == 0 && fds != null && fds.length >= 2 && currentTask != null) {
            byte[] buf = new byte[8];
            currentTask.getAddressSpace().readBytes(fdArrayPtr, buf, 0, 8);
            fds[0] = (buf[0] & 0xFF) | ((buf[1] & 0xFF) << 8) | ((buf[2] & 0xFF) << 16) | ((buf[3] & 0xFF) << 24);
            fds[1] = (buf[4] & 0xFF) | ((buf[5] & 0xFF) << 8) | ((buf[6] & 0xFF) << 16) | ((buf[7] & 0xFF) << 24);
        }
        
        return result;
    }
    
    // ==================== 标准文件描述符常量 ====================
    
    /** 标准输入 */
    public static final int STDIN_FILENO = 0;
    
    /** 标准输出 */
    public static final int STDOUT_FILENO = 1;
    
    /** 标准错误输出 */
    public static final int STDERR_FILENO = 2;
}
