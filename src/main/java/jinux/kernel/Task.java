package jinux.kernel;

import jinux.include.Const;
import jinux.mm.AddressSpace;
import jinux.fs.FileDescriptorTable;

/**
 * 进程控制块（PCB）
 * 对应 Linux 0.01 中的 task_struct
 * 
 * 包含进程的所有状态信息：PID、状态、内存、文件、寄存器等
 * 
 * @author Jinux Project
 */
public class Task {
    
    /** 进程 ID */
    private final int pid;
    
    /** 父进程 ID */
    private int ppid;
    
    /** 进程状态 */
    private int state;
    
    /** 时间片计数器 */
    private int counter;
    
    /** 优先级 */
    private int priority;
    
    /** 地址空间 */
    private AddressSpace addressSpace;
    
    /** 文件描述符表 */
    private FileDescriptorTable fdTable;
    
    /** 当前工作目录（inode 号） */
    private int currentWorkingDir;
    
    /** 退出码 */
    private int exitCode;
    
    /** 用户态运行时间（时钟滴答数） */
    private long utime;
    
    /** 内核态运行时间（时钟滴答数） */
    private long stime;
    
    /** 启动时间 */
    private long startTime;
    
    /** 进程可执行代码（简化：用 Runnable 表示） */
    private Runnable executable;
    
    /** 进程执行线程（Java 层面） */
    private Thread executionThread;
    
    /** 等待的子进程 PID（用于 wait） */
    private int waitingForPid;
    
    /** 信号位图（记录待处理的信号） */
    private long signalPending;
    
    /** 信号屏蔽位图 */
    private long signalBlocked;
    
    /** 信号处理器数组 */
    private final SignalHandlerEntry[] signalHandlers;
    
    /**
     * 构造进程控制块
     * 
     * @param pid 进程 ID
     * @param ppid 父进程 ID
     * @param addressSpace 地址空间
     */
    public Task(int pid, int ppid, AddressSpace addressSpace) {
        this.pid = pid;
        this.ppid = ppid;
        this.state = Const.TASK_RUNNING;
        this.priority = Const.DEF_PRIORITY;
        this.counter = Const.DEF_COUNTER;
        this.addressSpace = addressSpace;
        this.fdTable = new FileDescriptorTable();
        this.currentWorkingDir = 1; // 根目录 inode 号
        this.exitCode = 0;
        this.utime = 0;
        this.stime = 0;
        this.startTime = System.currentTimeMillis();
        this.waitingForPid = -1;
        this.signalPending = 0;
        this.signalBlocked = 0;
        this.signalHandlers = new SignalHandlerEntry[Signal.NSIG];
        
        // 初始化所有信号处理器为默认
        for (int i = 0; i < Signal.NSIG; i++) {
            signalHandlers[i] = new SignalHandlerEntry(Signal.SIG_DFL, null);
        }
    }
    
    /**
     * 切换到运行状态
     */
    public void switchToRunning() {
        this.state = Const.TASK_RUNNING;
    }
    
    /**
     * 切换到睡眠状态
     * 
     * @param interruptible 是否可中断
     */
    public void sleep(boolean interruptible) {
        this.state = interruptible ? Const.TASK_INTERRUPTIBLE : Const.TASK_UNINTERRUPTIBLE;
    }
    
    /**
     * 唤醒进程
     */
    public void wakeUp() {
        if (state == Const.TASK_INTERRUPTIBLE || state == Const.TASK_UNINTERRUPTIBLE) {
            state = Const.TASK_RUNNING;
        }
    }
    
    /**
     * 进程退出
     * 
     * @param code 退出码
     */
    public void exit(int code) {
        this.exitCode = code;
        this.state = Const.TASK_ZOMBIE;
        
        // 关闭所有打开的文件
        if (fdTable != null) {
            fdTable.closeAll();
        }
        
        // 释放地址空间（除了内核需要保留的信息）
        // 注意：实际的 Linux 会等父进程回收
    }
    
    /**
     * 为 exec 系统调用重置进程状态
     * 清理旧程序的状态，准备加载新程序
     */
    public void resetForExec() {
        // 重置信号处理器为默认
        for (int i = 0; i < Signal.NSIG; i++) {
            signalHandlers[i] = new SignalHandlerEntry(Signal.SIG_DFL, null);
        }
        
        // 清除待处理信号
        signalPending = 0;
        signalBlocked = 0;
        
        // 保留标准文件描述符（stdin, stdout, stderr），关闭其他
        if (fdTable != null) {
            for (int fd = 3; fd < 64; fd++) {
                fdTable.close(fd);
            }
        }
        
        // 重置用户态时间（新程序开始计时）
        utime = 0;
        
        System.out.println("[TASK] Process " + pid + " reset for exec");
    }
    
    /**
     * 减少时间片
     */
    public void decrementCounter() {
        if (counter > 0) {
            counter--;
        }
    }
    
    /**
     * 重置时间片
     */
    public void resetCounter() {
        this.counter = priority;
    }
    
    /**
     * 是否可以运行
     */
    public boolean isRunnable() {
        return state == Const.TASK_RUNNING && counter > 0;
    }
    
