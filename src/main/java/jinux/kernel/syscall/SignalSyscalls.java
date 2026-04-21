package jinux.kernel.syscall;

import jinux.include.ErrorCode;
import jinux.include.ProcessConstants;
import jinux.include.Syscalls;
import jinux.kernel.Task;
import jinux.kernel.Scheduler;
import jinux.kernel.Signal;
import jinux.kernel.SystemCallDispatcher;

import java.util.Map;

/**
 * 信号系统调用
 * 从 SystemCallDispatcher 中拆分出的信号相关系统调用实现
 * 
 * @author Jinux Project
 */
public class SignalSyscalls {

    private final Scheduler scheduler;

    public SignalSyscalls(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    /**
     * 注册信号相关的系统调用处理器
     */
    public void registerHandlers(Map<Integer, SystemCallDispatcher.SystemCallHandler> handlers) {
        handlers.put(Syscalls.SYS_SIGNAL, this::sysSignal);
        handlers.put(Syscalls.SYS_KILL, this::sysKill);
    }

    // ==================== 系统调用实现 ====================

    /**
     * sys_signal - 设置信号处理器
     */
    private long sysSignal(Task task, long signum, long handler, long arg3) {
        System.out.println("[SYSCALL] signal(" + signum + ", " + handler + ") called by pid=" + task.getPid());

        if (signum < 1 || signum >= Signal.NSIG) {
            System.err.println("[SYSCALL] signal: invalid signal number " + signum);
            return -ErrorCode.EINVAL;
        }

        if (signum == Signal.SIGKILL || signum == Signal.SIGSTOP) {
            System.err.println("[SYSCALL] signal: cannot catch or ignore SIGKILL/SIGSTOP");
            return -ErrorCode.EINVAL;
        }

        Task.SignalHandlerEntry[] signalHandlers = task.getSignalHandlers();
        long oldHandler = signalHandlers[(int) signum].getHandler();

        signalHandlers[(int) signum].setHandler(handler);
        signalHandlers[(int) signum].setCustomHandler(null);

        System.out.println("[SYSCALL] signal() set handler for " + Signal.getSignalName((int) signum) +
                ", old=" + oldHandler + ", new=" + handler);
        return oldHandler;
    }

    /**
     * sys_kill - 发送信号到进程
     */
    private long sysKill(Task task, long pid, long signum, long arg3) {
        System.out.println("[SYSCALL] kill(" + pid + ", " + Signal.getSignalName((int) signum) +
                ") called by pid=" + task.getPid());

        if (signum < 0 || signum >= Signal.NSIG) {
            System.err.println("[SYSCALL] kill: invalid signal number " + signum);
            return -ErrorCode.EINVAL;
        }

        Task target = scheduler.findTask((int) pid);
        if (target == null) {
            System.err.println("[SYSCALL] kill: process " + pid + " not found");
            return -ErrorCode.ESRCH;
        }

        if (signum > 0) {
            target.sendSignal((int) signum);
            System.out.println("[SYSCALL] kill() sent " + Signal.getSignalName((int) signum) +
                    " to pid=" + pid);
        }

        return 0;
    }

    // ==================== 信号处理 ====================

    /**
     * 处理进程的待处理信号
     * 应在系统调用返回前或调度器中调用
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

            task.clearSignal(signum);

            Task.SignalHandlerEntry[] signalHandlers = task.getSignalHandlers();
            long handler = signalHandlers[signum].getHandler();
            Signal.SignalHandler customHandler = signalHandlers[signum].getCustomHandler();

            System.out.println("[SIGNAL] Processing " + Signal.getSignalName(signum) +
                    " for pid=" + task.getPid() + ", handler=" + handler);

            if (handler == Signal.SIG_IGN) {
                System.out.println("[SIGNAL] Ignored " + Signal.getSignalName(signum));
            } else if (handler == Signal.SIG_DFL) {
                Signal.SignalAction action = Signal.getDefaultAction(signum);
                handleDefaultSignalAction(task, signum, action);
            } else if (customHandler != null) {
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
                task.exit(128 + signum);
                scheduler.schedule();
                break;

            case STOP:
                System.out.println("[SIGNAL] Default action: stop pid=" + task.getPid());
                task.setState(ProcessConstants.TASK_STOPPED);
                scheduler.schedule();
                break;

            case CORE_DUMP:
                System.out.println("[SIGNAL] Default action: core dump pid=" + task.getPid() +
                        " by " + Signal.getSignalName(signum));
                task.exit(128 + signum);
                scheduler.schedule();
                break;

            case CONTINUE:
                System.out.println("[SIGNAL] Default action: continue pid=" + task.getPid());
                if (task.getState() == ProcessConstants.TASK_STOPPED) {
                    task.setState(ProcessConstants.TASK_RUNNING);
                }
                break;
        }
    }
}
