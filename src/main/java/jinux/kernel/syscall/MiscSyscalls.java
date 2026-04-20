package jinux.kernel.syscall;

import jinux.include.Const;
import jinux.include.Syscalls;
import jinux.include.Types;
import jinux.kernel.Task;
import jinux.kernel.SystemCallDispatcher;

import java.util.Map;

/**
 * 杂项系统调用（内存管理、时间）
 * 从 SystemCallDispatcher 中拆分出的内存和时间相关系统调用实现
 * 
 * @author Jinux Project
 */
public class MiscSyscalls {

    public MiscSyscalls() {
    }

    /**
     * 注册杂项系统调用处理器
     */
    public void registerHandlers(Map<Integer, SystemCallDispatcher.SystemCallHandler> handlers) {
        handlers.put(Syscalls.SYS_BRK, this::sysBrk);
        handlers.put(Syscalls.SYS_TIME, this::sysTime);
        handlers.put(Syscalls.SYS_TIMES, this::sysTimes);
    }

    // ==================== 系统调用实现 ====================

    /**
     * sys_brk - 设置堆结束地址
     */
    private long sysBrk(Task task, long newBrk, long arg2, long arg3) {
        System.out.println("[SYSCALL] brk(0x" + Long.toHexString(newBrk) + ") called by pid=" + task.getPid());
        long result = task.getAddressSpace().expandBrk(newBrk);
        System.out.println("[SYSCALL] brk() returned 0x" + Long.toHexString(result));
        return result;
    }

    /**
     * sys_time - 获取系统时间
     */
    private long sysTime(Task task, long timePtr, long arg2, long arg3) {
        return System.currentTimeMillis() / 1000;
    }

    /**
     * sys_times - 获取进程时间
     */
    private long sysTimes(Task task, long tmsPtr, long arg2, long arg3) {
        System.out.println("[SYSCALL] times() called by pid=" + task.getPid());

        Types.Tms tms = new Types.Tms();
        tms.tms_utime = task.getUtime();
        tms.tms_stime = task.getStime();
        tms.tms_cutime = 0;
        tms.tms_cstime = 0;

        if (tmsPtr != 0) {
            byte[] tmsBytes = tms.toBytes();
            int copied = copyToUser(task, tmsPtr, tmsBytes, 0, tmsBytes.length);
            if (copied < 0) {
                return -Const.EFAULT;
            }
        }

        long currentTime = System.currentTimeMillis();
        long startTime = task.getStartTime();
        long ticks = (currentTime - startTime) / 10;

        System.out.println("[SYSCALL] times() returned " + ticks + " ticks");
        return ticks;
    }

    // ==================== 辅助方法 ====================

    private int copyToUser(Task task, long userPtr, byte[] buf, int offset, int len) {
        if (userPtr == 0 || buf == null || len <= 0) {
            return -1;
        }
        try {
            task.getAddressSpace().writeBytes(userPtr, buf, offset, len);
            return len;
        } catch (Exception e) {
            System.err.println("[SYSCALL] Failed to copy to user space: " + e.getMessage());
            return -1;
        }
    }
}
