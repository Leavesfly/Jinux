package jinux.demo;

import jinux.kernel.Kernel;
import jinux.drivers.ConsoleDevice;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Jinux 操作系统交互式知识问答系统
 * 用于教学项目，覆盖进程管理、内存管理、文件系统、信号机制、IPC五大模块
 */
public class QuizSystem {

    // ANSI 颜色常量
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_YELLOW = "\033[33m";
    private static final String ANSI_BLUE = "\033[34m";
    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_BOLD = "\033[1m";

    // 难度级别枚举
    public enum Difficulty {
        BEGINNER("初学者"),
        BASIC("基础"),
        INTERMEDIATE("中级"),
        ADVANCED("高级"),
        EXPERT("专家");

        private final String displayName;

        Difficulty(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 知识模块枚举
    public enum Module {
        PROCESS("进程管理"),
        MEMORY("内存管理"),
        FILESYSTEM("文件系统"),
        SIGNAL("信号机制"),
        IPC("进程间通信");

        private final String displayName;

        Module(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    // 题目内部类
    public static class Question {
        String question;
        String[] options; // 4个选项
        int correctIndex; // 0-3
        String explanation;
        String module;
        String difficulty;

        public Question(String question, String[] options, int correctIndex, String explanation, String module, String difficulty) {
            this.question = question;
            this.options = options;
            this.correctIndex = correctIndex;
            this.explanation = explanation;
            this.module = module;
            this.difficulty = difficulty;
        }
    }

    // 存储所有题目的列表
    private static final List<Question> allQuestions = new ArrayList<>();

    // 静态初始化块：添加所有题目
    static {
        initializeQuestions();
    }

    private static void initializeQuestions() {
        // ==================== 进程管理模块 (PROCESS) ====================
        
        // BEGINNER 难度 - 进程管理
        allQuestions.add(new Question(
            "在 Jinux 中，PCB（进程控制块）对应的类是什么？",
            new String[]{"A. Process.java", "B. Task.java", "C. Thread.java", "D. Job.java"},
            1,
            "Jinux 中使用 Task.java 类来表示进程的 PCB（进程控制块），包含进程的所有状态信息。",
            "PROCESS", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "Jinux 中进程有哪些状态？",
            new String[]{"A. RUNNING/READY/WAITING/ZOMBIE/STOPPED", "B. NEW/RUNNING/BLOCKED/TERMINATED", "C. ACTIVE/INACTIVE/SUSPENDED", "D. IDLE/BUSY/WAIT"},
            0,
            "Jinux 中进程状态包括：RUNNING（运行）、READY（就绪）、WAITING（等待）、ZOMBIE（僵尸）、STOPPED（停止）。",
            "PROCESS", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "进程处于 RUNNING 状态表示什么？",
            new String[]{"A. 进程正在 CPU 上执行", "B. 进程等待 I/O", "C. 进程已终止", "D. 进程被挂起"},
            0,
            "RUNNING 状态表示进程当前正在 CPU 上执行指令。",
            "PROCESS", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "ZOMBIE（僵尸）状态的进程有什么特点？",
            new String[]{"A. 进程已终止但父进程未回收", "B. 进程正在运行", "C. 进程等待资源", "D. 进程被杀死"},
            0,
            "僵尸进程是已终止但其父进程尚未调用 wait() 回收的进程，仍占用 PCB 资源。",
            "PROCESS", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "READY 状态的进程表示什么？",
            new String[]{"A. 进程已准备好运行，等待调度器分配 CPU", "B. 进程正在运行", "C. 进程等待 I/O 完成", "D. 进程已终止"},
            0,
            "READY 状态表示进程已具备运行条件，只等待调度器分配 CPU 时间片。",
            "PROCESS", "BEGINNER"
        ));

        // BASIC 难度 - 进程管理
        allQuestions.add(new Question(
            "fork() 系统调用在子进程中返回什么值？",
            new String[]{"A. 0", "B. 子进程 PID", "C. 父进程 PID", "D. -1"},
            0,
            "fork() 在子进程中返回 0，在父进程中返回子进程的 PID，失败返回 -1。",
            "PROCESS", "BASIC"
        ));
        allQuestions.add(new Question(
            "时间片轮转调度（Round Robin）的基本原理是什么？",
            new String[]{"A. 每个进程分配固定时间片，轮流执行", "B. 优先级高的先执行", "C. 最短作业优先", "D. 先到先得"},
            0,
            "时间片轮转调度为每个进程分配固定的时间片，按顺序轮流执行，保证公平性。",
            "PROCESS", "BASIC"
        ));
        allQuestions.add(new Question(
            "fork() 在父进程中返回什么？",
            new String[]{"A. 0", "B. 子进程的 PID", "C. -1", "D. 父进程自己的 PID"},
            1,
            "fork() 在父进程中返回新创建的子进程的 PID，用于父进程管理子进程。",
            "PROCESS", "BASIC"
        ));
        allQuestions.add(new Question(
            "进程从 RUNNING 状态变为 WAITING 状态的常见原因是什么？",
            new String[]{"A. 发起 I/O 请求或等待资源", "B. 时间片用完", "C. 被更高优先级进程抢占", "D. 进程终止"},
            0,
            "进程发起 I/O 请求或等待某种资源时会从 RUNNING 转为 WAITING 状态。",
            "PROCESS", "BASIC"
        ));
        allQuestions.add(new Question(
            "exec() 系统调用的作用是什么？",
            new String[]{"A. 用新程序替换当前进程的地址空间", "B. 创建新进程", "C. 终止进程", "D. 切换进程状态"},
            0,
            "exec() 系统调用用新的程序映像替换当前进程的代码段、数据段等，但保持 PID 不变。",
            "PROCESS", "BASIC"
        ));

        // INTERMEDIATE 难度 - 进程管理
        allQuestions.add(new Question(
            "在 Jinux 调度算法中，counter 重新计算的公式是什么？",
            new String[]{"A. counter = counter / 2 + priority", "B. counter = priority * 2", "C. counter = counter + priority", "D. counter = priority"},
            0,
            "Jinux 调度器在每个调度周期结束时重新计算 counter：counter = counter / 2 + priority，兼顾历史执行时间和优先级。",
            "PROCESS", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "僵尸进程产生的主要原因是什么？",
            new String[]{"A. 子进程终止后父进程未调用 wait() 回收", "B. 进程被 kill -9 杀死", "C. 进程陷入死循环", "D. 内存不足"},
            0,
            "僵尸进程产生于子进程终止后，父进程没有调用 wait() 或 waitpid() 来读取子进程的退出状态并释放其 PCB。",
            "PROCESS", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "调度器中 priority 值越小表示什么？",
            new String[]{"A. 优先级越高", "B. 优先级越低", "C. 执行时间越长", "D. 等待时间越短"},
            0,
            "在 Jinux 中，priority 值越小表示优先级越高，高优先级进程获得更多 CPU 时间。",
            "PROCESS", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "进程上下文切换时需要保存哪些信息？",
            new String[]{"A. 寄存器状态、程序计数器、栈指针等", "B. 仅程序计数器", "C. 仅栈指针", "D. 仅寄存器状态"},
            0,
            "上下文切换需要保存当前进程的完整执行上下文，包括通用寄存器、程序计数器（PC）、栈指针（SP）、状态寄存器等。",
            "PROCESS", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "什么是进程的内核态和用户态？",
            new String[]{"A. 内核态可执行特权指令，用户态受限", "B. 两种不同的进程类型", "C. 两种不同的调度策略", "D. 两种不同的内存区域"},
            0,
            "内核态（Kernel Mode）下进程可以执行特权指令、访问所有硬件资源；用户态（User Mode）下进程只能执行非特权指令，访问受限。",
            "PROCESS", "INTERMEDIATE"
        ));

        // ADVANCED 难度 - 进程管理
        allQuestions.add(new Question(
            "COW（Copy-On-Write，写时复制）在 fork() 中的作用是什么？",
            new String[]{"A. 延迟物理内存复制，提高 fork 效率", "B. 立即复制所有内存", "C. 共享所有内存页", "D. 释放父进程内存"},
            0,
            "COW 技术在 fork() 时不立即复制物理内存，而是让父子进程共享页面，只有当某一方尝试写入时才复制该页，显著提高 fork 效率。",
            "PROCESS", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "init 进程（PID=1）有什么特殊性？",
            new String[]{"A. 它是所有孤儿进程的父进程，负责回收僵尸进程", "B. 它不能被杀死", "C. 它拥有最高优先级", "D. 它不占用内存"},
            0,
            "init 进程是系统启动后创建的第一个用户态进程（PID=1），它会收养所有孤儿进程，并通过 wait() 回收僵尸进程。",
            "PROCESS", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "为什么 fork() + exec() 是创建新进程的标准模式？",
            new String[]{"A. fork 复制进程，exec 替换程序，分离关注点", "B. 这是唯一能创建进程的方式", "C. 为了节省内存", "D. 为了提高安全性"},
            0,
            "fork() 复制父进程创建子进程，exec() 在子进程中加载新程序。这种分离设计允许在 fork 和 exec 之间设置文件描述符、信号处理等。",
            "PROCESS", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "进程组（Process Group）的作用是什么？",
            new String[]{"A. 方便向一组相关进程发送信号", "B. 提高调度效率", "C. 共享内存", "D. 限制资源使用"},
            0,
            "进程组将一组相关进程组织在一起，便于终端驱动程序向整个前台进程组发送信号（如 SIGINT）。",
            "PROCESS", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "守护进程（Daemon）的特点是什么？",
            new String[]{"A. 在后台运行，脱离控制终端，通常由 init 收养", "B. 优先级最高", "C. 不能被杀死", "D. 只在内核态运行"},
            0,
            "守护进程是在后台长期运行的服务进程，通常脱离控制终端，成为会话首进程，并由 init 进程收养。",
            "PROCESS", "ADVANCED"
        ));

        // EXPERT 难度 - 进程管理
        allQuestions.add(new Question(
            "进程 0（swapper/idle）和进程 1（init）的区别是什么？",
            new String[]{"A. 进程0是内核空闲任务，进程1是第一个用户态进程", "B. 进程0是init的父进程", "C. 两者都是用户态进程", "D. 没有区别"},
            0,
            "进程 0 是内核的空闲任务（idle task），在没有其他进程可运行时执行；进程 1 是第一个用户态进程 init，负责系统初始化。",
            "PROCESS", "EXPERT"
        ));
        allQuestions.add(new Question(
            "调度器饥饿（Starvation）问题是如何产生的？",
            new String[]{"A. 低优先级进程长期得不到 CPU 时间", "B. 高优先级进程过多", "C. 时间片太短", "D. 进程数量超过 CPU 核心数"},
            0,
            "调度器饥饿指低优先级进程因高优先级进程持续占用 CPU 而长期得不到执行。可通过老化（aging）技术缓解。",
            "PROCESS", "EXPERT"
        ));
        allQuestions.add(new Question(
            "什么是进程的老化（Aging）技术？",
            new String[]{"A. 随等待时间增加而提高优先级，防止饥饿", "B. 降低老进程的优先级", "C. 记录进程年龄", "D. 定期终止老进程"},
            0,
            "老化技术通过逐渐提高长期等待进程的有效优先级，确保低优先级进程最终能获得 CPU 时间，防止饥饿。",
            "PROCESS", "EXPERT"
        ));
        allQuestions.add(new Question(
            "线程与进程的主要区别是什么？",
            new String[]{"A. 线程共享地址空间，进程有独立地址空间", "B. 线程更快", "C. 进程更轻量", "D. 没有区别"},
            0,
            "同一进程内的线程共享代码段、数据段、堆等资源，但有独立的栈和寄存器；不同进程有完全独立的地址空间。",
            "PROCESS", "EXPERT"
        ));
        allQuestions.add(new Question(
            "什么是上下文切换的开销主要来自哪里？",
            new String[]{"A. 保存/恢复寄存器状态、TLB 刷新、缓存失效", "B. 仅寄存器保存", "C. 仅内存分配", "D. 仅磁盘 I/O"},
            0,
            "上下文切换开销包括：保存/恢复 CPU 寄存器、更新页表、刷新 TLB（转换后备缓冲区）、导致 CPU 缓存失效等。",
            "PROCESS", "EXPERT"
        ));

        // ==================== 内存管理模块 (MEMORY) ====================
        
        // BEGINNER 难度 - 内存管理
        allQuestions.add(new Question(
            "Jinux 中的页面大小（Page Size）是多少？",
            new String[]{"A. 4KB", "B. 8KB", "C. 1KB", "D. 16KB"},
            0,
            "Jinux 采用 4KB（4096 字节）作为标准页面大小，这是 x86 架构的常见选择。",
            "MEMORY", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "Jinux 模拟的物理内存总量是多少？",
            new String[]{"A. 16MB", "B. 64MB", "C. 4MB", "D. 128MB"},
            0,
            "Jinux 模拟的物理内存总量为 16MB，用于教学演示内存管理机制。",
            "MEMORY", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "什么是虚拟内存？",
            new String[]{"A. 为每个进程提供独立的地址空间抽象", "B. 更大的物理内存", "C. 磁盘存储空间", "D. 缓存"},
            0,
            "虚拟内存为每个进程提供独立的虚拟地址空间抽象，使进程无需关心物理内存的实际布局。",
            "MEMORY", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "页框（Page Frame）是什么？",
            new String[]{"A. 物理内存中固定大小的连续块", "B. 虚拟地址的一部分", "C. 磁盘上的交换空间", "D. 寄存器"},
            0,
            "页框是物理内存中固定大小（如 4KB）的连续块，用于存放页面数据。",
            "MEMORY", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "缺页异常（Page Fault）何时发生？",
            new String[]{"A. 访问的页面不在物理内存中", "B. 访问非法地址", "C. 内存已满", "D. 权限不足"},
            0,
            "缺页异常发生在进程访问的虚拟页面当前未被映射到物理内存时，需要操作系统加载该页面。",
            "MEMORY", "BEGINNER"
        ));

        // BASIC 难度 - 内存管理
        allQuestions.add(new Question(
            "Jinux 中使用什么方式管理物理页面的分配？",
            new String[]{"A. 位图（Bitmap）", "B. 链表", "C. 树", "D. 哈希表"},
            0,
            "Jinux 使用位图管理物理页面，每位代表一个页框，0 表示空闲，1 表示已分配。",
            "MEMORY", "BASIC"
        ));
        allQuestions.add(new Question(
            "Jinux 中每个进程的虚拟地址空间大小是多少？",
            new String[]{"A. 64MB", "B. 16MB", "C. 4GB", "D. 128MB"},
            0,
            "Jinux 中每个进程拥有 64MB 的虚拟地址空间，用于教学演示。",
            "MEMORY", "BASIC"
        ));
        allQuestions.add(new Question(
            "页表（Page Table）的作用是什么？",
            new String[]{"A. 建立虚拟地址到物理地址的映射", "B. 存储进程代码", "C. 管理文件描述符", "D. 调度进程"},
            0,
            "页表是操作系统维护的数据结构，用于将进程的虚拟地址转换为物理地址。",
            "MEMORY", "BASIC"
        ));
        allQuestions.add(new Question(
            "什么是内存分页（Paging）？",
            new String[]{"A. 将内存划分为固定大小的页进行管理", "B. 将内存划分为可变大小的段", "C. 交换内存到磁盘", "D. 压缩内存"},
            0,
            "分页是将虚拟地址空间和物理地址空间都划分为固定大小的页（如 4KB），通过页表建立映射关系。",
            "MEMORY", "BASIC"
        ));
        allQuestions.add(new Question(
            "MMU（内存管理单元）的功能是什么？",
            new String[]{"A. 硬件自动完成虚拟地址到物理地址的转换", "B. 分配内存", "C. 回收内存", "D. 压缩内存"},
            0,
            "MMU 是 CPU 中的硬件组件，负责在每次内存访问时自动查页表完成地址转换。",
            "MEMORY", "BASIC"
        ));

        // INTERMEDIATE 难度 - 内存管理
        allQuestions.add(new Question(
            "两级页表的主要作用是什么？",
            new String[]{"A. 减少页表占用的内存空间", "B. 加快地址转换速度", "C. 增加虚拟地址空间", "D. 简化页表结构"},
            0,
            "两级页表通过按需分配二级页表，避免为整个虚拟地址空间预分配巨大的单级页表，从而节省内存。",
            "MEMORY", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "brk() 系统调用的功能是什么？",
            new String[]{"A. 调整进程堆的大小", "B. 创建新进程", "C. 打开文件", "D. 发送信号"},
            0,
            "brk() 系统调用用于调整进程数据段（堆）的结束位置，实现动态内存分配（如 malloc 的底层实现）。",
            "MEMORY", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "页表项（PTE）中通常包含哪些信息？",
            new String[]{"A. 物理页框号、有效位、读写权限、脏位等", "B. 仅物理地址", "C. 仅权限位", "D. 仅有效位"},
            0,
            "页表项包含物理页框号、有效位（Present）、读写权限、脏位（Dirty）、访问位（Accessed）等控制信息。",
            "MEMORY", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "什么是 TLB（Translation Lookaside Buffer）？",
            new String[]{"A. 缓存最近使用的页表项，加速地址转换", "B. 页表的备份", "C. 磁盘缓存", "D. 寄存器"},
            0,
            "TLB 是 MMU 中的高速缓存，存储最近使用的虚拟地址到物理地址的映射，避免每次都访问内存中的页表。",
            "MEMORY", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "内存保护机制如何防止进程访问非法内存？",
            new String[]{"A. 通过页表项中的权限位和 MMU 检查", "B. 通过编译器检查", "C. 通过操作系统定期检查", "D. 无法防止"},
            0,
            "页表项中包含读/写/执行权限位，MMU 在地址转换时检查权限，违规访问触发异常由操作系统处理。",
            "MEMORY", "INTERMEDIATE"
        ));

        // ADVANCED 难度 - 内存管理
        allQuestions.add(new Question(
            "进程地址空间通常包含哪些段（Segment）？",
            new String[]{"A. 代码段、数据段、堆、栈", "B. 仅代码段和数据段", "C. 仅堆和栈", "D. 仅代码段"},
            0,
            "典型进程地址空间包括：代码段（Text，只读）、数据段（Data，全局变量）、堆（Heap，动态分配）、栈（Stack，局部变量和函数调用）。",
            "MEMORY", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "页表项的结构中，为什么需要保留一些软件可用位？",
            new String[]{"A. 供操作系统实现 COW、页面换出等功能", "B. 硬件要求", "C. 对齐需要", "D. 没有用处"},
            0,
            "页表项中的软件可用位供操作系统实现高级功能，如 COW 标记、页面是否在交换空间中、页面年龄等。",
            "MEMORY", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "什么是交换空间（Swap Space）？",
            new String[]{"A. 磁盘上用于存放被换出页面的区域", "B. 额外的物理内存", "C. 缓存", "D. 寄存器"},
            0,
            "交换空间是磁盘上的区域，当物理内存不足时，操作系统将不常用的页面换出到交换空间，需要时再换入。",
            "MEMORY", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "大页面（Huge Page）的优势是什么？",
            new String[]{"A. 减少 TLB 缺失和页表遍历次数", "B. 节省内存", "C. 提高安全性", "D. 简化编程"},
            0,
            "大页面（如 2MB、1GB）减少页表层级和 TLB 条目数量，降低 TLB 缺失率，提高内存访问性能。",
            "MEMORY", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "什么是内存映射（mmap）？",
            new String[]{"A. 将文件或设备映射到进程地址空间", "B. 复制内存", "C. 分配堆内存", "D. 交换内存"},
            0,
            "mmap 系统调用将文件或设备直接映射到进程的虚拟地址空间，实现高效的文件 I/O 和进程间共享内存。",
            "MEMORY", "ADVANCED"
        ));

        // EXPERT 难度 - 内存管理
        allQuestions.add(new Question(
            "COW（写时复制）在内存管理层面是如何实现的？",
            new String[]{"A. 父子进程共享只读页面，写入时触发缺页异常并复制页面", "B. 立即复制所有页面", "C. 使用特殊硬件", "D. 不需要实现"},
            0,
            "COW 实现：fork 时将父子进程的页面标记为只读并共享；任一方写入时触发缺页异常，内核复制该页并更新页表为可写。",
            "MEMORY", "EXPERT"
        ));
        allQuestions.add(new Question(
            "内存碎片（Fragmentation）问题指的是什么？",
            new String[]{"A. 空闲内存分散成小块，无法满足大块连续分配请求", "B. 内存泄漏", "C. 内存溢出", "D. 页面错误"},
            0,
            "外部碎片指空闲内存总量足够，但分散成不连续的小块，无法满足大块连续内存分配请求。分页机制可有效减少外部碎片。",
            "MEMORY", "EXPERT"
        ));
        allQuestions.add(new Question(
            "伙伴系统（Buddy System）用于解决什么问题？",
            new String[]{"A. 物理页面分配中的外部碎片", "B. 虚拟地址转换", "C. 页面置换", "D. 内存保护"},
            0,
            "伙伴系统将空闲页面按 2 的幂次分组，分配和释放时合并或分割伙伴块，有效减少外部碎片。",
            "MEMORY", "EXPERT"
        ));
        allQuestions.add(new Question(
            "什么是工作集（Working Set）模型？",
            new String[]{"A. 进程在最近一段时间内访问的页面集合", "B. 所有进程的页面总和", "C. 空闲页面集合", "D. 交换空间中的页面"},
            0,
            "工作集是进程在最近 Δ 时间内访问的页面集合，用于指导页面置换决策和预防抖动（Thrashing）。",
            "MEMORY", "EXPERT"
        ));
        allQuestions.add(new Question(
            "抖动（Thrashing）现象是如何产生的？",
            new String[]{"A. 进程工作集超过可用物理内存，频繁页面换入换出", "B. CPU 过载", "C. 磁盘故障", "D. 网络拥塞"},
            0,
            "抖动发生在多道程序度过高时，进程的工作集总和超过物理内存，导致频繁的页面置换，系统大部分时间花在 I/O 而非执行。",
            "MEMORY", "EXPERT"
        ));

        // ==================== 文件系统模块 (FILESYSTEM) ====================
        
        // BEGINNER 难度 - 文件系统
        allQuestions.add(new Question(
            "VFS（Virtual File System）的含义是什么？",
            new String[]{"A. 虚拟文件系统，提供统一的文件操作接口", "B. 虚拟内存文件系统", "C. 快速文件系统", "D. 安全文件系统"},
            0,
            "VFS 是内核中的抽象层，为上层应用提供统一的文件操作接口（open/read/write），屏蔽底层不同文件系统的差异。",
            "FILESYSTEM", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "Inode 是什么？",
            new String[]{"A. 索引节点，存储文件的元数据（权限、大小、位置等）", "B. 文件内容", "C. 目录项", "D. 超级块"},
            0,
            "Inode（索引节点）存储文件的元数据，如权限、所有者、大小、时间戳、数据块位置等，但不包含文件名。",
            "FILESYSTEM", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "文件描述符（File Descriptor）是什么？",
            new String[]{"A. 进程打开文件表中条目的索引（非负整数）", "B. 文件名", "C. Inode 号", "D. 文件内容"},
            0,
            "文件描述符是进程级别的非负整数，指向进程打开文件表中的条目，用于标识已打开的文件。",
            "FILESYSTEM", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "根目录（/）在文件系统中的地位是什么？",
            new String[]{"A. 文件系统的起点，所有路径的根", "B. 普通目录", "C. 用户主目录", "D. 临时目录"},
            0,
            "根目录（/）是文件系统的顶层目录，是所有绝对路径的起点。",
            "FILESYSTEM", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "什么是绝对路径和相对路径？",
            new String[]{"A. 绝对路径从根目录开始，相对路径从当前目录开始", "B. 绝对路径更长", "C. 相对路径更快", "D. 没有区别"},
            0,
            "绝对路径以 / 开头，从根目录完整指定位置；相对路径不以 / 开头，相对于当前工作目录解析。",
            "FILESYSTEM", "BEGINNER"
        ));

        // BASIC 难度 - 文件系统
        allQuestions.add(new Question(
            "SuperBlock（超级块）的作用是什么？",
            new String[]{"A. 存储文件系统的整体信息（大小、空闲块数、Inode 数量等）", "B. 存储单个文件的信息", "C. 存储目录结构", "D. 存储文件内容"},
            0,
            "超级块存储文件系统的元信息，如文件系统大小、块大小、空闲块数量、Inode 总数和空闲数等，通常在挂载时读入内存。",
            "FILESYSTEM", "BASIC"
        ));
        allQuestions.add(new Question(
            "目录（Directory）在文件系统中本质上是什么？",
            new String[]{"A. 一种特殊文件，包含文件名到 Inode 号的映射", "B. 硬件结构", "C. 内存区域", "D. 数据库"},
            0,
            "目录是一种特殊文件，其内容是文件名与对应 Inode 号的映射表（dirent 结构）。",
            "FILESYSTEM", "BASIC"
        ));
        allQuestions.add(new Question(
            "硬链接（Hard Link）和软链接（Symbolic Link）的区别是什么？",
            new String[]{"A. 硬链接指向 Inode，软链接指向路径字符串", "B. 硬链接更快", "C. 软链接不能跨文件系统", "D. 没有区别"},
            0,
            "硬链接是同一个 Inode 的多个目录项，删除一个不影响其他；软链接是包含目标路径的特殊文件，类似快捷方式。",
            "FILESYSTEM", "BASIC"
        ));
        allQuestions.add(new Question(
            "文件权限 rwx 分别代表什么？",
            new String[]{"A. 读、写、执行", "B. 运行、等待、退出", "C. 读取、写入、扩展", "D. 注册、写入、执行"},
            0,
            "rwx 分别代表 read（读）、write（写）、execute（执行）权限，对文件和目录有不同含义。",
            "FILESYSTEM", "BASIC"
        ));
        allQuestions.add(new Question(
            "open() 系统调用中的 O_RDONLY、O_WRONLY、O_RDWR 标志分别表示什么？",
            new String[]{"A. 只读、只写、读写", "B. 随机读、随机写、随机读写", "C. 远程读、远程写、远程读写", "D. 快速读、快速写、快速读写"},
            0,
            "这些标志指定文件打开模式：O_RDONLY（只读）、O_WRONLY（只写）、O_RDWR（可读可写）。",
            "FILESYSTEM", "BASIC"
        ));

        // INTERMEDIATE 难度 - 文件系统
        allQuestions.add(new Question(
            "Jinux 使用的 MINIX 文件系统中，默认有多少个 Inodes？",
            new String[]{"A. 1024", "B. 512", "C. 2048", "D. 4096"},
            0,
            "Jinux 模拟的 MINIX 文件系统默认配置为 1024 个 Inodes，限制最多同时存在 1024 个文件/目录。",
            "FILESYSTEM", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "Jinux 使用的 MINIX 文件系统中，默认有多少个 Zones（数据块）？",
            new String[]{"A. 10240", "B. 1024", "C. 5120", "D. 20480"},
            0,
            "Jinux 模拟的 MINIX 文件系统默认配置为 10240 个 Zones（数据块），用于存储文件内容。",
            "FILESYSTEM", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "目录项（Directory Entry）的结构包含什么？",
            new String[]{"A. 文件名和 Inode 号", "B. 文件内容和权限", "C. 文件大小和时间", "D. 文件所有者"},
            0,
            "目录项（dirent）主要包含文件名（name）和对应的 Inode 号（inode），建立文件名到 Inode 的映射。",
            "FILESYSTEM", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "什么是缓冲区缓存（Buffer Cache）？",
            new String[]{"A. 内存中缓存磁盘块，减少磁盘 I/O", "B. 磁盘上的缓存", "C. CPU 缓存", "D. 网络缓存"},
            0,
            "缓冲区缓存在内存中缓存最近访问的磁盘块，后续访问相同块时直接从内存读取，显著减少慢速磁盘 I/O。",
            "FILESYSTEM", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "文件系统的 journaling（日志）功能的作用是什么？",
            new String[]{"A. 记录元数据变更，保证崩溃后的一致性", "B. 记录用户操作日志", "C. 提高读写速度", "D. 压缩文件"},
            0,
            "日志文件系统在执行元数据变更前先将操作记录到日志，崩溃后可通过重放日志恢复文件系统一致性。",
            "FILESYSTEM", "INTERMEDIATE"
        ));

        // ADVANCED 难度 - 文件系统
        allQuestions.add(new Question(
            "间接块索引（Indirect Block）的作用是什么？",
            new String[]{"A. 支持大文件，通过额外块存储数据块指针", "B. 加速小文件访问", "C. 加密文件", "D. 压缩文件"},
            0,
            "间接块索引允许文件超出直接块指针的限制：一级间接块存储数据块指针，二级间接块存储一级间接块指针，以此类推支持超大文件。",
            "FILESYSTEM", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "缓冲区缓存（Buffer Cache）如何保证数据一致性？",
            new String[]{"A. 通过脏块标记和定期同步（sync）写回磁盘", "B. 不使用缓存", "C. 每次写入立即写磁盘", "D. 仅读取不写入"},
            0,
            "修改的缓存块标记为脏（Dirty），操作系统定期或通过 sync/fsync 系统调用将脏块写回磁盘，保证持久化。",
            "FILESYSTEM", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "什么是文件系统挂载（Mount）？",
            new String[]{"A. 将文件系统附加到目录树上某个挂载点", "B. 格式化文件系统", "C. 删除文件系统", "D. 复制文件系统"},
            0,
            "挂载是将一个文件系统（如分区、网络文件系统）连接到 VFS 目录树的某个目录（挂载点），使其可被访问。",
            "FILESYSTEM", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "i-node 中的直接指针、一级间接指针、二级间接指针分别能寻址多大范围？",
            new String[]{"A. 直接指针寻址小文件，间接指针逐级扩展支持更大文件", "B. 三者范围相同", "C. 仅直接指针有效", "D. 仅间接指针有效"},
            0,
            "直接指针（如 10 个）直接指向数据块；一级间接指针指向一个存满数据块指针的块；二级间接指针指向存一级间接指针的块，逐级扩展支持更大文件。",
            "FILESYSTEM", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "什么是 RAID 技术？",
            new String[]{"A. 冗余磁盘阵列，提高性能和可靠性", "B. 快速文件系统", "C. 内存扩展技术", "D. 网络协议"},
            0,
            "RAID（Redundant Array of Independent Disks）通过组合多个磁盘提供数据冗余（如 RAID 1 镜像）或性能提升（如 RAID 0 条带化）。",
            "FILESYSTEM", "ADVANCED"
        ));

        // EXPERT 难度 - 文件系统
        allQuestions.add(new Question(
            "namei() 函数的作用是什么？",
            new String[]{"A. 解析路径名，逐层查找得到目标 Inode", "B. 创建文件", "C. 删除文件", "D. 复制文件"},
            0,
            "namei() 是内核函数，负责解析路径名字符串，从根目录或当前目录开始逐层查找目录项，最终返回目标文件的 Inode。",
            "FILESYSTEM", "EXPERT"
        ));
        allQuestions.add(new Question(
            "文件系统挂载时发生了什么？",
            new String[]{"A. 读取超级块，初始化 VFS 挂载结构，关联到挂载点目录的 Inode", "B. 格式化磁盘", "C. 复制所有文件", "D. 删除原有文件"},
            0,
            "挂载时：读取设备超级块到内存，创建 mount 结构体，将文件系统根 Inode 关联到挂载点目录的 dentry，更新 VFS 树。",
            "FILESYSTEM", "EXPERT"
        ));
        allQuestions.add(new Question(
            "什么是 ext2/ext3/ext4 文件系统中的 extent 机制？",
            new String[]{"A. 用连续块范围代替块指针列表，提高大文件性能", "B. 扩展权限", "C. 扩展文件名长度", "D. 扩展用户数量"},
            0,
            "Extent 用起始块号+长度的方式记录文件块分配，相比传统块指针列表，减少元数据开销，提高大文件顺序访问性能。",
            "FILESYSTEM", "EXPERT"
        ));
        allQuestions.add(new Question(
            "文件系统的 writeback 和 ordered 模式有什么区别？",
            new String[]{"A. writeback 先写元数据后写数据，ordered 保证数据先落盘再写元数据", "B. 没有区别", "C. writeback 更快但更安全", "D. ordered 更快"},
            0,
            "Ordered 模式确保文件数据先写回磁盘后再提交元数据事务，保证崩溃后不会出现数据块指向未初始化的情况；writeback 不保证此顺序。",
            "FILESYSTEM", "EXPERT"
        ));
        allQuestions.add(new Question(
            "什么是 FUSE（Filesystem in Userspace）？",
            new String[]{"A. 允许在用户态实现文件系统，无需修改内核", "B. 快速用户文件系统", "C. 融合文件系统", "D. 未来文件系统"},
            0,
            "FUSE 框架允许开发者在用户态实现文件系统逻辑，通过内核模块转发请求，便于开发自定义文件系统（如 sshfs、ntfs-3g）。",
            "FILESYSTEM", "EXPERT"
        ));

        // ==================== 信号机制模块 (SIGNAL) ====================
        
        // BEGINNER 难度 - 信号机制
        allQuestions.add(new Question(
            "信号（Signal）的基本概念是什么？",
            new String[]{"A. 异步通知机制，用于进程间通信和控制", "B. 同步消息", "C. 数据传输协议", "D. 内存共享方式"},
            0,
            "信号是 Unix/Linux 中的异步通知机制，用于通知进程发生了某种事件（如 Ctrl+C、子进程终止、非法指令等）。",
            "SIGNAL", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "SIGKILL 信号的作用是什么？",
            new String[]{"A. 强制终止进程，不可捕获或忽略", "B. 暂停进程", "C. 继续进程", "D. 重启进程"},
            0,
            "SIGKILL（信号值 9）强制终止进程，进程不能捕获、阻塞或忽略该信号，是最后手段。",
            "SIGNAL", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "SIGINT 信号通常由什么触发？",
            new String[]{"A. 用户按下 Ctrl+C", "B. 用户按下 Ctrl+Z", "C. 子进程终止", "D. 定时器到期"},
            0,
            "SIGINT（信号值 2）通常由终端驱动程序在用户按下 Ctrl+C 时发送给前台进程组，请求中断进程。",
            "SIGNAL", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "SIGTERM 和 SIGKILL 的区别是什么？",
            new String[]{"A. SIGTERM 可被捕获和处理，SIGKILL 不可", "B. SIGTERM 更强", "C. 没有区别", "D. SIGKILL 可被捕获"},
            0,
            "SIGTERM（信号值 15）是优雅的终止请求，进程可捕获并进行清理；SIGKILL（信号值 9）强制终止，不可捕获。",
            "SIGNAL", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "信号的三种处理方式是什么？",
            new String[]{"A. 默认处理、忽略、自定义处理函数", "B. 仅默认处理", "C. 仅忽略", "D. 仅自定义"},
            0,
            "进程对信号可选择：1) 默认处理（如终止、忽略、停止）；2) 忽略（SIG_IGN）；3) 安装自定义信号处理函数（SIG_DFL 外的 handler）。",
            "SIGNAL", "BEGINNER"
        ));

        // BASIC 难度 - 信号机制
        allQuestions.add(new Question(
            "SIG_DFL 的含义是什么？",
            new String[]{"A. 使用信号的默认处理方式", "B. 忽略信号", "C. 自定义处理", "D. 阻塞信号"},
            0,
            "SIG_DFL 表示恢复信号的默认处置行为，如 SIGINT 默认终止进程，SIGCHLD 默认忽略。",
            "SIGNAL", "BASIC"
        ));
        allQuestions.add(new Question(
            "SIG_IGN 的含义是什么？",
            new String[]{"A. 忽略信号", "B. 默认处理", "C. 自定义处理", "D. 阻塞信号"},
            0,
            "SIG_IGN 表示忽略该信号，信号到达进程时被丢弃，不产生任何效果（SIGKILL 和 SIGSTOP 除外）。",
            "SIGNAL", "BASIC"
        ));
        allQuestions.add(new Question(
            "SIGCHLD 信号的用途是什么？",
            new String[]{"A. 通知父进程子进程状态变化（终止、停止等）", "B. 终止子进程", "C. 创建子进程", "D. 暂停进程"},
            0,
            "SIGCHLD 在子进程终止、停止或继续时发送给父进程，父进程通常在此信号处理中调用 wait() 回收子进程。",
            "SIGNAL", "BASIC"
        ));
        allQuestions.add(new Question(
            "kill() 系统调用的作用是什么？",
            new String[]{"A. 向指定进程或进程组发送信号", "B. 仅终止进程", "C. 仅暂停进程", "D. 仅继续进程"},
            0,
            "kill() 系统调用可向指定 PID 的进程或进程组发送任意信号，不仅限于终止（SIGKILL）。",
            "SIGNAL", "BASIC"
        ));
        allQuestions.add(new Question(
            "alarm() 系统调用配合哪个信号使用？",
            new String[]{"A. SIGALRM", "B. SIGINT", "C. SIGTERM", "D. SIGKILL"},
            0,
            "alarm() 设置定时器，超时后向调用进程发送 SIGALRM 信号，常用于实现超时控制。",
            "SIGNAL", "BASIC"
        ));

        // INTERMEDIATE 难度 - 信号机制
        allQuestions.add(new Question(
            "信号位图（Signal Bitmap）的实现方式是什么？",
            new String[]{"A. 用整数的每一位表示一个待处理信号", "B. 用数组存储信号", "C. 用链表存储信号", "D. 用哈希表存储信号"},
            0,
            "信号位图用一个整数（如 32 位）的每一位表示一个信号是否 pending，高效支持最多 32 种标准信号的集合操作。",
            "SIGNAL", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "信号屏蔽（Signal Masking）的作用是什么？",
            new String[]{"A. 临时阻塞某些信号，延迟其递送", "B. 永久禁用信号", "C. 删除信号", "D. 增强信号"},
            0,
            "信号屏蔽通过 sigprocmask() 设置进程的信号掩码，被屏蔽的信号在解除屏蔽前不会递送给进程，但会标记为 pending。",
            "SIGNAL", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "sigaction() 相比 signal() 的优势是什么？",
            new String[]{"A. 更可靠，可指定信号处理期间的行为和掩码", "B. 更快", "C. 更简单", "D. 没有优势"},
            0,
            "sigaction() 允许精确控制信号处理器的行为，包括处理期间自动屏蔽该信号、指定 sa_flags 等，比 signal() 更可靠和灵活。",
            "SIGNAL", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "信号处理函数中为什么应避免调用非异步信号安全的函数？",
            new String[]{"A. 可能导致死锁或未定义行为", "B. 会降低性能", "C. 会丢失信号", "D. 会崩溃"},
            0,
            "信号可在任意时刻中断主程序，若处理函数调用 malloc、printf 等非异步信号安全函数，可能与主程序中的调用冲突导致死锁或数据损坏。",
            "SIGNAL", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "pause() 系统调用的作用是什么？",
            new String[]{"A. 使进程休眠直到收到信号", "B. 暂停进程执行固定时间", "C. 终止进程", "D. 继续进程"},
            0,
            "pause() 使调用进程进入睡眠状态，直到收到任意信号（且该信号的处理函数返回或终止进程）。",
            "SIGNAL", "INTERMEDIATE"
        ));

        // ADVANCED 难度 - 信号机制
        allQuestions.add(new Question(
            "哪两个信号是不可捕获、不可忽略的？",
            new String[]{"A. SIGKILL 和 SIGSTOP", "B. SIGINT 和 SIGTERM", "C. SIGCHLD 和 SIGALRM", "D. SIGSEGV 和 SIGBUS"},
            0,
            "SIGKILL（9）和 SIGSTOP（19）是唯二不能被捕获、阻塞或忽略的信号，确保系统总能终止或停止任何进程。",
            "SIGNAL", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "SIGPIPE 信号在什么情况下触发？",
            new String[]{"A. 向没有读取端的管道或 socket 写入数据", "B. 管道满时写入", "C. 管道空时读取", "D. 创建管道时"},
            0,
            "当进程向一个没有读取端（所有读取端已关闭）的管道或 socket 写入时，内核发送 SIGPIPE 信号，默认终止进程。",
            "SIGNAL", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "信号处理的时机是什么时候？",
            new String[]{"A. 从内核态返回用户态时检查并递送 pending 信号", "B. 任意时刻", "C. 仅在系统调用时", "D. 仅在进程启动时"},
            0,
            "内核在进程从内核态返回用户态前检查是否有 pending 且未屏蔽的信号，若有则切换到用户态的信号处理函数执行。",
            "SIGNAL", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "sigsuspend() 的作用是什么？",
            new String[]{"A. 原子地替换信号掩码并休眠，直到收到信号", "B. 暂停进程", "C. 终止进程", "D. 忽略信号"},
            0,
            "sigsuspend() 原子地用新掩码替换当前信号掩码并使进程休眠，直到收到非屏蔽信号，用于避免竞态条件。",
            "SIGNAL", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "实时信号（Real-time Signals）与传统信号的区别是什么？",
            new String[]{"A. 实时信号支持排队和携带额外数据", "B. 实时信号更快", "C. 实时信号更少", "D. 没有区别"},
            0,
            "实时信号（SIGRTMIN~SIGRTMAX）支持排队（多个相同信号不会丢失）和通过 sigqueue() 携带额外数据（int 或指针）。",
            "SIGNAL", "ADVANCED"
        ));

        // EXPERT 难度 - 信号机制
        allQuestions.add(new Question(
            "信号与进程状态的关系是什么？",
            new String[]{"A. 某些信号可改变进程状态（如 SIGSTOP 使进程进入 STOPPED）", "B. 信号不影响进程状态", "C. 信号仅终止进程", "D. 信号仅唤醒进程"},
            0,
            "SIGSTOP 使进程进入 STOPPED 状态，SIGCONT 使 STOPPED 进程恢复，SIGKILL 使进程进入 ZOMBIE 状态等。",
            "SIGNAL", "EXPERT"
        ));
        allQuestions.add(new Question(
            "信号处理函数返回后，进程从哪里继续执行？",
            new String[]{"A. 从被信号中断的指令处继续（或被中断的系统调用可能重启）", "B. 从 main 函数开始", "C. 从信号处理函数末尾", "D. 随机位置"},
            0,
            "信号处理函数返回后，进程恢复到被信号中断时的执行点继续执行；若中断的是慢速系统调用，可能返回 EINTR 错误或自动重启。",
            "SIGNAL", "EXPERT"
        ));
        allQuestions.add(new Question(
            "什么是信号的竞态条件（Race Condition）？",
            new String[]{"A. 检查信号状态和执行操作之间存在时间窗口，信号可能在此期间到达", "B. 信号处理太慢", "C. 信号太多", "D. 信号丢失"},
            0,
            "例如：检查某条件后准备休眠，但在调用 pause() 前信号到达，导致永久休眠。需用 sigsuspend() 等原子操作避免。",
            "SIGNAL", "EXPERT"
        ));
        allQuestions.add(new Question(
            "siglongjmp() 在信号处理中的作用和风险是什么？",
            new String[]{"A. 可从信号处理函数跳转回主程序，但可能跳过资源清理代码", "B. 无风险", "C. 仅用于终止", "D. 仅用于继续"},
            0,
            "siglongjmp 可从信号处理函数非局部跳转到 setjmp 保存点，但会跳过中间的栈帧，可能导致资源泄漏或不一致状态。",
            "SIGNAL", "EXPERT"
        ));
        allQuestions.add(new Question(
            "信号处理器重入（Reentrancy）问题是什么？",
            new String[]{"A. 同一信号在处理期间再次到达，导致嵌套调用", "B. 信号处理太慢", "C. 信号丢失", "D. 信号重复"},
            0,
            "若信号处理期间同一信号再次到达（除非被自动屏蔽），会导致处理器递归调用，可能栈溢出。sigaction 默认在处理期间自动屏蔽该信号。",
            "SIGNAL", "EXPERT"
        ));

        // ==================== IPC 模块 (IPC) ====================
        
        // BEGINNER 难度 - IPC
        allQuestions.add(new Question(
            "管道（Pipe）的基本概念是什么？",
            new String[]{"A. 单向进程间通信机制，数据从写端流入，从读端流出", "B. 双向通信", "C. 共享内存", "D. 消息队列"},
            0,
            "管道是 Unix 中最基本的 IPC 机制，提供单向字节流通信，一端写入，另一端读取。",
            "IPC", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "管道是单向还是双向的？",
            new String[]{"A. 单向", "B. 双向", "C. 多向", "D. 无方向"},
            0,
            "传统匿名管道是单向的，数据只能从写端流向读端。如需双向通信，需创建两个管道或使用 socket。",
            "IPC", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "pipe() 系统调用返回什么？",
            new String[]{"A. 两个文件描述符：fd[0] 读端，fd[1] 写端", "B. 一个文件描述符", "C. 管道名称", "D. 内存地址"},
            0,
            "pipe(int fd[2]) 创建管道，fd[0] 为读端文件描述符，fd[1] 为写端文件描述符。",
            "IPC", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "命名管道（FIFO）与匿名管道的区别是什么？",
            new String[]{"A. 命名管道有文件系统路径，无关进程也可通信", "B. 命名管道更快", "C. 匿名管道更大", "D. 没有区别"},
            0,
            "匿名管道仅用于有亲缘关系的进程（如父子）；命名管道（mkfifo 创建）在文件系统中有路径，任意进程可通过路径打开通信。",
            "IPC", "BEGINNER"
        ));
        allQuestions.add(new Question(
            "IPC 的主要目的是什么？",
            new String[]{"A. 实现进程间的数据交换和协调", "B. 提高单个进程性能", "C. 减少内存使用", "D. 简化编程"},
            0,
            "IPC（进程间通信）机制允许多个进程交换数据、同步操作、协调任务，实现协作。",
            "IPC", "BEGINNER"
        ));

        // BASIC 难度 - IPC
        allQuestions.add(new Question(
            "Jinux 中管道缓冲区大小是多少？",
            new String[]{"A. 4KB", "B. 8KB", "C. 1KB", "D. 16KB"},
            0,
            "Jinux 中管道缓冲区大小为 4KB（一个页面大小），写入数据暂存于此，读端从中读取。",
            "IPC", "BASIC"
        ));
        allQuestions.add(new Question(
            "管道的读端和写端分别对应什么操作？",
            new String[]{"A. 读端用 read()，写端用 write()", "B. 都用 read()", "C. 都用 write()", "D. 用特殊函数"},
            0,
            "进程对管道读端文件描述符调用 read() 读取数据，对写端文件描述符调用 write() 写入数据。",
            "IPC", "BASIC"
        ));
        allQuestions.add(new Question(
            "当管道写端全部关闭后，读端 read() 会返回什么？",
            new String[]{"A. 0（EOF）", "B. -1", "C. 继续阻塞", "D. 抛出异常"},
            0,
            "当管道所有写端关闭后，读端 read() 返回 0 表示 EOF（文件结束），表明不再有数据可读取。",
            "IPC", "BASIC"
        ));
        allQuestions.add(new Question(
            "当管道读端全部关闭后，写端 write() 会发生什么？",
            new String[]{"A. 触发 SIGPIPE 信号，write 返回 -1", "B. 继续正常写入", "C. 阻塞", "D. 返回 0"},
            0,
            "当管道所有读端关闭后，写端 write() 会收到 SIGPIPE 信号（默认终止进程），若忽略信号则 write 返回 -1 且 errno=EPIPE。",
            "IPC", "BASIC"
        ));
        allQuestions.add(new Question(
            "dup2() 在管道使用中常用于什么？",
            new String[]{"A. 重定向标准输入/输出到管道端", "B. 复制文件", "C. 关闭管道", "D. 创建管道"},
            0,
            "dup2(pipe_fd, STDIN_FILENO) 或 dup2(pipe_fd, STDOUT_FILENO) 将管道的读/写端重定向到标准输入/输出，实现 shell 管道命令。",
            "IPC", "BASIC"
        ));

        // INTERMEDIATE 难度 - IPC
        allQuestions.add(new Question(
            "管道在什么情况下会阻塞写入？",
            new String[]{"A. 管道缓冲区已满", "B. 管道为空", "C. 读端关闭", "D. 写端关闭"},
            0,
            "当管道缓冲区已满时，write() 会阻塞直到有空间可用（除非设置为非阻塞模式 O_NONBLOCK）。",
            "IPC", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "管道在什么情况下会阻塞读取？",
            new String[]{"A. 管道为空且写端未关闭", "B. 管道已满", "C. 写端关闭", "D. 读端关闭"},
            0,
            "当管道缓冲区为空且仍有写端打开时，read() 会阻塞直到有数据可读或所有写端关闭（返回 EOF）。",
            "IPC", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "EOF 在管道中如何检测？",
            new String[]{"A. read() 返回 0", "B. read() 返回 -1", "C. write() 返回 0", "D. 特殊信号"},
            0,
            "当管道中所有写端关闭后，读端调用 read() 返回 0 表示到达 EOF，表明没有更多数据且不会有新数据。",
            "IPC", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "O_NONBLOCK 标志对管道操作的影响是什么？",
            new String[]{"A. 使 read/write 在不满足条件时立即返回而非阻塞", "B. 加速传输", "C. 增大缓冲区", "D. 减小缓冲区"},
            0,
            "设置 O_NONBLOCK 后，管道空时 read 立即返回 -1（errno=EAGAIN），管道满时 write 立即返回 -1（errno=EAGAIN），而非阻塞等待。",
            "IPC", "INTERMEDIATE"
        ));
        allQuestions.add(new Question(
            "管道容量（PIPE_BUF）的意义是什么？",
            new String[]{"A. 保证小于等于 PIPE_BUF 的写入是原子的", "B. 管道最大容量", "C. 最小读取单位", "D. 缓冲区数量"},
            0,
            "PIPE_BUF（通常 4KB）是保证原子写入的最大字节数：单次 write ≤ PIPE_BUF 时，数据不会被其他进程的输出交错。",
            "IPC", "INTERMEDIATE"
        ));

        // ADVANCED 难度 - IPC
        allQuestions.add(new Question(
            "SIGPIPE 信号的触发条件是什么？",
            new String[]{"A. 向所有读端已关闭的管道或 socket 写入", "B. 管道满时写入", "C. 管道空时读取", "D. 创建管道失败"},
            0,
            "当进程向一个所有读端已关闭的管道（或 broken socket）写入时，内核发送 SIGPIPE 信号，默认终止进程以防止无限写入。",
            "IPC", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "管道与 fork() 配合的典型用法是什么？",
            new String[]{"A. 父进程创建管道后 fork，父子进程各关闭一端实现单向通信", "B. 仅父进程使用", "C. 仅子进程使用", "D. 不需要 fork"},
            0,
            "典型模式：父进程 pipe() 创建管道，fork() 后，子进程关闭写端、父进程关闭读端（或反之），实现父子进程单向通信。",
            "IPC", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "popen() 函数的作用是什么？",
            new String[]{"A. 创建管道并启动子进程执行命令，返回文件指针", "B. 打开文件", "C. 创建命名管道", "D. 关闭管道"},
            0,
            "popen(\"command\", \"r\") 创建管道，fork+exec 执行 command，返回文件指针供读取命令输出，简化管道使用。",
            "IPC", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "管道的半关闭（Half-close）是什么？",
            new String[]{"A. 关闭管道的一端（读或写），保留另一端", "B. 关闭一半缓冲区", "C. 暂停管道", "D. 重置管道"},
            0,
            "半关闭指进程关闭管道的读端或写端之一，如子进程关闭写端后父进程可检测 EOF，常用于通知对端数据发送完毕。",
            "IPC", "ADVANCED"
        ));
        allQuestions.add(new Question(
            "socketpair() 与普通 pipe() 的区别是什么？",
            new String[]{"A. socketpair 创建双向通信通道，pipe 是单向", "B. socketpair 更快", "C. pipe 支持网络", "D. 没有区别"},
            0,
            "socketpair(AF_UNIX, SOCK_STREAM, 0, fd) 创建一对相连的 socket，支持双向通信；pipe() 创建单向管道。",
            "IPC", "ADVANCED"
        ));

        // EXPERT 难度 - IPC
        allQuestions.add(new Question(
            "管道内部环形缓冲区（Ring Buffer）的实现原理是什么？",
            new String[]{"A. 用头尾指针在固定大小缓冲区中循环写入读取", "B. 用链表", "C. 用动态数组", "D. 用哈希表"},
            0,
            "管道内核缓冲区是环形缓冲区：维护 read_pos 和 write_pos 指针，在固定大小缓冲区中循环使用空间，高效支持流式数据。",
            "IPC", "EXPERT"
        ));
        allQuestions.add(new Question(
            "管道的引用计数机制用于什么？",
            new String[]{"A. 跟踪管道两端的打开文件描述符数量，全关闭时释放资源", "B. 加速传输", "C. 加密数据", "D. 压缩数据"},
            0,
            "内核为管道维护引用计数，每有一个文件描述符指向管道则计数+1，关闭时-1，计数归零时释放管道缓冲区和 inode 等资源。",
            "IPC", "EXPERT"
        ));
        allQuestions.add(new Question(
            "管道数据在用户空间和内核空间之间如何拷贝？",
            new String[]{"A. write 从用户缓冲区拷贝到内核管道缓冲区，read 从内核拷贝到用户缓冲区", "B. 零拷贝", "C. 仅内核空间", "D. 仅用户空间"},
            0,
            "传统管道涉及两次拷贝：write() 将用户数据拷贝到内核管道缓冲区，read() 从内核缓冲区拷贝到用户缓冲区。splice() 可减少拷贝。",
            "IPC", "EXPERT"
        ));
        allQuestions.add(new Question(
            "什么是 splice() 系统调用？",
            new String[]{"A. 在内核中移动数据，避免用户空间拷贝", "B. 连接管道", "C. 分割管道", "D. 复制管道"},
            0,
            "splice() 在内核中直接将数据从一个文件描述符移动到另一个（如从文件到管道），避免数据在用户空间和内核空间之间来回拷贝，提高效率。",
            "IPC", "EXPERT"
        ));
        allQuestions.add(new Question(
            "管道与消息队列的主要区别是什么？",
            new String[]{"A. 管道是字节流无边界，消息队列保留消息边界", "B. 管道更快", "C. 消息队列更小", "D. 没有区别"},
            0,
            "管道是字节流，不保留写入边界（多次 write 可能合并读取）；消息队列（msgsnd/msgrcv）保留消息边界，按消息为单位收发。",
            "IPC", "EXPERT"
        ));
    }

    /**
     * 显示主菜单让用户选择模式
     */
    public static void runQuiz(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        while (true) {
            console.println(ANSI_BOLD + ANSI_CYAN + "\n╔══════════════════════════════════════╗" + ANSI_RESET);
            console.println(ANSI_BOLD + ANSI_CYAN + "║   Jinux 操作系统知识问答系统         ║" + ANSI_RESET);
            console.println(ANSI_BOLD + ANSI_CYAN + "╚══════════════════════════════════════╝" + ANSI_RESET);
            console.println(ANSI_BLUE + "\n请选择答题模式：" + ANSI_RESET);
            console.println(ANSI_GREEN + "  1. 按模块答题" + ANSI_RESET);
            console.println(ANSI_GREEN + "  2. 按难度答题" + ANSI_RESET);
            console.println(ANSI_GREEN + "  3. 随机挑战（10题）" + ANSI_RESET);
            console.println(ANSI_GREEN + "  4. 综合测试（全部题目）" + ANSI_RESET);
            console.println(ANSI_RED + "  5. 返回" + ANSI_RESET);
            console.print(ANSI_YELLOW + "\n请输入选项 (1-5): " + ANSI_RESET);
            
            try {
                String input = reader.readLine();
                if (input == null) break;
                
                switch (input.trim()) {
                    case "1":
                        runModuleQuiz(kernel, reader);
                        break;
                    case "2":
                        runDifficultyQuiz(kernel, reader);
                        break;
                    case "3":
                        runRandomChallenge(kernel, reader, 10);
                        break;
                    case "4":
                        askQuestions(kernel, reader, new ArrayList<>(allQuestions));
                        break;
                    case "5":
                        console.println(ANSI_CYAN + "\n感谢使用 Jinux 知识问答系统！" + ANSI_RESET);
                        return;
                    default:
                        console.println(ANSI_RED + "无效选项，请重新输入。" + ANSI_RESET);
                }
            } catch (Exception e) {
                console.println(ANSI_RED + "输入错误: " + e.getMessage() + ANSI_RESET);
            }
        }
    }

    /**
     * 按模块答题：让用户选模块后答该模块所有题
     */
    private static void runModuleQuiz(Kernel kernel, BufferedReader reader) {
        ConsoleDevice console = kernel.getConsole();
        
        console.println(ANSI_BOLD + ANSI_CYAN + "\n--- 按模块答题 ---" + ANSI_RESET);
        console.println(ANSI_BLUE + "请选择知识模块：" + ANSI_RESET);
        
        Module[] modules = Module.values();
        for (int i = 0; i < modules.length; i++) {
            console.println(ANSI_GREEN + "  " + (i + 1) + ". " + modules[i].getDisplayName() + " (" + modules[i].name() + ")" + ANSI_RESET);
        }
        console.println(ANSI_RED + "  6. 返回上级菜单" + ANSI_RESET);
        console.print(ANSI_YELLOW + "\n请输入选项 (1-6): " + ANSI_RESET);
        
        try {
            String input = reader.readLine();
            if (input == null) return;
            
            int choice = Integer.parseInt(input.trim());
            if (choice == 6) return;
            if (choice < 1 || choice > modules.length) {
                console.println(ANSI_RED + "无效选项。" + ANSI_RESET);
                return;
            }
            
            Module selectedModule = modules[choice - 1];
            List<Question> moduleQuestions = filterByModule(selectedModule.name());
            
            console.println(ANSI_CYAN + "\n开始 " + selectedModule.getDisplayName() + " 模块答题，共 " + moduleQuestions.size() + " 题\n" + ANSI_RESET);
            askQuestions(kernel, reader, moduleQuestions);
            
        } catch (NumberFormatException e) {
            console.println(ANSI_RED + "请输入有效数字。" + ANSI_RESET);
        } catch (Exception e) {
            console.println(ANSI_RED + "错误: " + e.getMessage() + ANSI_RESET);
        }
    }

    /**
     * 按难度答题：让用户选难度后答该难度所有题
     */
    private static void runDifficultyQuiz(Kernel kernel, BufferedReader reader) {
        ConsoleDevice console = kernel.getConsole();
        
        console.println(ANSI_BOLD + ANSI_CYAN + "\n--- 按难度答题 ---" + ANSI_RESET);
        console.println(ANSI_BLUE + "请选择难度级别：" + ANSI_RESET);
        
        Difficulty[] difficulties = Difficulty.values();
        for (int i = 0; i < difficulties.length; i++) {
            console.println(ANSI_GREEN + "  " + (i + 1) + ". " + difficulties[i].getDisplayName() + " (" + difficulties[i].name() + ")" + ANSI_RESET);
        }
        console.println(ANSI_RED + "  6. 返回上级菜单" + ANSI_RESET);
        console.print(ANSI_YELLOW + "\n请输入选项 (1-6): " + ANSI_RESET);
        
        try {
            String input = reader.readLine();
            if (input == null) return;
            
            int choice = Integer.parseInt(input.trim());
            if (choice == 6) return;
            if (choice < 1 || choice > difficulties.length) {
                console.println(ANSI_RED + "无效选项。" + ANSI_RESET);
                return;
            }
            
            Difficulty selectedDifficulty = difficulties[choice - 1];
            List<Question> difficultyQuestions = filterByDifficulty(selectedDifficulty.name());
            
            console.println(ANSI_CYAN + "\n开始 " + selectedDifficulty.getDisplayName() + " 难度答题，共 " + difficultyQuestions.size() + " 题\n" + ANSI_RESET);
            askQuestions(kernel, reader, difficultyQuestions);
            
        } catch (NumberFormatException e) {
            console.println(ANSI_RED + "请输入有效数字。" + ANSI_RESET);
        } catch (Exception e) {
            console.println(ANSI_RED + "错误: " + e.getMessage() + ANSI_RESET);
        }
    }

    /**
     * 随机挑战：随机抽 count 题
     */
    private static void runRandomChallenge(Kernel kernel, BufferedReader reader, int count) {
        ConsoleDevice console = kernel.getConsole();
        
        if (count > allQuestions.size()) {
            count = allQuestions.size();
        }
        
        // 随机打乱并选取 count 题
        List<Question> shuffled = new ArrayList<>(allQuestions);
        Collections.shuffle(shuffled);
        List<Question> selected = shuffled.subList(0, count);
        
        console.println(ANSI_BOLD + ANSI_CYAN + "\n--- 随机挑战（" + count + " 题）---" + ANSI_RESET);
        askQuestions(kernel, reader, selected);
    }

    /**
     * 核心答题逻辑
     */
    private static void askQuestions(Kernel kernel, BufferedReader reader, List<Question> questions) {
        ConsoleDevice console = kernel.getConsole();
        List<Boolean> results = new ArrayList<>();
        int correctCount = 0;
        
        for (int i = 0; i < questions.size(); i++) {
            Question q = questions.get(i);
            
            // 显示题目
            console.println(ANSI_BOLD + ANSI_CYAN + "\n【第 " + (i + 1) + "/" + questions.size() + " 题】" + ANSI_RESET);
            console.println(ANSI_YELLOW + "模块: " + q.module + " | 难度: " + q.difficulty + ANSI_RESET);
            console.println(ANSI_BOLD + "\n" + q.question + ANSI_RESET);
            
            // 显示选项
            for (int j = 0; j < q.options.length; j++) {
                console.println("  " + q.options[j]);
            }
            
            console.print(ANSI_YELLOW + "\n请输入答案 (A/B/C/D): " + ANSI_RESET);
            
            try {
                String input = reader.readLine();
                if (input == null) break;
                
                String answer = input.trim().toUpperCase();
                int answerIndex = -1;
                
                if (answer.equals("A")) answerIndex = 0;
                else if (answer.equals("B")) answerIndex = 1;
                else if (answer.equals("C")) answerIndex = 2;
                else if (answer.equals("D")) answerIndex = 3;
                
                if (answerIndex == -1) {
                    console.println(ANSI_RED + "无效答案，请输入 A/B/C/D" + ANSI_RESET);
                    i--; // 重新回答此题
                    continue;
                }
                
                boolean isCorrect = (answerIndex == q.correctIndex);
                results.add(isCorrect);
                
                if (isCorrect) {
                    correctCount++;
                    console.println(ANSI_GREEN + "\n✓ 正确！" + ANSI_RESET);
                } else {
                    console.println(ANSI_RED + "\n✗ 错误！正确答案是: " + q.options[q.correctIndex] + ANSI_RESET);
                }
                
                // 显示解释
                console.println(ANSI_BLUE + "解析: " + q.explanation + ANSI_RESET);
                
            } catch (Exception e) {
                console.println(ANSI_RED + "输入错误: " + e.getMessage() + ANSI_RESET);
                results.add(false);
            }
        }
        
        // 显示结果统计
        showResults(kernel, questions, results);
    }

    /**
     * 显示结果统计
     */
    private static void showResults(Kernel kernel, List<Question> questions, List<Boolean> results) {
        ConsoleDevice console = kernel.getConsole();
        
        int total = questions.size();
        int correct = 0;
        Map<String, Integer> moduleTotal = new HashMap<>();
        Map<String, Integer> moduleCorrect = new HashMap<>();
        
        for (int i = 0; i < total; i++) {
            Question q = questions.get(i);
            moduleTotal.put(q.module, moduleTotal.getOrDefault(q.module, 0) + 1);
            
            if (results.get(i)) {
                correct++;
                moduleCorrect.put(q.module, moduleCorrect.getOrDefault(q.module, 0) + 1);
            }
        }
        
        double accuracy = total > 0 ? (correct * 100.0 / total) : 0;
        
        console.println(ANSI_BOLD + ANSI_CYAN + "\n╔══════════════════════════════════════╗" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "║           答题结果统计               ║" + ANSI_RESET);
        console.println(ANSI_BOLD + ANSI_CYAN + "╚══════════════════════════════════════╝" + ANSI_RESET);
        
        console.println(ANSI_BOLD + "\n总题数: " + total + ANSI_RESET);
        
        if (accuracy >= 80) {
            console.println(ANSI_GREEN + "正确数: " + correct + ANSI_RESET);
        } else if (accuracy >= 60) {
            console.println(ANSI_YELLOW + "正确数: " + correct + ANSI_RESET);
        } else {
            console.println(ANSI_RED + "正确数: " + correct + ANSI_RESET);
        }
        
        console.println(ANSI_BOLD + "正确率: " + String.format("%.1f", accuracy) + "%" + ANSI_RESET);
        
        // 薄弱模块分析
        console.println(ANSI_BOLD + ANSI_CYAN + "\n--- 各模块表现 ---" + ANSI_RESET);
        for (Map.Entry<String, Integer> entry : moduleTotal.entrySet()) {
            String module = entry.getKey();
            int modTotal = entry.getValue();
            int modCorrect = moduleCorrect.getOrDefault(module, 0);
            double modAccuracy = modTotal > 0 ? (modCorrect * 100.0 / modTotal) : 0;
            
            String color = modAccuracy >= 80 ? ANSI_GREEN : (modAccuracy >= 60 ? ANSI_YELLOW : ANSI_RED);
            console.println(color + "  " + module + ": " + modCorrect + "/" + modTotal + " (" + String.format("%.1f", modAccuracy) + "%)" + ANSI_RESET);
        }
        
        // 建议
        console.println(ANSI_BOLD + ANSI_CYAN + "\n--- 学习建议 ---" + ANSI_RESET);
        if (accuracy >= 90) {
            console.println(ANSI_GREEN + "  优秀！你对 Jinux 掌握得很好！" + ANSI_RESET);
        } else if (accuracy >= 70) {
            console.println(ANSI_YELLOW + "  不错！继续巩固薄弱环节。" + ANSI_RESET);
        } else {
            console.println(ANSI_RED + "  建议复习基础知识，重点关注正确率较低的模块。" + ANSI_RESET);
        }
        
        console.println("");
    }

    /**
     * 按模块过滤题目
     */
    private static List<Question> filterByModule(String moduleName) {
        List<Question> filtered = new ArrayList<>();
        for (Question q : allQuestions) {
            if (q.module.equals(moduleName)) {
                filtered.add(q);
            }
        }
        return filtered;
    }

    /**
     * 按难度过滤题目
     */
    private static List<Question> filterByDifficulty(String difficultyName) {
        List<Question> filtered = new ArrayList<>();
        for (Question q : allQuestions) {
            if (q.difficulty.equals(difficultyName)) {
                filtered.add(q);
            }
        }
        return filtered;
    }
}
