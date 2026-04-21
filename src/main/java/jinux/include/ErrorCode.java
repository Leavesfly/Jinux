package jinux.include;

/**
 * Jinux 操作系统错误码常量定义
 * <p>
 * 包含系统调用和内核操作可能返回的错误码常量。
 * 这些错误码对应 Linux 0.01 中 include/linux/errno.h 的相关定义，
 * 与 POSIX 标准的 errno 值保持一致。
 * </p>
 *
 * @author Jinux Project
 */
public class ErrorCode {

    /**
     * 成功
     * <p>
     * 表示操作成功完成，无错误。
     * </p>
     */
    public static final int E_OK = 0;

    /**
     * 操作不允许 (Operation not permitted)
     * <p>
     * 尝试执行没有权限的操作时返回。
     * </p>
     */
    public static final int EPERM = 1;

    /**
     * 文件或目录不存在 (No such file or directory)
     * <p>
     * 尝试访问不存在的文件或目录时返回。
     * </p>
     */
    public static final int ENOENT = 2;

    /**
     * 进程不存在 (No such process)
     * <p>
     * 尝试操作不存在的进程时返回。
     * </p>
     */
    public static final int ESRCH = 3;

    /**
     * 中断的系统调用 (Interrupted system call)
     * <p>
     * 系统调用被信号中断时返回。
     * </p>
     */
    public static final int EINTR = 4;

    /**
     * I/O 错误 (I/O error)
     * <p>
     * 发生输入/输出错误时返回。
     * </p>
     */
    public static final int EIO = 5;

    /**
     * 文件描述符错误 (Bad file number)
     * <p>
     * 使用无效的文件描述符时返回。
     * </p>
     */
    public static final int EBADF = 9;

    /**
     * 内存不足 (Out of memory)
     * <p>
     * 系统无法分配所需的内存资源时返回。
     * </p>
     */
    public static final int ENOMEM = 12;

    /**
     * 访问被拒绝 (Permission denied)
     * <p>
     * 没有足够的权限访问资源时返回。
     * </p>
     */
    public static final int EACCES = 13;

    /**
     * 地址错误 (Bad address)
     * <p>
     * 尝试访问无效的内存地址时返回。
     * </p>
     */
    public static final int EFAULT = 14;

    /**
     * 设备忙 (Device or resource busy)
     * <p>
     * 尝试操作正在使用的设备或资源时返回。
     * </p>
     */
    public static final int EBUSY = 16;

    /**
     * 文件已存在 (File exists)
     * <p>
     * 尝试创建已存在的文件时返回。
     * </p>
     */
    public static final int EEXIST = 17;

    /**
     * 不是目录 (Not a directory)
     * <p>
     * 期望是目录但实际不是时返回。
     * </p>
     */
    public static final int ENOTDIR = 20;

    /**
     * 是目录 (Is a directory)
     * <p>
     * 期望是普通文件但实际是目录时返回。
     * </p>
     */
    public static final int EISDIR = 21;

    /**
     * 参数无效 (Invalid argument)
     * <p>
     * 传递给系统调用的参数无效时返回。
     * </p>
     */
    public static final int EINVAL = 22;

    /**
     * 打开文件过多 (Too many open files)
     * <p>
     * 进程打开的文件数量超过限制时返回。
     * </p>
     */
    public static final int EMFILE = 24;

    /**
     * 管道破裂 (Broken pipe)
     * <p>
     * 向没有读取端的管道写入数据时返回。
     * </p>
     */
    public static final int EPIPE = 32;
}
