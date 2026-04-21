package jinux.include;

/**
 * Jinux 操作系统进程管理常量定义
 * <p>
 * 包含进程数量限制、段选择符、进程状态、时钟频率和调度参数等
 * 进程管理相关的常量。这些常量对应 Linux 0.01 中 include/linux/sched.h
 * 的相关定义。
 * </p>
 *
 * @author Jinux Project
 */
public class ProcessConstants {

    // ==================== 进程限制 ====================

    /**
     * 最大进程数量
     * <p>
     * Linux 0.01 内核支持的最大进程数为 64。
     * </p>
     */
    public static final int NR_TASKS = 64;

    // ==================== 段选择符 ====================

    /**
     * 内核代码段选择符
     * <p>
     * 用于内核态执行时的代码段寄存器。
     * </p>
     */
    public static final int KERNEL_CS = 0x08;

    /**
     * 内核数据段选择符
     * <p>
     * 用于内核态执行时的数据段寄存器。
     * </p>
     */
    public static final int KERNEL_DS = 0x10;

    /**
     * 用户代码段选择符
     * <p>
     * 用于用户态执行时的代码段寄存器。
     * </p>
     */
    public static final int USER_CS = 0x1B;

    /**
     * 用户数据段选择符
     * <p>
     * 用于用户态执行时的数据段寄存器。
     * </p>
     */
    public static final int USER_DS = 0x23;

    // ==================== 进程状态常量 ====================

    /**
     * 进程状态：就绪（可运行）
     * <p>
     * 进程处于可运行状态，等待 CPU 调度执行。
     * </p>
     */
    public static final int TASK_RUNNING = 0;

    /**
     * 进程状态：可中断的睡眠
     * <p>
     * 进程正在等待某个事件，可以被信号中断。
     * </p>
     */
    public static final int TASK_INTERRUPTIBLE = 1;

    /**
     * 进程状态：不可中断的睡眠
     * <p>
     * 进程正在等待某个事件，不能被信号中断。
     * </p>
     */
    public static final int TASK_UNINTERRUPTIBLE = 2;

    /**
     * 进程状态：僵尸
     * <p>
     * 进程已退出，但父进程尚未回收其资源。
     * </p>
     */
    public static final int TASK_ZOMBIE = 3;

    /**
     * 进程状态：停止
     * <p>
     * 进程被调试器或其他机制暂停执行。
     * </p>
     */
    public static final int TASK_STOPPED = 4;

    // ==================== 时钟和调度常量 ====================

    /**
     * 时钟中断频率（HZ）
     * <p>
     * 每秒产生 100 次时钟中断，用于时间管理和进程调度。
     * </p>
     */
    public static final int HZ = 100;

    /**
     * 时钟中断间隔（毫秒）
     * <p>
     * 每次时钟中断的时间间隔，等于 1000/HZ = 10 毫秒。
     * </p>
     */
    public static final int TICK_MS = 1000 / HZ;

    /**
     * 进程默认时间片
     * <p>
     * 新创建进程或重新调度的进程默认拥有的时间片数量（以 tick 为单位）。
     * </p>
     */
    public static final int DEF_COUNTER = 10;

    /**
     * 进程默认优先级
     * <p>
     * 新创建进程的默认优先级值。
     * </p>
     */
    public static final int DEF_PRIORITY = 15;

    /**
     * execve 参数数组最大元素数（argv/envp）
     */
    public static final int MAX_EXEC_ARGS = 64;

    /**
     * 初始化进程分配的初始页面数
     */
    public static final int INIT_PROCESS_PAGES = 4;
}
