package jinux.include;

/**
 * Jinux 操作系统内存管理常量定义
 * <p>
 * 包含页面大小、内存布局、地址转换等内存管理相关的常量和辅助方法。
 * 这些常量对应 Linux 0.01 中 include/linux/mm.h 的相关定义。
 * </p>
 *
 * @author Jinux Project
 */
public class MemoryConstants {

    /**
     * 页面大小（4KB）
     * <p>
     * x86 架构标准页面大小，用于内存分页管理。
     * </p>
     */
    public static final int PAGE_SIZE = 4096;

    /**
     * 页面位移量（log2(PAGE_SIZE) = 12）
     * <p>
     * 用于虚拟地址和页号之间的快速转换。
     * </p>
     */
    public static final int PAGE_SHIFT = 12;

    /**
     * 物理内存总大小（16MB，Linux 0.01 最大支持）
     * <p>
     * Linux 0.01 内核支持的最大物理内存容量。
     * </p>
     */
    public static final int MEMORY_SIZE = 16 * 1024 * 1024;

    /**
     * 物理页面数量
     * <p>
     * 根据总内存大小和页面大小计算得出的物理页面总数。
     * </p>
     */
    public static final int NR_PAGES = MEMORY_SIZE / PAGE_SIZE;

    /**
     * 内核占用的内存（低端 1MB）
     * <p>
     * 内核代码和数据占据的低端 1MB 内存空间。
     * </p>
     */
    public static final int KERNEL_MEMORY = 1 * 1024 * 1024;

    /**
     * 每个进程的虚拟地址空间大小（64MB）
     * <p>
     * 在 Linux 0.01 中，每个用户进程拥有 64MB 的虚拟地址空间。
     * </p>
     */
    public static final int TASK_SIZE = 64 * 1024 * 1024;

    /**
     * 用户空间缓冲区基地址（63MB 处）
     * <p>
     * 用于 LibC 层在用户空间分配临时缓冲区，传递系统调用参数。
     * </p>
     */
    public static final long USER_BUF_BASE = 0x03F00000L;

    /**
     * 用户空间缓冲区大小（1MB）
     */
    public static final int USER_BUF_SIZE = 1024 * 1024;

    /**
     * 默认页面标志：存在 + 可读写 + 用户态可访问
     */
    public static final int DEFAULT_PAGE_FLAGS = 7;

    // ==================== 辅助方法 ====================

    /**
     * 虚拟地址转页号
     *
     * @param vaddr 虚拟地址
     * @return 对应的页号
     */
    public static int vaddr2page(long vaddr) {
        return (int) (vaddr >> PAGE_SHIFT);
    }

    /**
     * 页号转虚拟地址
     *
     * @param pageNo 页号
     * @return 对应的虚拟地址
     */
    public static long page2vaddr(int pageNo) {
        return ((long) pageNo) << PAGE_SHIFT;
    }
}
