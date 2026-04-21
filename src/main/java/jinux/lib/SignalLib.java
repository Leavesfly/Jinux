package jinux.lib;

import jinux.kernel.SystemCallDispatcher;
import jinux.include.Syscalls;

/**
 * 信号管理子模块。
 * 提供信号处理和发送功能。
 */
public class SignalLib {
    
    private final SystemCallDispatcher syscallDispatcher;
    
    /**
     * 构造函数
     * 
     * @param syscallDispatcher 系统调用分发器
     */
    public SignalLib(SystemCallDispatcher syscallDispatcher) {
        this.syscallDispatcher = syscallDispatcher;
    }
    
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
}
