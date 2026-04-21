package jinux.kernel.syscall;

import jinux.kernel.Task;

/**
 * 用户空间数据拷贝工具类
 * 
 * 提供在内核态与用户态之间安全拷贝数据的通用方法。
 * 从 FileSyscalls、ProcessSyscalls、MiscSyscalls 中提取的公共逻辑，
 * 消除重复代码，统一错误处理策略。
 *
 * @author Jinux Project
 */
public final class UserSpaceCopier {

    /** 从用户空间拷贝字符串时的最大长度限制，防止 OOM */
    public static final int MAX_STRING_LENGTH = 4096;

    private UserSpaceCopier() {
        // 工具类，禁止实例化
    }

    /**
     * 从用户空间读取以 null 结尾的字符串
     *
     * @param task   当前进程
     * @param userPtr 用户空间字符串指针
     * @param maxLen  最大读取长度
     * @return 读取到的字符串，失败返回 null
     */
    public static String copyStringFromUser(Task task, long userPtr, int maxLen) {
        if (userPtr == 0 || maxLen <= 0) {
            return null;
        }
        maxLen = Math.min(maxLen, MAX_STRING_LENGTH);
        try {
            byte[] buffer = new byte[maxLen];
            task.getAddressSpace().readBytes(userPtr, buffer, 0, maxLen);
            int length = 0;
            while (length < maxLen && buffer[length] != 0) {
                length++;
            }
            return new String(buffer, 0, length, "UTF-8");
        } catch (Exception e) {
            System.err.println("[SYSCALL] Failed to copy string from user space: " + e.getMessage());
            return null;
        }
    }

    /**
     * 从用户空间拷贝数据到内核缓冲区
     *
     * @param task    当前进程
     * @param userPtr 用户空间源地址
     * @param buffer  目标缓冲区
     * @param offset  缓冲区偏移量
     * @param length  拷贝长度
     * @return 实际拷贝的字节数，失败返回 -1
     */
    public static int copyFromUser(Task task, long userPtr, byte[] buffer, int offset, int length) {
        if (userPtr == 0 || buffer == null || length <= 0) {
            return -1;
        }
        try {
            task.getAddressSpace().readBytes(userPtr, buffer, offset, length);
            return length;
        } catch (Exception e) {
            System.err.println("[SYSCALL] Failed to copy from user space: " + e.getMessage());
            return -1;
        }
    }

    /**
     * 从内核缓冲区拷贝数据到用户空间
     *
     * @param task    当前进程
     * @param userPtr 用户空间目标地址
     * @param buffer  源缓冲区
     * @param offset  缓冲区偏移量
     * @param length  拷贝长度
     * @return 实际拷贝的字节数，失败返回 -1
     */
    public static int copyToUser(Task task, long userPtr, byte[] buffer, int offset, int length) {
        if (userPtr == 0 || buffer == null || length <= 0) {
            return -1;
        }
        try {
            task.getAddressSpace().writeBytes(userPtr, buffer, offset, length);
            return length;
        } catch (Exception e) {
            System.err.println("[SYSCALL] Failed to copy to user space: " + e.getMessage());
            return -1;
        }
    }
}
