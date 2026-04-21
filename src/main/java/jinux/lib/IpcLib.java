package jinux.lib;

import jinux.kernel.Scheduler;
import jinux.kernel.Task;
import jinux.kernel.SystemCallDispatcher;
import jinux.include.Syscalls;

/**
 * 进程间通信（IPC）子模块。
 * 提供管道等进程间通信机制。
 */
public class IpcLib {
    
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
    public IpcLib(SystemCallDispatcher syscallDispatcher, Scheduler scheduler, UserSpaceBufferManager bufferManager) {
        this.syscallDispatcher = syscallDispatcher;
        this.scheduler = scheduler;
        this.bufferManager = bufferManager;
    }
    
    /**
     * 创建管道
     * 
     * @param fds 文件描述符数组，fds[0] 为读端，fds[1] 为写端
     * @return 0 成功，-1 失败
     */
    public int pipe(int[] fds) {
        bufferManager.resetUserBuffer();
        
        // 分配用户空间缓冲区存放 2 个 int（8 字节）
        long fdArrayPtr = USER_BUF_BASE + bufferManager.getUserBufOffset();
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
}
