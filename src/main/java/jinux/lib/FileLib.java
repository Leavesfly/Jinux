package jinux.lib;

import jinux.include.Syscalls;
import jinux.kernel.Scheduler;
import jinux.kernel.SystemCallDispatcher;
import jinux.kernel.Task;

/**
 * 文件操作子模块。
 * 提供文件打开、关闭、读写、创建、删除、定位等核心文件操作功能。
 */
public class FileLib {
    
    /** 用户空间缓冲区基址 */
    private static final long USER_BUF_BASE = jinux.include.MemoryConstants.USER_BUF_BASE;
    
    private final SystemCallDispatcher syscallDispatcher;
    private final Scheduler scheduler;
    private final UserSpaceBufferManager bufferManager;
    
    /**
     * 构造函数
     * 
     * @param syscallDispatcher 系统调用分发器
     * @param scheduler 调度器
     * @param bufferManager 用户空间缓冲区管理器
     */
    public FileLib(SystemCallDispatcher syscallDispatcher, Scheduler scheduler, UserSpaceBufferManager bufferManager) {
        this.syscallDispatcher = syscallDispatcher;
        this.scheduler = scheduler;
        this.bufferManager = bufferManager;
    }
    
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
        bufferManager.resetUserBuffer();
        long pathPtr = bufferManager.writeStringToUserSpace(pathname);
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
        bufferManager.resetUserBuffer();
        int readCount = Math.min(count, buf != null ? buf.length : 0);
        
        // 分配用户空间缓冲区用于接收数据
        long bufPtr = USER_BUF_BASE + bufferManager.getUserBufOffset();
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
        bufferManager.resetUserBuffer();
        int writeCount = Math.min(count, buf != null ? buf.length : 0);
        
        // 将数据写入用户空间缓冲区
        long bufPtr = USER_BUF_BASE + bufferManager.getUserBufOffset();
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
        bufferManager.resetUserBuffer();
        long pathPtr = bufferManager.writeStringToUserSpace(pathname);
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
        bufferManager.resetUserBuffer();
        long pathPtr = bufferManager.writeStringToUserSpace(pathname);
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
    
    /**
     * 改变当前工作目录
     * 
     * @param path 目录路径
     * @return 成功返回 0，失败返回 -1
     */
    public int chdir(String path) {
        bufferManager.resetUserBuffer();
        long pathPtr = bufferManager.writeStringToUserSpace(path);
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
        bufferManager.resetUserBuffer();
        long pathPtr = bufferManager.writeStringToUserSpace(pathname);
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
        bufferManager.resetUserBuffer();
        long pathPtr = bufferManager.writeStringToUserSpace(pathname);
        if (pathPtr == 0) {
            return -1;
        }
        return (int) syscallDispatcher.dispatch(Syscalls.SYS_RMDIR, pathPtr, 0, 0);
    }
    
    /**
     * 同步磁盘缓冲
     */
    public void sync() {
        syscallDispatcher.dispatch(Syscalls.SYS_SYNC, 0, 0, 0);
    }
}
