package jinux.lib;

import jinux.kernel.SystemCallDispatcher;
import jinux.include.Syscalls;

/**
 * 内存管理子模块。
 * 提供堆内存分配和管理功能。
 */
public class MemoryLib {
    
    private final SystemCallDispatcher syscallDispatcher;
    
    /**
     * 构造函数
     * 
     * @param syscallDispatcher 系统调用分发器
     */
    public MemoryLib(SystemCallDispatcher syscallDispatcher) {
        this.syscallDispatcher = syscallDispatcher;
    }
    
    /**
     * 设置堆结束地址
     * 
     * @param addr 新的堆结束地址
     * @return 实际的堆结束地址
     */
    public long brk(long addr) {
        return syscallDispatcher.dispatch(Syscalls.SYS_BRK, addr, 0, 0);
    }
    
    /**
     * 简化的 malloc 实现（基于 brk）
     * 注意：这是一个简化版本，真实的 malloc 更复杂
     * 
     * @param size 分配大小
     * @return 分配的地址（简化返回 long）
     */
    public long malloc(long size) {
        // 简化：直接扩展 brk
        long currentBrk = brk(0); // 获取当前 brk
        long newBrk = brk(currentBrk + size);
        if (newBrk >= currentBrk + size) {
            return currentBrk;
        }
        return 0; // 分配失败
    }
}
