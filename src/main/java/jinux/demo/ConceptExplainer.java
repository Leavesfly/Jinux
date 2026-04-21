package jinux.demo;

import jinux.drivers.ConsoleDevice;
import jinux.kernel.Kernel;

/**
 * 概念解释器 - 用简单的语言和生活类比来解释操作系统的核心概念
 */
public class ConceptExplainer {

    // ANSI 颜色常量
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_YELLOW = "\033[33m";
    private static final String ANSI_BLUE = "\033[34m";
    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_BOLD = "\033[1m";

    /**
     * 根据主题显示解释
     * @param kernel 内核实例
     * @param topic 主题名称（不区分大小写）
     */
    public static void explain(Kernel kernel, String topic) {
        ConsoleDevice console = kernel.getConsole();
        
        if (topic == null || topic.isEmpty()) {
            console.println(ANSI_RED + "错误: 请指定一个主题" + ANSI_RESET);
            showTopicList(kernel);
            return;
        }

        String lowerTopic = topic.toLowerCase();
        
        switch (lowerTopic) {
            case "process":
                explainProcess(console);
                break;
            case "scheduling":
                explainScheduling(console);
                break;
            case "memory":
                explainMemory(console);
                break;
            case "virtual-memory":
            case "virtual_memory":
                explainVirtualMemory(console);
                break;
            case "syscall":
                explainSyscall(console);
                break;
            case "signal":
                explainSignal(console);
                break;
            case "pipe":
                explainPipe(console);
                break;
            case "filesystem":
            case "file-system":
            case "file_system":
                explainFilesystem(console);
                break;
            case "fork":
                explainFork(console);
                break;
            case "inode":
                explainInode(console);
                break;
            case "page-table":
            case "page_table":
            case "pagetable":
                explainPageTable(console);
                break;
            case "context-switch":
            case "context_switch":
            case "contextswitch":
                explainContextSwitch(console);
                break;
            default:
                console.println(ANSI_RED + "错误: 未知的主题 '" + topic + "'" + ANSI_RESET);
                console.println("");
                console.println("可用的主题列表:");
                showTopicList(kernel);
                break;
        }
    }

