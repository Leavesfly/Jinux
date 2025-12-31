# Jinux 核心概念详解

**版本**: 1.0  
**最后更新**: 2024-12-23

---

## 📚 目录

1. [进程管理](#进程管理)
2. [内存管理](#内存管理)
3. [系统调用](#系统调用)
4. [信号机制](#信号机制)
5. [进程间通信](#进程间通信)
6. [文件系统](#文件系统)
7. [设备驱动](#设备驱动)

---

## 进程管理

### 1.1 进程控制块（PCB/Task）

**概念**：进程控制块是操作系统用来描述和管理进程的数据结构，包含了进程的所有状态信息。

**在 Jinux 中的实现**：

```java
class Task {
    int pid;              // 进程 ID
    int ppid;             // 父进程 ID
    int state;            // 进程状态
    int counter;          // 时间片计数器
    int priority;         // 优先级
    AddressSpace addressSpace;  // 地址空间
    FileDescriptorTable fdTable; // 文件描述符表
    long signalPending;   // 待处理信号位图
    SignalHandlerEntry[] signalHandlers; // 信号处理器
}
```

**关键字段说明**：

- **pid/ppid**：唯一标识进程和进程关系
- **state**：进程当前状态（RUNNING、SLEEPING、ZOMBIE 等）
- **counter**：剩余时间片，用于调度
- **priority**：进程优先级，影响时间片分配
- **addressSpace**：进程的虚拟地址空间
- **fdTable**：进程打开的文件列表

**学习要点**：
- 理解每个字段的作用
- 理解进程状态转换
- 理解进程间的关系（父子进程）

### 1.2 进程状态

**状态定义**：

```java
TASK_RUNNING = 0          // 可运行（正在运行或等待运行）
TASK_INTERRUPTIBLE = 1    // 可中断睡眠（等待事件）
TASK_UNINTERRUPTIBLE = 2  // 不可中断睡眠（等待 I/O）
TASK_ZOMBIE = 3           // 僵尸状态（已退出但未回收）
TASK_STOPPED = 4          // 停止状态（收到 SIGSTOP）
```

**状态转换图**：

```
创建 → RUNNING → INTERRUPTIBLE → RUNNING
  ↓        ↓
ZOMBIE ← exit()    sleep() → INTERRUPTIBLE
  ↓
回收 ← wait()
```

**关键理解**：
- **RUNNING**：进程可以执行，但不一定正在执行（可能等待调度）
- **INTERRUPTIBLE**：进程等待事件，可以被信号唤醒
- **ZOMBIE**：进程已退出，但父进程还未调用 wait() 回收
- **STOPPED**：进程被信号暂停，需要 SIGCONT 恢复

### 1.3 进程调度

**调度算法**：时间片轮转 + 优先级调度

**算法原理**：

```java
// Linux 0.01 原始调度算法
// 1. 选择 counter 最大的可运行进程
// 2. 若所有进程 counter=0，重新分配时间片
counter = counter/2 + priority
```

**调度时机**：
1. 时钟中断（每 10ms）
2. 进程主动让出 CPU（sleep、wait）
3. 进程退出（exit）

**时间片分配**：
- 基础时间片 = priority
- 每次调度时，counter 减 1
- counter 为 0 时，进程被抢占

**学习要点**：
- 理解调度算法的公平性
- 理解优先级的作用
- 理解抢占式调度的意义

### 1.4 Fork 系统调用

**功能**：创建子进程，子进程是父进程的副本。

**执行流程**：

```
1. 分配新的 PID
2. 创建新的 Task 对象
3. 复制地址空间（COW 优化）
4. 复制文件描述符表
5. 复制信号处理器
6. 设置父子进程关系
7. 将子进程加入调度队列
8. 返回子进程 PID（父进程）或 0（子进程）
```

**写时复制（COW）优化**：

```
fork() 时：
- 不立即复制物理页面
- 父子进程共享物理页面
- 页面标记为只读和 COW
- 增加页面引用计数

写入时：
- 检测到 COW 标记
- 检查引用计数
- 如果 > 1：复制页面，更新映射
- 如果 = 1：直接修改，清除 COW 标记
```

**学习要点**：
- 理解 fork 的返回值（为什么父进程返回子进程 PID，子进程返回 0）
- 理解 COW 优化的原理和好处
- 理解父子进程的资源共享

---

## 内存管理

### 2.1 物理内存管理

**管理方式**：位图（Bitmap）

**实现原理**：

```java
class PhysicalMemory {
    byte[] memory;        // 16MB 物理内存
    BitSet pageBitmap;   // 页面分配位图（3840 页）
    int[] pageRefCount;  // 页面引用计数（用于 COW）
}
```

**页面分配**：
- 页面大小：4KB
- 总页面数：3840 页（16MB / 4KB）
- 位图：每个位表示一个页面的分配状态
- 引用计数：支持多个进程共享页面（COW）

**分配算法**：
1. 在位图中查找空闲页面
2. 标记页面为已分配
3. 初始化页面引用计数为 1
4. 返回物理页号

**学习要点**：
- 理解位图管理的高效性
- 理解引用计数的作用
- 理解内存碎片问题

### 2.2 虚拟地址空间

**地址空间布局**（每个进程 64MB）：

```
0x00000000 ┌─────────────────┐
           │   代码段         │  (Code Segment)
           │   (只读)         │
           ├─────────────────┤
           │   数据段         │  (Data Segment)
           │   (可读写)       │
           ├─────────────────┤
           │   堆 (Heap)      │  ← brk（可扩展）
           │   (向上增长)     │
           │                 │
           │   ...           │
           │                 │
           ├─────────────────┤
           │   栈 (Stack)    │  ← stackTop（向下增长）
           │   (向下增长)     │
0x04000000 └─────────────────┘
```

**关键地址**：
- **codeStart/codeEnd**：代码段范围
- **dataStart/dataEnd**：数据段范围
- **brk**：堆结束地址（可通过 brk 系统调用扩展）
- **stackTop**：栈顶地址

**学习要点**：
- 理解为什么需要虚拟地址空间
- 理解不同段的权限（代码段只读）
- 理解堆和栈的增长方向

### 2.3 页表机制

**功能**：将虚拟地址转换为物理地址

**实现方式**（简化）：

```java
class PageTable {
    Map<Long, Long> mappings;  // 虚拟页号 -> 物理页号
    Map<Long, Integer> flags; // 页面权限标志
}
```

**页面标志**：
- **PAGE_PRESENT**：页面存在
- **PAGE_RW**：页面可写
- **PAGE_USER**：用户态可访问
- **PAGE_COW**：写时复制标记

**地址转换**：

```
虚拟地址 = 虚拟页号 * PAGE_SIZE + 页内偏移
物理地址 = 物理页号 * PAGE_SIZE + 页内偏移
```

**学习要点**：
- 理解虚拟地址和物理地址的区别
- 理解页表的作用
- 理解页面权限控制

### 2.4 写时复制（COW）

**原理**：fork 时不立即复制页面，而是共享页面，直到需要写入时才复制。

**实现步骤**：

1. **fork 时**：
   ```java
   // 共享物理页面
   pageTable.map(vaddr, paddr);
   // 标记为只读和 COW
   flags = PAGE_PRESENT | PAGE_COW;
   // 增加引用计数
   physicalMemory.incrementPageRef(paddr);
   ```

2. **写入时**：
   ```java
   // 检测到 COW 标记
   if (flags & PAGE_COW) {
       // 检查引用计数
       if (refCount > 1) {
           // 复制页面
           newPaddr = allocatePage();
           copyPage(paddr, newPaddr);
           // 更新映射
           pageTable.map(vaddr, newPaddr);
           // 清除 COW 标记
           flags &= ~PAGE_COW;
       }
   }
   ```

**优势**：
- fork 操作从 O(n) 变为 O(1)
- 节省内存（多个进程共享只读页面）
- 提高性能（减少内存复制）

**学习要点**：
- 理解 COW 的触发时机
- 理解引用计数的作用
- 理解 COW 的性能优势

---

## 系统调用

### 3.1 系统调用接口

**概念**：用户程序与内核交互的接口

**调用流程**：

```
用户程序
  ↓
LibC 封装（如 fork()）
  ↓
SystemCallDispatcher.dispatch()
  ↓
系统调用处理函数（如 sysFork()）
  ↓
内核模块（如 Scheduler）
  ↓
返回结果
```

**参数传递**：

```java
long result = syscallDispatcher.dispatch(
    syscallNr,  // 系统调用号
    arg1,       // 第一个参数
    arg2,       // 第二个参数
    arg3        // 第三个参数
);
```

**用户空间数据拷贝**：

```java
// 从用户空间读取字符串
String path = copyStringFromUser(pathPtr);

// 向用户空间写入数据
copyToUser(bufPtr, data, length);
```

**学习要点**：
- 理解用户态和内核态的边界
- 理解为什么需要数据拷贝
- 理解系统调用的安全性

### 3.2 系统调用分发

**分发机制**：

```java
class SystemCallDispatcher {
    Map<Integer, SystemCallHandler> handlers;
    
    long dispatch(int syscallNr, long arg1, long arg2, long arg3) {
        // 1. 获取当前进程
        Task current = scheduler.getCurrentTask();
        
        // 2. 查找处理器
        SystemCallHandler handler = handlers.get(syscallNr);
        
        // 3. 调用处理函数
        return handler.handle(current, arg1, arg2, arg3);
    }
}
```

**已实现的系统调用**（28 个）：

- **进程管理**：fork, exit, wait, getpid, getppid, pause, execve
- **信号管理**：signal, kill
- **文件操作**：read, write, open, close, lseek, creat, unlink
- **目录操作**：chdir, mkdir, rmdir
- **文件状态**：stat, fstat
- **内存管理**：brk
- **时间**：time, times
- **IPC**：pipe
- **文件描述符**：dup, dup2
- **文件系统**：sync

**学习要点**：
- 理解系统调用号的分配
- 理解参数传递的限制（最多 3 个参数）
- 理解错误处理机制

---

## 信号机制

### 3.1 信号概念

**定义**：信号是进程间通信的一种方式，用于通知进程发生了某个事件。

**信号类型**（32 种标准信号）：

- **进程控制**：SIGTERM（终止）、SIGKILL（强制终止）、SIGSTOP（停止）、SIGCONT（继续）
- **异常信号**：SIGSEGV（段错误）、SIGILL（非法指令）、SIGFPE（浮点异常）
- **用户信号**：SIGINT（中断，Ctrl+C）、SIGQUIT（退出）、SIGHUP（挂起）
- **子进程**：SIGCHLD（子进程状态改变）
- **管道**：SIGPIPE（管道破裂）

**信号处理方式**：

- **SIG_DFL**：默认行为（终止、忽略、停止等）
- **SIG_IGN**：忽略信号
- **自定义处理器**：用户定义的函数

**特殊信号**：
- **SIGKILL** 和 **SIGSTOP**：不可被捕获或忽略

### 3.2 信号处理流程

**发送信号**：

```
进程 A 调用 kill(pid, SIGINT)
  ↓
sysKill() 系统调用
  ↓
查找目标进程
  ↓
设置信号位图（signalPending）
  ↓
如果进程在睡眠，唤醒进程
```

**处理信号**：

```
调度器检查待处理信号
  ↓
查找信号处理器
  ↓
SIG_IGN：忽略信号
SIG_DFL：执行默认行为（终止/停止）
自定义处理器：执行用户代码
```

**信号位图**：

```java
long signalPending;  // 待处理信号位图（64 位）
long signalBlocked; // 信号屏蔽位图（64 位）

// 设置信号
signalPending |= (1L << signum);

// 检查信号
if ((signalPending & (1L << signum)) != 0) {
    // 处理信号
}
```

**学习要点**：
- 理解信号的异步特性
- 理解信号处理的安全问题
- 理解信号屏蔽的作用

---

## 进程间通信

### 4.1 管道（Pipe）

**概念**：管道是一种半双工通信机制，数据只能单向流动。

**实现**：

```java
class Pipe {
    byte[] buffer;      // 4KB 循环缓冲区
    int readPos;       // 读位置
    int writePos;      // 写位置
    int count;         // 数据量
    int readRefs;      // 读端引用计数
    int writeRefs;     // 写端引用计数
}
```

**操作**：

- **写入**：如果缓冲区满，阻塞等待
- **读取**：如果缓冲区空，阻塞等待
- **关闭写端**：读操作返回 EOF（0）
- **关闭读端**：写操作触发 SIGPIPE

**阻塞机制**：

```java
// 写入时
while (count == buffer.length) {
    // 缓冲区满，等待
    Thread.sleep(10);
}

// 读取时
while (count == 0 && writeRefs > 0) {
    // 缓冲区空且写端未关闭，等待
    Thread.sleep(10);
}
```

**学习要点**：
- 理解管道的单向性
- 理解阻塞机制的作用
- 理解 EOF 和 SIGPIPE 的处理

---

## 文件系统

### 5.1 VFS（虚拟文件系统）

**概念**：VFS 是文件系统的抽象层，提供统一的文件操作接口。

**核心功能**：

- **路径解析**（namei）：将路径名转换为 inode
- **目录查找**（lookup）：在目录中查找文件
- **文件创建**（createFile）：创建新文件
- **目录创建**（createDirectory）：创建目录
- **文件删除**（unlink）：删除文件/目录

**路径解析流程**：

```
"/tmp/file"
  ↓
解析路径组件：["", "tmp", "file"]
  ↓
从根目录开始查找
  ↓
查找 "tmp" 目录
  ↓
在 "tmp" 目录中查找 "file"
  ↓
返回 file 的 inode
```

### 5.2 Inode（索引节点）

**概念**：Inode 存储文件的元数据和数据块指针。

**结构**：

```java
class Inode {
    int ino;                    // inode 号
    int mode;                   // 文件类型和权限
    long size;                  // 文件大小
    int[] directBlocks;         // 10 个直接块指针
    int indirectBlock;          // 一级间接块指针
    int doubleIndirectBlock;    // 二级间接块指针
    long atime, mtime, ctime;   // 时间戳
}
```

**块指针结构**：

```
直接块（10 个）：直接指向数据块
一级间接块：指向一个块，该块包含 1024 个块指针
二级间接块：指向一个块，该块包含 1024 个一级间接块指针
```

**文件大小计算**：
- 直接块：10 * 4KB = 40KB
- 一级间接：1024 * 4KB = 4MB
- 二级间接：1024 * 1024 * 4KB = 4GB

### 5.3 SuperBlock（超级块）

**功能**：管理文件系统的元数据

**内容**：
- Inode 总数和数据块总数
- Inode 位图和数据块位图
- Inode 分配和释放
- 数据块分配和释放

**位图管理**：
- Inode 位图：标记哪些 inode 已分配
- 数据块位图：标记哪些数据块已分配

### 5.4 缓冲区缓存

**功能**：缓存磁盘块，减少磁盘 I/O

**策略**：LRU（最近最少使用）

**实现**：

```java
class BufferCache {
    Map<Integer, Buffer> cache;  // 块号 -> 缓冲区
    LinkedList<Buffer> lruList; // LRU 链表
    
    Buffer getBlock(int blockNo) {
        // 1. 查找缓存
        Buffer buf = cache.get(blockNo);
        if (buf != null) {
            // 命中，移到链表头部
            lruList.remove(buf);
            lruList.addFirst(buf);
            return buf;
        }
        
        // 2. 缓存未命中，从磁盘读取
        buf = readFromDisk(blockNo);
        
        // 3. 加入缓存（如果缓存满，淘汰最旧的）
        if (cache.size() >= MAX_CACHE_SIZE) {
            Buffer oldest = lruList.removeLast();
            cache.remove(oldest.blockNo);
        }
        cache.put(blockNo, buf);
        lruList.addFirst(buf);
        
        return buf;
    }
}
```

**学习要点**：
- 理解缓存的作用
- 理解 LRU 策略
- 理解脏块写回机制

---

## 设备驱动

### 6.1 设备抽象

**设备类型**：

- **字符设备**：按字节访问（如控制台）
- **块设备**：按块访问（如磁盘）

**设备接口**：

```java
interface Device {
    void init();
    void close();
}

interface CharDevice extends Device {
    int read(byte[] buf, int len);
    int write(byte[] buf, int len);
}

interface BlockDevice extends Device {
    int readBlock(int blockNo, byte[] buf);
    int writeBlock(int blockNo, byte[] buf);
}
```

### 6.2 控制台设备

**实现**：使用 System.in/out 模拟

**功能**：
- 标准输入输出
- 字符读写

### 6.3 虚拟磁盘设备

**实现**：使用文件模拟磁盘

**特性**：
- 10MB 大小
- 4KB 块大小
- 支持块读写操作

### 6.4 时钟设备

**功能**：定时器中断

**实现**：
- 100 HZ 频率（每 10ms 触发一次）
- 使用 Java Timer 模拟
- 触发进程调度

**学习要点**：
- 理解设备抽象的作用
- 理解字符设备和块设备的区别
- 理解时钟中断的作用

---

## 总结

通过理解这些核心概念，你将能够：

1. **理解操作系统的基本原理**
2. **掌握 Jinux 的实现细节**
3. **具备修改和扩展系统的能力**
4. **为深入学习 Linux 内核打下基础**

**下一步**：
- 阅读 `docs/CODE_READING_GUIDE.md` 学习代码阅读技巧
- 阅读 `docs/PRACTICE_EXERCISES.md` 进行实践练习
- 参考 `docs/FAQ.md` 解决常见问题

---

**文档版本**: 1.0  
**最后更新**: 2024-12-23  
**维护者**: Jinux Project Team