    /**
     * 获取状态名称（用于调试）
     */
    public String getStateName() {
        switch (state) {
            case Const.TASK_RUNNING: return "RUNNING";
            case Const.TASK_INTERRUPTIBLE: return "INTERRUPTIBLE";
            case Const.TASK_UNINTERRUPTIBLE: return "UNINTERRUPTIBLE";
            case Const.TASK_ZOMBIE: return "ZOMBIE";
            case Const.TASK_STOPPED: return "STOPPED";
            default: return "UNKNOWN";
        }
    }
    
    @Override
    public String toString() {
        return String.format("Task[pid=%d, ppid=%d, state=%s, counter=%d, priority=%d]",
            pid, ppid, getStateName(), counter, priority);
    }
    
    // ==================== Getters and Setters ====================
    
    public int getPid() {
        return pid;
    }
    
    public int getPpid() {
        return ppid;
    }
    
    public void setPpid(int ppid) {
        this.ppid = ppid;
    }
    
    public int getState() {
        return state;
    }
    
    public void setState(int state) {
        this.state = state;
    }
    
    public int getCounter() {
        return counter;
    }
    
    public void setCounter(int counter) {
        this.counter = counter;
    }
    
    public int getPriority() {
        return priority;
    }
    
    public void setPriority(int priority) {
        this.priority = priority;
    }
    
    public AddressSpace getAddressSpace() {
        return addressSpace;
    }
    
    public void setAddressSpace(AddressSpace addressSpace) {
        this.addressSpace = addressSpace;
    }
    
    public FileDescriptorTable getFdTable() {
        return fdTable;
    }
    
    public void setFdTable(FileDescriptorTable fdTable) {
        this.fdTable = fdTable;
    }
    
    public int getCurrentWorkingDir() {
        return currentWorkingDir;
    }
    
    public void setCurrentWorkingDir(int currentWorkingDir) {
        this.currentWorkingDir = currentWorkingDir;
    }
    
    public int getExitCode() {
        return exitCode;
    }
    
    public long getUtime() {
        return utime;
    }
    
    public void addUtime(long delta) {
        this.utime += delta;
    }
    
    public long getStime() {
        return stime;
    }
    
    public void addStime(long delta) {
        this.stime += delta;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public Runnable getExecutable() {
        return executable;
    }
    
    public void setExecutable(Runnable executable) {
        this.executable = executable;
    }
    
    public Thread getExecutionThread() {
        return executionThread;
    }
    
    public void setExecutionThread(Thread executionThread) {
        this.executionThread = executionThread;
    }
    
    public int getWaitingForPid() {
        return waitingForPid;
    }
    
    public void setWaitingForPid(int waitingForPid) {
        this.waitingForPid = waitingForPid;
    }
    
    public long getSignalPending() {
        return signalPending;
    }
    
    public void setSignalPending(long signalPending) {
        this.signalPending = signalPending;
    }
    
    public long getSignalBlocked() {
        return signalBlocked;
    }
    
    public void setSignalBlocked(long signalBlocked) {
        this.signalBlocked = signalBlocked;
    }
    
    public SignalHandlerEntry[] getSignalHandlers() {
        return signalHandlers;
    }
    
    /**
     * 发送信号到进程
     * 
     * @param signum 信号编号
     */
    public void sendSignal(int signum) {
        if (signum < 1 || signum >= Signal.NSIG) {
            return;
        }
        
        // 设置信号位
        signalPending |= (1L << signum);
        
        // 如果是 SIGKILL 或 SIGSTOP，立即生效（不可阻塞）
        if (signum == Signal.SIGKILL || signum == Signal.SIGSTOP) {
            wakeUp(); // 唤醒进程以处理信号
        }
        
        // SIGCONT 唤醒停止的进程
        if (signum == Signal.SIGCONT && state == Const.TASK_STOPPED) {
            state = Const.TASK_RUNNING;
        }
    }
    
    /**
     * 检查是否有待处理的信号
     */
    public boolean hasPendingSignals() {
        // 排除被阻塞的信号
        long unblocked = signalPending & ~signalBlocked;
        return unblocked != 0;
    }
    
    /**
     * 获取下一个待处理的信号
     */
    public int getNextSignal() {
        long unblocked = signalPending & ~signalBlocked;
        if (unblocked == 0) {
            return -1;
        }
        
        // 查找第一个待处理的信号
        for (int i = 1; i < Signal.NSIG; i++) {
            if ((unblocked & (1L << i)) != 0) {
                return i;
            }
        }
        
        return -1;
    }
    
    /**
     * 清除信号位
     */
    public void clearSignal(int signum) {
        signalPending &= ~(1L << signum);
    }
    
    /**
     * 信号处理器表项
     */
    public static class SignalHandlerEntry {
        /** 处理器地址（SIG_DFL、SIG_IGN 或自定义地址） */
        private long handler;
        
        /** 自定义处理器 */
        private Signal.SignalHandler customHandler;
        
        public SignalHandlerEntry(long handler, Signal.SignalHandler customHandler) {
            this.handler = handler;
            this.customHandler = customHandler;
        }
        
        public long getHandler() {
            return handler;
        }
        
        public void setHandler(long handler) {
            this.handler = handler;
        }
        
        public Signal.SignalHandler getCustomHandler() {
            return customHandler;
        }
        
        public void setCustomHandler(Signal.SignalHandler customHandler) {
            this.customHandler = customHandler;
        }
    }
}
