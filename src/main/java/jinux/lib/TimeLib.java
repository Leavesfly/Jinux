package jinux.lib;

import jinux.kernel.SystemCallDispatcher;
import jinux.include.Syscalls;

/**
 * 时间管理子模块。
 * 提供系统时间获取功能。
 */
public class TimeLib {
    
    private final SystemCallDispatcher syscallDispatcher;
    
    /**
     * 构造函数
     * 
     * @param syscallDispatcher 系统调用分发器
     */
    public TimeLib(SystemCallDispatcher syscallDispatcher) {
        this.syscallDispatcher = syscallDispatcher;
    }
    
    /**
     * 获取系统时间
     * 
     * @return Unix 时间戳（秒）
     */
    public long time() {
        return syscallDispatcher.dispatch(Syscalls.SYS_TIME, 0, 0, 0);
    }
}
