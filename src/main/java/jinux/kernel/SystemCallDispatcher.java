package jinux.kernel;

import jinux.include.Const;
import jinux.include.Syscalls;
import jinux.mm.MemoryManager;
import jinux.fs.VirtualFileSystem;
import jinux.kernel.syscall.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统调用表和分发器
 * 对应 Linux 0.01 中的 kernel/system_call.s 和各系统调用实现
 * 
 * 系统调用的具体实现已拆分到 syscall 子包中的各个子处理器：
 * - ProcessSyscalls: 进程管理（fork, exit, wait, execve 等）
 * - FileSyscalls: 文件操作（read, write, open, close 等）
 * - SignalSyscalls: 信号管理（signal, kill）
 * - IpcSyscalls: 进程间通信（pipe）
 * - MiscSyscalls: 杂项（brk, time, times）
 * 
 * @author Jinux Project
 */
public class SystemCallDispatcher {
    
    /** 调度器 */
    private final Scheduler scheduler;
    
    /** 系统调用处理器映射 */
    private final Map<Integer, SystemCallHandler> handlers;
    
    /** 子处理器 */
    private final ProcessSyscalls processSyscalls;
    private final FileSyscalls fileSyscalls;
    private final SignalSyscalls signalSyscalls;
    private final IpcSyscalls ipcSyscalls;
    private final MiscSyscalls miscSyscalls;
    
    /**
     * 系统调用处理器接口
     */
    @FunctionalInterface
    public interface SystemCallHandler {
        long handle(Task task, long arg1, long arg2, long arg3);
    }
    
    /**
     * 构造系统调用分发器
     */
    public SystemCallDispatcher(Scheduler scheduler, MemoryManager memoryManager) {
        this.scheduler = scheduler;
        this.handlers = new HashMap<>();
        
        // 初始化子处理器
        this.processSyscalls = new ProcessSyscalls(scheduler, memoryManager);
        this.fileSyscalls = new FileSyscalls();
        this.signalSyscalls = new SignalSyscalls(scheduler);
        this.ipcSyscalls = new IpcSyscalls();
        this.miscSyscalls = new MiscSyscalls();
        
        registerSystemCalls();
    }
    
    /**
     * 设置文件系统
     */
    public void setVfs(VirtualFileSystem vfs) {
        processSyscalls.setVfs(vfs);
        fileSyscalls.setVfs(vfs);
    }
    
    /**
     * 注册所有系统调用（委托给各子处理器）
     */
    private void registerSystemCalls() {
        processSyscalls.registerHandlers(handlers);
        fileSyscalls.registerHandlers(handlers);
        signalSyscalls.registerHandlers(handlers);
        ipcSyscalls.registerHandlers(handlers);
        miscSyscalls.registerHandlers(handlers);
    }
    
    /**
     * 分发系统调用
     * 
     * @param nr 系统调用号
     * @param arg1 参数1
     * @param arg2 参数2
     * @param arg3 参数3
     * @return 返回值
     */
    public long dispatch(int nr, long arg1, long arg2, long arg3) {
        Task currentTask = scheduler.getCurrentTask();
        
        if (currentTask == null) {
            System.err.println("[SYSCALL] ERROR: No current task!");
            return -Const.ESRCH;
        }
        
        SystemCallHandler handler = handlers.get(nr);
        if (handler == null) {
            System.err.println("[SYSCALL] ERROR: Unknown syscall: " + nr + " (" + 
                Syscalls.getSyscallName(nr) + ")");
            return -Const.EINVAL;
        }
        
        // 记录进入内核态
        long startTime = System.nanoTime();
        
        try {
            long result = handler.handle(currentTask, arg1, arg2, arg3);
            
            // 统计内核态时间
            long endTime = System.nanoTime();
            currentTask.addStime((endTime - startTime) / 1000000);
            
            return result;
            
        } catch (Exception e) {
            System.err.println("[SYSCALL] Exception in syscall " + Syscalls.getSyscallName(nr) + 
                ": " + e.getMessage());
            e.printStackTrace();
            return -Const.EFAULT;
        }
    }
    
    /**
     * 处理进程的待处理信号（委托给 SignalSyscalls）
     * 应在系统调用返回前或调度器中调用
     */
    public void processSignals(Task task) {
        signalSyscalls.processSignals(task);
    }
}