    /**
     * 显示所有可解释的主题列表
     * @param kernel 内核实例
     */
    public static void showTopicList(Kernel kernel) {
        ConsoleDevice console = kernel.getConsole();
        
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "📚 Jinux 操作系统概念主题列表" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        console.println("  1. " + ANSI_GREEN + "process" + ANSI_RESET + "           - 什么是进程");
        console.println("  2. " + ANSI_GREEN + "scheduling" + ANSI_RESET + "        - 进程调度算法");
        console.println("  3. " + ANSI_GREEN + "memory" + ANSI_RESET + "            - 内存管理机制");
        console.println("  4. " + ANSI_GREEN + "virtual-memory" + ANSI_RESET + "    - 虚拟内存原理");
        console.println("  5. " + ANSI_GREEN + "syscall" + ANSI_RESET + "           - 系统调用接口");
        console.println("  6. " + ANSI_GREEN + "signal" + ANSI_RESET + "            - 信号通知机制");
        console.println("  7. " + ANSI_GREEN + "pipe" + ANSI_RESET + "              - 管道通信方式");
        console.println("  8. " + ANSI_GREEN + "filesystem" + ANSI_RESET + "        - 文件系统结构");
        console.println("  9. " + ANSI_GREEN + "fork" + ANSI_RESET + "              - 进程创建方法");
        console.println(" 10. " + ANSI_GREEN + "inode" + ANSI_RESET + "             - 索引节点概念");
        console.println(" 11. " + ANSI_GREEN + "page-table" + ANSI_RESET + "        - 页表映射机制");
        console.println(" 12. " + ANSI_GREEN + "context-switch" + ANSI_RESET + "    - 上下文切换过程");
        console.println("");
        console.println(ANSI_YELLOW + "💡 使用方法: explain(kernel, \"主题名\")" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    // ==================== 各个主题的解释方法 ====================

    /**
     * 解释进程概念
     */
    private static void explainProcess(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 什么是进程 (Process)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   进程就像餐厅里的一位厨师在做一道菜。菜谱（程序）是静态的文字，");
        console.println("   而厨师做菜的过程（进程）是动态的活动。同一个菜谱可以让多个厨师");
        console.println("   同时做（多个进程运行同一个程序）。每个厨师有自己的工作台（地址");
        console.println("   空间）、食材进度（寄存器状态）和工作证（PID）。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   进程是程序的一次执行实例，拥有独立的地址空间、程序计数器、");
        console.println("   寄存器集合和系统资源。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   Task.java 是进程控制块(PCB)，包含 pid、state、priority、");
        console.println("   counter、addressSpace 等字段。进程状态有 RUNNING、");
        console.println("   READY(INTERRUPTIBLE)、WAITING(UNINTERRUPTIBLE)、ZOMBIE、STOPPED。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/kernel/Task.java");
        console.println("   src/main/java/jinux/kernel/Scheduler.java");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   阅读 Task.java 了解进程控制块的结构，然后查看 Scheduler.java");
        console.println("   理解进程如何被调度和管理。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释进程调度
     */
    private static void explainScheduling(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 进程调度 (Scheduling)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   调度就像一个游乐场的旋转木马管理员。每个小朋友（进程）轮流骑");
        console.println("   一会儿（时间片），时间到了就换下一个。VIP小朋友（高优先级）可以");
        console.println("   多骑一会儿。当所有人都骑完一轮，管理员重新发放骑行券（重新计算");
        console.println("   counter）。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   Linux 0.01 使用基于优先级的时间片轮转调度。每次选择 counter");
        console.println("   值最大的就绪进程运行。当所有进程 counter=0 时，按");
        console.println("   counter = counter/2 + priority 重新计算。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   Scheduler.java 的 schedule() 方法实现了这个算法。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/kernel/Scheduler.java");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   重点阅读 Scheduler.schedule() 方法，理解如何选择下一个要运行的进程。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释内存管理
     */
    private static void explainMemory(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 内存管理 (Memory)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   物理内存就像一栋公寓楼，每个房间（页面）大小相同（4KB）。操作");
        console.println("   系统是物业管理员，用一本登记簿（位图）记录哪些房间有人住、哪些");
        console.println("   空着。新住户来了就分配空房间，搬走了就标记为空闲。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   Jinux 使用位图管理 16MB 物理内存（3840个4KB页面）。每一位代表");
        console.println("   一个页面，0=空闲，1=已用。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   PhysicalMemory.java 管理物理页面，MemoryManager.java 提供高层接口。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/mm/PhysicalMemory.java");
        console.println("   src/main/java/jinux/mm/MemoryManager.java");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   先阅读 PhysicalMemory.java 理解位图管理，再看 MemoryManager.java");
        console.println("   了解内存分配的高层接口。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释虚拟内存
     */
    private static void explainVirtualMemory(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 虚拟内存 (Virtual Memory)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   虚拟内存就像每个住户拿到的\"楼层平面图\"。虽然整栋楼只有100个房");
        console.println("   间（物理内存），但每个住户的平面图上画了1000个房间（虚拟地址空");
        console.println("   间）。页表就是一本\"房间号对照表\"，把平面图上的房间号翻译成实际");
        console.println("   的房间号。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   每个进程拥有独立的 64MB 虚拟地址空间，通过两级页表映射到物理内存。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   AddressSpace.java 管理虚拟地址空间，PageTable.java 实现地址映射。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/mm/AddressSpace.java");
        console.println("   src/main/java/jinux/mm/PageTable.java");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   理解虚拟地址如何通过页表转换为物理地址，这是现代操作系统的核心机制。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释系统调用
     */
    private static void explainSyscall(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 系统调用 (System Call)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   系统调用就像去银行柜台办业务。你（用户程序）不能直接进金库（操");
        console.println("   作硬件），必须填一张申请单（系统调用号+参数），递给柜台（系统调");
        console.println("   用接口），由银行职员（内核）帮你操作。办完后把结果（返回值）交还给你。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   系统调用是用户态程序请求内核服务的标准接口。Jinux 实现了 22+ 个系统调用。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   SystemCallDispatcher.java 接收调用请求，分发给 ProcessSyscalls、");
        console.println("   FileSyscalls、SignalSyscalls、IpcSyscalls、MiscSyscalls 五个子处理器。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/kernel/SystemCallDispatcher.java");
        console.println("   src/main/java/jinux/kernel/syscall/");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   从 SystemCallDispatcher.java 开始，了解系统调用的分发机制。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释信号机制
     */
    private static void explainSignal(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 信号机制 (Signal)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   信号就像手机的通知推送。有些通知你可以忽略（SIG_IGN），有些你");
        console.println("   可以自定义铃声（自定义处理器），有些是默认铃声（SIG_DFL）。但\"强");
        console.println("   制关机\"通知（SIGKILL）你没法屏蔽——系统说关就关。信号位图就像通");
        console.println("   知栏，记录着哪些通知还没处理。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   信号是一种异步通知机制，用于通知进程发生了某个事件。Jinux 支持 32 种信号。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   Signal.java 定义信号常量和处理逻辑，Task 中的 signalPending 位图记录待处理信号。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/kernel/Signal.java");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   阅读 Signal.java 了解信号的发送和处理流程。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释管道通信
     */
    private static void explainPipe(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 管道通信 (Pipe)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   管道就像一根水管连接两个水箱。一端只能往里倒水（写端），另一端只");
        console.println("   能接水（读端）。水管有固定容量（4KB缓冲区），满了就倒不进去（写阻");
        console.println("   塞），空了就接不到水（读阻塞）。如果接水端被堵死了（读端关闭），继");
        console.println("   续倒水会溢出报警（SIGPIPE）。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   管道是一种半双工的 IPC 机制，使用 4KB 环形缓冲区实现数据传输。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   Pipe.java 实现环形缓冲区，PipeFile.java 将管道封装为文件接口。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/ipc/Pipe.java");
        console.println("   src/main/java/jinux/ipc/PipeFile.java");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   理解环形缓冲区的读写指针管理机制。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释文件系统
     */
    private static void explainFilesystem(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 文件系统 (Filesystem)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   文件系统就像一个图书馆的管理系统。SuperBlock 是图书馆的总目录");
        console.println("   （记录有多少书架、多少书）。Inode 是每本书的索引卡（记录作者、");
        console.println("   页数、存放位置，但不记录书名）。目录就是书架上的标签，把书名和");
        console.println("   索引卡号对应起来。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   Jinux 实现了 MINIX 文件系统，包含 1024 个 Inode 和 10240 个数");
        console.println("   据块。VFS 层提供统一的文件操作接口。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   VirtualFileSystem.java 是 VFS 层，Inode.java 管理文件元数据，");
        console.println("   SuperBlock.java 管理文件系统全局信息。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/fs/VirtualFileSystem.java");
        console.println("   src/main/java/jinux/fs/Inode.java");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   从 VFS 层开始理解文件系统的抽象，再深入 Inode 的具体实现。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释 Fork 创建进程
     */
    private static void explainFork(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: Fork 创建进程" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   fork 就像用复印机复印一份完整的文件。复印出来的（子进程）和原件");
        console.println("   （父进程）内容完全一样，但之后各自修改互不影响。聪明的做法是\"写");
        console.println("   时复制\"（COW）——先让两份文件共享同一叠纸，谁要改哪一页，谁就单");
        console.println("   独复印那一页。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   fork() 创建当前进程的副本。父进程返回子进程PID，子进程返回0。");
        console.println("   使用 COW 优化内存使用。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   Scheduler.fork() 方法实现进程复制，AddressSpace.copy() 实现地");
        console.println("   址空间的 COW 复制。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/kernel/Scheduler.java (fork方法)");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   理解 COW 机制如何优化内存使用，这是现代操作系统的重要优化。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释索引节点
     */
    private static void explainInode(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 索引节点 (Inode)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   Inode 就像快递单号系统。你不需要知道包裹在仓库的哪个货架上（数");
        console.println("   据块位置），只需要报单号（inode号），系统就能找到你的包裹。单号上");
        console.println("   还记录了包裹的重量（文件大小）、寄件人（所有者）、寄件时间（时间戳）等信息。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   Inode 存储文件的元数据（大小、权限、时间戳、数据块指针），不包含文件名。");
        console.println("   文件名存储在目录项中。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   Inode.java 包含 mode、size、zone[]（数据块指针）等字段。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/fs/Inode.java");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   理解为什么文件名不在 Inode 中，这与硬链接的实现有关。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释页表
     */
    private static void explainPageTable(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 页表 (Page Table)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   页表就像一本翻译词典。虚拟地址是外语单词，物理地址是中文翻译。");
        console.println("   CPU 每次访问内存都要查这本词典。为了加快查找，词典分成两级：先查");
        console.println("   大目录（页目录）找到小章节，再在小章节（页表）里找到具体翻译。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   两级页表将虚拟地址分为页目录索引(10位)、页表索引(10位)、页内");
        console.println("   偏移(12位)三部分。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   PageTable.java 实现两级页表结构。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/mm/PageTable.java");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   理解两级页表如何减少内存占用，以及TLB的作用。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }

    /**
     * 解释上下文切换
     */
    private static void explainContextSwitch(ConsoleDevice console) {
        console.println("");
        console.println(ANSI_BOLD + ANSI_BLUE + "🎓 主题: 上下文切换 (Context Switch)" + ANSI_RESET);
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
        
        console.println(ANSI_YELLOW + "🎯 生活类比" + ANSI_RESET);
        console.println("   上下文切换就像一个学生在做数学作业和英语作业之间切换。切换前要");
        console.println("   把数学书签夹好、记住做到第几题（保存CPU寄存器）。然后拿出英语书");
        console.println("   翻到上次的位置（恢复寄存器）。切换本身不产出任何作业（纯开销），");
        console.println("   所以切换越少越好。");
        console.println("");
        
        console.println(ANSI_CYAN + "📖 技术定义" + ANSI_RESET);
        console.println("   上下文切换是保存当前进程的CPU状态并恢复下一个进程的CPU状态的过程。");
        console.println("");
        
        console.println("💻 Jinux 中的实现");
        console.println("   Scheduler.switchTo() 方法实现上下文切换。");
        console.println("");
        
        console.println(ANSI_GREEN + "📂 关键代码" + ANSI_RESET);
        console.println("   src/main/java/jinux/kernel/Scheduler.java (switchTo方法)");
        console.println("");
        
        console.println(ANSI_YELLOW + "💡 学习建议" + ANSI_RESET);
        console.println("   理解为什么上下文切换是性能开销，以及如何优化。");
        console.println(ANSI_BLUE + "═══════════════════════════════════════════" + ANSI_RESET);
        console.println("");
    }
}
