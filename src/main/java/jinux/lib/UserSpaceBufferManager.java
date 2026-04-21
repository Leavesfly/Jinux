package jinux.lib;

import jinux.kernel.Scheduler;
import jinux.kernel.Task;

/**
 * 用户空间缓冲区管理器。
 * 负责将数据写入用户进程的地址空间，用于系统调用参数传递。
 * <p>
 * 该类管理一个临时缓冲区（位于用户空间的固定基址），提供以下功能：
 * <ul>
 *   <li>将 Java 字符串写入用户空间（以 null 结尾的 C 风格字符串）</li>
 *   <li>将字符串数组写入用户空间（构建 C 风格的 char** 指针数组）</li>
 *   <li>重置缓冲区偏移量（每次系统调用前调用）</li>
 * </ul>
 */
public class UserSpaceBufferManager {
    
    /** 用户空间缓冲区基址（位于栈下方的保留区域，63MB 处） */
    private static final long USER_BUF_BASE = 0x03F00000L;
    
    /** 用户空间缓冲区大小（64KB） */
    private static final int USER_BUF_SIZE = 64 * 1024;
    
    /** 当前缓冲区偏移量 */
    private long userBufOffset;
    
    /** 调度器引用，用于获取当前任务和地址空间 */
    private final Scheduler scheduler;
    
    /**
     * 构造函数
     * 
     * @param scheduler 调度器实例
     */
    public UserSpaceBufferManager(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.userBufOffset = 0;
    }
    
    /**
     * 重置用户空间临时缓冲区（每次系统调用前调用）
     */
    public void resetUserBuffer() {
        userBufOffset = 0;
    }
    
    /**
     * 获取当前缓冲区偏移量
     * 
     * @return 当前缓冲区偏移量
     */
    public long getUserBufOffset() {
        return userBufOffset;
    }
    
    /**
     * 将字符串写入用户空间内存，返回虚拟地址。
     * 字符串以 null 字节结尾，符合 C 语言约定。
     * 
     * @param str 要写入的字符串
     * @return 用户空间虚拟地址，失败或 str 为 null 时返回 0
     */
    public long writeStringToUserSpace(String str) {
        if (str == null || scheduler == null) {
            return 0;
        }
        
        Task currentTask = scheduler.getCurrentTask();
        if (currentTask == null) {
            return 0;
        }
        
        try {
            byte[] bytes = str.getBytes();
            int totalLen = bytes.length + 1; // +1 for null terminator
            
            // 检查缓冲区空间
            if (userBufOffset + totalLen > USER_BUF_SIZE) {
                return 0; // 缓冲区空间不足
            }
            
            long vaddr = USER_BUF_BASE + userBufOffset;
            
            // 确保内存页已映射
            currentTask.getAddressSpace().allocateAndMap(vaddr, 7); // PRESENT|RW|USER
            if (totalLen > 4096) {
                // 跨页时映射下一页
                currentTask.getAddressSpace().allocateAndMap(vaddr + 4096, 7);
            }
            
            // 写入字符串内容
            currentTask.getAddressSpace().writeBytes(vaddr, bytes, 0, bytes.length);
            // 写入 null 终止符
            currentTask.getAddressSpace().writeByte(vaddr + bytes.length, (byte) 0);
            
            userBufOffset += totalLen;
            // 对齐到 8 字节边界
            userBufOffset = (userBufOffset + 7) & ~7;
            
            return vaddr;
        } catch (Exception e) {
            System.err.println("[UserSpaceBufferManager] Failed to write string to user space: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 将字符串数组写入用户空间内存，构建 C 风格的 char** 指针数组。
     * 数组以 NULL 指针结尾。
     * 
     * @param array 字符串数组
     * @return 用户空间指针数组的虚拟地址，失败或 array 为 null 时返回 0
     */
    public long writeStringArrayToUserSpace(String[] array) {
        if (array == null || array.length == 0 || scheduler == null) {
            return 0;
        }
        
        Task currentTask = scheduler.getCurrentTask();
        if (currentTask == null) {
            return 0;
        }
        
        try {
            // 先写入所有字符串，收集各字符串的虚拟地址
            long[] stringAddrs = new long[array.length];
            for (int i = 0; i < array.length; i++) {
                stringAddrs[i] = writeStringToUserSpace(array[i]);
                if (stringAddrs[i] == 0) {
                    return 0;
                }
            }
            
            // 构建指针数组（每个指针 8 字节，末尾 NULL）
            int pointerArraySize = (array.length + 1) * 8;
            if (userBufOffset + pointerArraySize > USER_BUF_SIZE) {
                return 0;
            }
            
            long arrayAddr = USER_BUF_BASE + userBufOffset;
            currentTask.getAddressSpace().allocateAndMap(arrayAddr, 7);
            if (pointerArraySize > 4096) {
                currentTask.getAddressSpace().allocateAndMap(arrayAddr + 4096, 7);
            }
            
            // 写入各字符串指针（小端序）
            for (int i = 0; i < array.length; i++) {
                byte[] ptrBytes = longToLittleEndianBytes(stringAddrs[i]);
                currentTask.getAddressSpace().writeBytes(arrayAddr + i * 8, ptrBytes, 0, 8);
            }
            // 写入末尾 NULL 指针
            byte[] nullPtr = new byte[8];
            currentTask.getAddressSpace().writeBytes(arrayAddr + array.length * 8, nullPtr, 0, 8);
            
            userBufOffset += pointerArraySize;
            userBufOffset = (userBufOffset + 7) & ~7;
            
            return arrayAddr;
        } catch (Exception e) {
            System.err.println("[UserSpaceBufferManager] Failed to write string array to user space: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * 将 long 值转换为小端序字节数组
     * 
     * @param value long 值
     * @return 8 字节的小端序字节数组
     */
    private byte[] longToLittleEndianBytes(long value) {
        byte[] bytes = new byte[8];
        for (int i = 0; i < 8; i++) {
            bytes[i] = (byte) (value >>> (i * 8));
        }
        return bytes;
    }
}
