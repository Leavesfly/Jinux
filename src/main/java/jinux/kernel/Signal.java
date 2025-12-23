package jinux.kernel;

/**
 * 信号定义和处理
 * 对应 Linux 0.01 中的信号机制
 * 
 * @author Jinux Project
 */
public class Signal {
    
    // ==================== 信号编号定义 ====================
    
    /** SIGHUP - 挂起 */
    public static final int SIGHUP = 1;
    
    /** SIGINT - 中断（Ctrl+C） */
    public static final int SIGINT = 2;
    
    /** SIGQUIT - 退出（Ctrl+\） */
    public static final int SIGQUIT = 3;
    
    /** SIGILL - 非法指令 */
    public static final int SIGILL = 4;
    
    /** SIGTRAP - 跟踪陷阱 */
    public static final int SIGTRAP = 5;
    
    /** SIGABRT - 中止 */
    public static final int SIGABRT = 6;
    
    /** SIGFPE - 浮点异常 */
    public static final int SIGFPE = 8;
    
    /** SIGKILL - 强制终止（不可捕获） */
    public static final int SIGKILL = 9;
    
    /** SIGSEGV - 段错误 */
    public static final int SIGSEGV = 11;
    
    /** SIGPIPE - 管道破裂 */
    public static final int SIGPIPE = 13;
    
    /** SIGALRM - 定时器到期 */
    public static final int SIGALRM = 14;
    
    /** SIGTERM - 终止 */
    public static final int SIGTERM = 15;
    
    /** SIGCHLD - 子进程状态改变 */
    public static final int SIGCHLD = 17;
    
    /** SIGCONT - 继续执行 */
    public static final int SIGCONT = 18;
    
    /** SIGSTOP - 停止（不可捕获） */
    public static final int SIGSTOP = 19;
    
    /** 信号数量 */
    public static final int NSIG = 32;
    
    // ==================== 信号处理方式 ====================
    
    /** 默认处理 */
    public static final long SIG_DFL = 0;
    
    /** 忽略信号 */
    public static final long SIG_IGN = 1;
    
    /**
     * 信号处理器接口
     */
    @FunctionalInterface
    public interface SignalHandler {
        /**
         * 处理信号
         * 
         * @param signum 信号编号
         */
        void handle(int signum);
    }
    
    /**
     * 获取信号名称
     */
    public static String getSignalName(int signum) {
        switch (signum) {
            case SIGHUP: return "SIGHUP";
            case SIGINT: return "SIGINT";
            case SIGQUIT: return "SIGQUIT";
            case SIGILL: return "SIGILL";
            case SIGTRAP: return "SIGTRAP";
            case SIGABRT: return "SIGABRT";
            case SIGFPE: return "SIGFPE";
            case SIGKILL: return "SIGKILL";
            case SIGSEGV: return "SIGSEGV";
            case SIGPIPE: return "SIGPIPE";
            case SIGALRM: return "SIGALRM";
            case SIGTERM: return "SIGTERM";
            case SIGCHLD: return "SIGCHLD";
            case SIGCONT: return "SIGCONT";
            case SIGSTOP: return "SIGSTOP";
            default: return "SIG" + signum;
        }
    }
    
    /**
     * 获取信号的默认行为
     */
    public static SignalAction getDefaultAction(int signum) {
        switch (signum) {
            case SIGCHLD:
            case SIGCONT:
                return SignalAction.IGNORE;
                
            case SIGSTOP:
                return SignalAction.STOP;
                
            case SIGKILL:
            case SIGTERM:
            case SIGHUP:
            case SIGINT:
            case SIGQUIT:
            case SIGABRT:
            case SIGALRM:
            case SIGPIPE:
                return SignalAction.TERMINATE;
                
            case SIGILL:
            case SIGTRAP:
            case SIGFPE:
            case SIGSEGV:
                return SignalAction.CORE_DUMP;
                
            default:
                return SignalAction.TERMINATE;
        }
    }
    
    /**
     * 信号默认行为
     */
    public enum SignalAction {
        /** 忽略信号 */
        IGNORE,
        
        /** 终止进程 */
        TERMINATE,
        
        /** 停止进程 */
        STOP,
        
        /** 核心转储并终止 */
        CORE_DUMP,
        
        /** 继续执行 */
        CONTINUE
    }
}
