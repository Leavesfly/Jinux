package jinux.lib;

import jinux.kernel.Scheduler;
import jinux.kernel.SystemCallDispatcher;
import jinux.include.Syscalls;

/**
 * 进程管理子模块。
 * 提供进程创建、退出、等待、ID 获取等核心进程管理功能。
 */
public class ProcessLib {
    
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
    public ProcessLib(SystemCallDispatcher syscallDispatcher, Scheduler scheduler, UserSpaceBufferManager bufferManager) {
        this.syscallDispatcher = syscallDispatcher;
        this.scheduler = scheduler;
        this.bufferManager = bufferManager;
    }
    
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
        bufferManager.resetUserBuffer();
        long pathPtr = bufferManager.writeStringToUserSpace(path);
        if (pathPtr == 0) {
            return -1;
        }
        
        long argvPtr = bufferManager.writeStringArrayToUserSpace(argv);
        long envpPtr = bufferManager.writeStringArrayToUserSpace(envp);
        
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
}
