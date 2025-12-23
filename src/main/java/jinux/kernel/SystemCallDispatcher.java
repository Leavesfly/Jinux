package jinux.kernel;

import jinux.include.Const;
import jinux.include.Syscalls;
import jinux.mm.MemoryManager;
import jinux.fs.VirtualFileSystem;
import jinux.ipc.Pipe;
import jinux.ipc.PipeFile;
import jinux.exec.ProgramLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统调用表和分发器
 * 对应 Linux 0.01 中的 kernel/system_call.s 和各系统调用实现
 * 
 * @author Jinux Project
 */
public class SystemCallDispatcher {
    
    /** 调度器 */
    private final Scheduler scheduler;
    
    /** 内存管理器 */
    private final MemoryManager memoryManager;
    
    /** 文件系统 */
    private VirtualFileSystem vfs;
    
    /** 系统调用处理器映射 */
    private final Map<Integer, SystemCallHandler> handlers;
    
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
        this.memoryManager = memoryManager;
        this.handlers = new HashMap<>();
        
        registerSystemCalls();
    }
    
    /**
     * 设置文件系统
     */
    public void setVfs(VirtualFileSystem vfs) {
        this.vfs = vfs;
    }
    
    /**
     * 注册所有系统调用
     */
    private void registerSystemCalls() {
        // 进程管理
        handlers.put(Syscalls.SYS_FORK, this::sysFork);
        handlers.put(Syscalls.SYS_EXIT, this::sysExit);
        handlers.put(Syscalls.SYS_WAIT, this::sysWait);
        handlers.put(Syscalls.SYS_EXECVE, this::sysExecve);
        handlers.put(Syscalls.SYS_GETPID, this::sysGetpid);
        handlers.put(Syscalls.SYS_GETPPID, this::sysGetppid);
        handlers.put(Syscalls.SYS_PAUSE, this::sysPause);
        
        // 信号管理
        handlers.put(Syscalls.SYS_SIGNAL, this::sysSignal);
        handlers.put(Syscalls.SYS_KILL, this::sysKill);
        
        // 进程间通信
        handlers.put(Syscalls.SYS_PIPE, this::sysPipe);
        
        // 文件操作
        handlers.put(Syscalls.SYS_READ, this::sysRead);
        handlers.put(Syscalls.SYS_WRITE, this::sysWrite);
        handlers.put(Syscalls.SYS_OPEN, this::sysOpen);
        handlers.put(Syscalls.SYS_CLOSE, this::sysClose);
        
        // 内存管理
        handlers.put(Syscalls.SYS_BRK, this::sysBrk);
        
        // 时间
        handlers.put(Syscalls.SYS_TIME, this::sysTime);
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
            currentTask.addStime((endTime - startTime) / 1000000); // 转换为毫秒
            
            return result;
            
        } catch (Exception e) {
            System.err.println("[SYSCALL] Exception in syscall " + Syscalls.getSyscallName(nr) + 
                ": " + e.getMessage());
            e.printStackTrace();
            return -Const.EFAULT;
        }
    }
    
    // ==================== 进程管理系统调用实现 ====================
    
    /**
     * sys_fork - 创建子进程
     */
    private long sysFork(Task parent, long arg1, long arg2, long arg3) {
        System.out.println("[SYSCALL] fork() called by pid=" + parent.getPid());
        
        // 分配新 PID
        int childPid = scheduler.allocatePid();
        
        // 复制地址空间
        var childAddrSpace = parent.getAddressSpace().copy();
        
        // 创建子进程
        Task child = new Task(childPid, parent.getPid(), childAddrSpace);
        child.setPriority(parent.getPriority());
        child.setCounter(parent.getCounter());
        
        // 复制文件描述符表
        child.setFdTable(parent.getFdTable().copy());
        child.setCurrentWorkingDir(parent.getCurrentWorkingDir());
        
        // 添加到调度器
        if (!scheduler.addTask(child)) {
            // 失败，释放资源
            childAddrSpace.free();
            return -Const.ENOMEM;
        }
        
        System.out.println("[SYSCALL] fork() created child pid=" + childPid);
        
        // 父进程返回子进程 PID，子进程返回 0（这里简化处理）
        return childPid;
    }
    
    /**
     * sys_exit - 退出进程
     */
    private long sysExit(Task task, long exitCode, long arg2, long arg3) {
        System.out.println("[SYSCALL] exit(" + exitCode + ") called by pid=" + task.getPid());
        
        task.exit((int) exitCode);
        
        // 唤醒父进程（如果在等待）
        Task parent = scheduler.findTask(task.getPpid());
        if (parent != null && parent.getWaitingForPid() == task.getPid()) {
            parent.wakeUp();
        }
        
        // 重新调度
        scheduler.schedule();
        
        return 0;
    }
    
    /**
     * sys_wait - 等待子进程
     */
    private long sysWait(Task task, long statusPtr, long arg2, long arg3) {
        System.out.println("[SYSCALL] wait() called by pid=" + task.getPid());
        
        // 查找僵尸子进程
        for (Task t : scheduler.getTaskTable()) {
            if (t != null && t.getPpid() == task.getPid() && t.getState() == Const.TASK_ZOMBIE) {
                int childPid = t.getPid();
                int exitCode = t.getExitCode();
                
                // 回收子进程
                scheduler.removeTask(childPid);
                
                System.out.println("[SYSCALL] wait() collected zombie child pid=" + childPid);
                return childPid;
            }
        }
        
        // 没有僵尸子进程，睡眠等待
        task.sleep(true);
        scheduler.schedule();
        
        return -Const.EINTR; // 简化：返回中断
    }
    
    /**
     * sys_getpid - 获取进程 ID
     */
    private long sysGetpid(Task task, long arg1, long arg2, long arg3) {
        return task.getPid();
    }
    
    /**
     * sys_getppid - 获取父进程 ID
     */
    private long sysGetppid(Task task, long arg1, long arg2, long arg3) {
        return task.getPpid();
    }
    
    /**
     * sys_execve - 加载并执行新程序
     * 
     * @param task 当前进程
     * @param filename 程序路径（模拟：使用程序名）
     * @param argv 参数数组（简化：传递字符串）
     * @param envp 环境变量
     * @return 0 成功，负数表示错误
     */
    private long sysExecve(Task task, long filename, long argv, long envp) {
        // 简化实现：直接传递程序名字符串
        // 实际实现中需要从用户空间读取字符串
        String programName = "unknown";
        String[] args = null;
        String[] env = null;
        
        // 这里简化处理，实际应从内存中读取
        // 暂时使用 filename 作为程序名
        if (filename != 0) {
            // 在实际实现中，这里应该从内存读取字符串
            // 现在简化为直接传递
            programName = String.valueOf(filename);
        }
        
        System.out.println("[SYSCALL] execve(\"" + programName + "\") called by pid=" + task.getPid());
        
        // 使用程序加载器加载程序
        int result = ProgramLoader.loadProgram(task, programName, args, env);
        
        if (result < 0) {
            System.err.println("[SYSCALL] execve() failed: " + result);
            return result;
        }
        
        // execve 成功后不返回（被新程序替换）
        // 这里需要启动新程序
        Runnable executable = task.getExecutable();
        if (executable != null) {
            // 创建新线程执行
            Thread newThread = new Thread(executable, "task-" + task.getPid());
            task.setExecutionThread(newThread);
            newThread.start();
            
            System.out.println("[SYSCALL] execve() started new program");
        }
        
        // execve 成功不返回
        return 0;
    }
    
    // ==================== 文件操作系统调用实现 ====================
    
    /**
     * sys_read - 读取文件
     */
    private long sysRead(Task task, long fd, long bufPtr, long count) {
        // 简化实现：只支持标准输入
        if (fd == 0) {
            // 从标准输入读取（这里简化为不实际读取）
            System.out.println("[SYSCALL] read(fd=0, count=" + count + ") - stub");
            return 0;
        }
        
        // TODO: 实际文件读取
        System.out.println("[SYSCALL] read(fd=" + fd + ", count=" + count + ") - not implemented");
        return -Const.EBADF;
    }
    
    /**
     * sys_write - 写入文件
     */
    private long sysWrite(Task task, long fd, long bufPtr, long count) {
        // 简化实现：只支持标准输出/错误输出
        if (fd == 1 || fd == 2) {
            // 写到控制台
            System.out.println("[SYSCALL] write(fd=" + fd + ", count=" + count + ") to console");
            return count;
        }
        
        // TODO: 实际文件写入
        System.out.println("[SYSCALL] write(fd=" + fd + ", count=" + count + ") - not implemented");
        return -Const.EBADF;
    }
    
    /**
     * sys_open - 打开文件
     */
    private long sysOpen(Task task, long pathPtr, long flags, long mode) {
        System.out.println("[SYSCALL] open() - not implemented");
        return -Const.ENOENT;
    }
    
    /**
     * sys_close - 关闭文件
     */
    private long sysClose(Task task, long fd, long arg2, long arg3) {
        System.out.println("[SYSCALL] close(fd=" + fd + ")");
        task.getFdTable().close((int) fd);
        return 0;
    }
    
    // ==================== 内存管理系统调用实现 ====================
    
    /**
     * sys_brk - 设置堆结束地址
     */
    private long sysBrk(Task task, long newBrk, long arg2, long arg3) {
        System.out.println("[SYSCALL] brk(0x" + Long.toHexString(newBrk) + ") called by pid=" + task.getPid());
        
        long result = task.getAddressSpace().expandBrk(newBrk);
        
        System.out.println("[SYSCALL] brk() returned 0x" + Long.toHexString(result));
        return result;
    }
    
    // ==================== 时间系统调用实现 ====================
    
    /**
     * sys_time - 获取系统时间
     */
    private long sysTime(Task task, long timePtr, long arg2, long arg3) {
        long currentTime = System.currentTimeMillis() / 1000; // Unix 时间戳（秒）
        return currentTime;
    }
    
    // ==================== 信号系统调用实现 ====================
    
    /**
     * sys_pause - 暂停进程直到收到信号
     */
    private long sysPause(Task task, long arg1, long arg2, long arg3) {
        System.out.println("[SYSCALL] pause() called by pid=" + task.getPid());
        
        // 设置进程为可中断睡眠
        task.sleep(true);
        scheduler.schedule();
        
        // pause 总是返回 -1，错误码为 EINTR
        return -Const.EINTR;
    }
    
    /**
     * sys_signal - 设置信号处理器
     * 
     * @param task 当前进程
     * @param signum 信号编号
     * @param handler 处理器地址（SIG_DFL、SIG_IGN 或自定义地址）
     * @return 旧的处理器地址
     */
    private long sysSignal(Task task, long signum, long handler, long arg3) {
        System.out.println("[SYSCALL] signal(" + signum + ", " + handler + ") called by pid=" + task.getPid());
        
        // 检查信号编号有效性
        if (signum < 1 || signum >= Signal.NSIG) {
            System.err.println("[SYSCALL] signal: invalid signal number " + signum);
            return -Const.EINVAL;
        }
        
        // SIGKILL 和 SIGSTOP 不能被捕获或忽略
        if (signum == Signal.SIGKILL || signum == Signal.SIGSTOP) {
            System.err.println("[SYSCALL] signal: cannot catch or ignore SIGKILL/SIGSTOP");
            return -Const.EINVAL;
        }
        
        // 获取旧的处理器
        Task.SignalHandlerEntry[] handlers = task.getSignalHandlers();
        long oldHandler = handlers[(int) signum].getHandler();
        
        // 设置新的处理器
        handlers[(int) signum].setHandler(handler);
        handlers[(int) signum].setCustomHandler(null);
        
        System.out.println("[SYSCALL] signal() set handler for " + Signal.getSignalName((int) signum) + 
            ", old=" + oldHandler + ", new=" + handler);
        
        return oldHandler;
    }
    
    /**
     * sys_kill - 发送信号到进程
     * 
     * @param task 当前进程
     * @param pid 目标进程 PID
     * @param signum 信号编号
     * @return 0 成功，-1 失败
     */
    private long sysKill(Task task, long pid, long signum, long arg3) {
        System.out.println("[SYSCALL] kill(" + pid + ", " + Signal.getSignalName((int) signum) + 
            ") called by pid=" + task.getPid());
        
        // 检查信号编号有效性
        if (signum < 0 || signum >= Signal.NSIG) {
            System.err.println("[SYSCALL] kill: invalid signal number " + signum);
            return -Const.EINVAL;
        }
        
        // 查找目标进程
        Task target = scheduler.findTask((int) pid);
        if (target == null) {
            System.err.println("[SYSCALL] kill: process " + pid + " not found");
            return -Const.ESRCH;
        }
        
        // 发送信号
        if (signum > 0) {
            target.sendSignal((int) signum);
            System.out.println("[SYSCALL] kill() sent " + Signal.getSignalName((int) signum) + 
                " to pid=" + pid);
        }
        
        return 0;
    }
    
    /**
     * 处理进程的待处理信号
     * 应在系统调用返回前或调度器中调用
     * 
     * @param task 要检查的进程
     */
    public void processSignals(Task task) {
        if (task == null || !task.hasPendingSignals()) {
            return;
        }
        
        while (task.hasPendingSignals()) {
            int signum = task.getNextSignal();
            if (signum < 0) {
                break;
            }
            
            // 清除信号位
            task.clearSignal(signum);
            
            // 获取处理器
            Task.SignalHandlerEntry[] handlers = task.getSignalHandlers();
            long handler = handlers[signum].getHandler();
            Signal.SignalHandler customHandler = handlers[signum].getCustomHandler();
            
            System.out.println("[SIGNAL] Processing " + Signal.getSignalName(signum) + 
                " for pid=" + task.getPid() + ", handler=" + handler);
            
            // 执行处理
            if (handler == Signal.SIG_IGN) {
                // 忽略信号
                System.out.println("[SIGNAL] Ignored " + Signal.getSignalName(signum));
                continue;
                
            } else if (handler == Signal.SIG_DFL) {
                // 默认处理
                Signal.SignalAction action = Signal.getDefaultAction(signum);
                handleDefaultSignalAction(task, signum, action);
                
            } else if (customHandler != null) {
                // 自定义处理器
                try {
                    customHandler.handle(signum);
                } catch (Exception e) {
                    System.err.println("[SIGNAL] Exception in custom handler: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 执行信号的默认行为
     */
    private void handleDefaultSignalAction(Task task, int signum, Signal.SignalAction action) {
        switch (action) {
            case IGNORE:
                System.out.println("[SIGNAL] Default action: ignore " + Signal.getSignalName(signum));
                break;
                
            case TERMINATE:
                System.out.println("[SIGNAL] Default action: terminate pid=" + task.getPid() + 
                    " by " + Signal.getSignalName(signum));
                task.exit(128 + signum); // 约定：信号终止的退出码 = 128 + signum
                scheduler.schedule();
                break;
                
            case STOP:
                System.out.println("[SIGNAL] Default action: stop pid=" + task.getPid());
                task.setState(Const.TASK_STOPPED);
                scheduler.schedule();
                break;
                
            case CORE_DUMP:
                System.out.println("[SIGNAL] Default action: core dump pid=" + task.getPid() + 
                    " by " + Signal.getSignalName(signum));
                // 简化：不实际生成 core 文件，直接终止
                task.exit(128 + signum);
                scheduler.schedule();
                break;
                
            case CONTINUE:
                System.out.println("[SIGNAL] Default action: continue pid=" + task.getPid());
                if (task.getState() == Const.TASK_STOPPED) {
                    task.setState(Const.TASK_RUNNING);
                }
                break;
        }
    }
    
    // ==================== 进程间通信系统调用 ====================
    
    /**
     * sys_pipe - 创建管道
     * 
     * @param task 当前进程
     * @param fdArray 文件描述符数组指针（简化：返回值编码 fd[0] 和 fd[1]）
     * @return 0 成功，-1 失败
     */
    private long sysPipe(Task task, long fdArray, long arg2, long arg3) {
        System.out.println("[SYSCALL] pipe() called by pid=" + task.getPid());
        
        // 创建管道
        Pipe pipe = new Pipe();
        
        // 创建读端和写端文件描述符
        PipeFile readEnd = new PipeFile(pipe, true);
        PipeFile writeEnd = new PipeFile(pipe, false);
        
        // 分配文件描述符
        // TODO: 需要完善 FileDescriptorTable 来支持管道
        // 这里简化处理，直接返回成功
        
        System.out.println("[SYSCALL] pipe() created: " + pipe);
        System.out.println("[SYSCALL] Note: pipe file descriptors not yet integrated with fd table");
        
        // 简化：返回 0 表示成功
        // 实际上应该返回 fd[0] 和 fd[1]
        return 0;
    }
}
