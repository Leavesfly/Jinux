# Jinux 快速参考指南

## 快速开始

### 1. 编译项目

```bash
cd Jinux
mkdir -p bin
javac -d bin -sourcepath src $(find src -name "*.java")
```

### 2. 运行系统

```bash
java -cp bin jinux.boot.Bootstrap
```

或使用脚本：

```bash
./run.sh
```

### 3. 停止系统

按 `Ctrl+C` 或在另一个终端执行：

```bash
pkill -f "jinux.boot.Bootstrap"
```

## 核心类说明

### 内存管理（mm/）

| 类 | 功能 | 关键方法 |
|---|------|---------|
| PhysicalMemory | 物理内存管理 | allocPage(), freePage() |
| PageTable | 页表管理 | map(), translate() |
| AddressSpace | 地址空间 | allocateAndMap(), expandBrk() |
| MemoryManager | 内存管理器 | createAddressSpace() |

### 进程管理（kernel/）

| 类 | 功能 | 关键方法 |
|---|------|---------|
| Task | 进程控制块 | getPid(), getState(), exit() |
| Scheduler | 进程调度器 | schedule(), addTask(), timerInterrupt() |
| SystemCallDispatcher | 系统调用 | dispatch() |
| Kernel | 内核主类 | init(), start(), shutdown() |

### 设备驱动（drivers/）

| 类 | 功能 | 关键方法 |
|---|------|---------|
| ConsoleDevice | 控制台 | read(), write(), print() |
| VirtualDiskDevice | 虚拟磁盘 | readBlock(), writeBlock() |
| ClockDevice | 时钟设备 | start(), stop() |

## 系统调用参考

### 进程管理

```java
// 获取进程 ID
long pid = syscall.dispatch(SYS_GETPID, 0, 0, 0);

// 获取父进程 ID
long ppid = syscall.dispatch(SYS_GETPPID, 0, 0, 0);

// Fork 创建子进程
long childPid = syscall.dispatch(SYS_FORK, 0, 0, 0);

// 退出进程
syscall.dispatch(SYS_EXIT, exitCode, 0, 0);

// 等待子进程
long childPid = syscall.dispatch(SYS_WAIT, 0, 0, 0);
```

### 文件操作

```java
// 打开文件
long fd = syscall.dispatch(SYS_OPEN, pathPtr, flags, mode);

// 读取文件
long bytesRead = syscall.dispatch(SYS_READ, fd, bufPtr, count);

// 写入文件
long bytesWritten = syscall.dispatch(SYS_WRITE, fd, bufPtr, count);

// 关闭文件
syscall.dispatch(SYS_CLOSE, fd, 0, 0);
```

### 内存管理

```java
// 扩展堆
long newBrk = syscall.dispatch(SYS_BRK, address, 0, 0);
```

### 时间

```java
// 获取系统时间
long timestamp = syscall.dispatch(SYS_TIME, 0, 0, 0);
```

## 常用常量

### 进程状态

```java
TASK_RUNNING           = 0  // 可运行
TASK_INTERRUPTIBLE     = 1  // 可中断睡眠
TASK_UNINTERRUPTIBLE   = 2  // 不可中断睡眠
TASK_ZOMBIE            = 3  // 僵尸状态
TASK_STOPPED           = 4  // 停止
```

### 内存常量

```java
PAGE_SIZE    = 4096        // 4KB
MEMORY_SIZE  = 16 * 1024 * 1024  // 16MB
TASK_SIZE    = 64 * 1024 * 1024  // 64MB
```

### 错误码

```java
E_OK       = 0   // 成功
EPERM      = 1   // 操作不允许
ENOENT     = 2   // 文件不存在
ENOMEM     = 12  // 内存不足
EINVAL     = 22  // 参数无效
```

## 代码示例

### 创建进程

```java
// 分配 PID
int pid = scheduler.allocatePid();

// 创建地址空间
AddressSpace as = memoryManager.createAddressSpace();

// 创建进程
Task task = new Task(pid, ppid, as);

// 添加到调度器
scheduler.addTask(task);
```

### 分配内存

```java
// 分配并映射页面
long vaddr = 0x10000;
addressSpace.allocateAndMap(vaddr, 
    PAGE_PRESENT | PAGE_RW | PAGE_USER);

// 扩展堆
long newBrk = 0x20000;
addressSpace.expandBrk(newBrk);
```

### 调度进程

```java
// 手动调度
scheduler.schedule();

// 时钟中断（自动调度）
scheduler.timerInterrupt();
```

## 调试技巧

### 打印进程列表

```java
scheduler.printProcessList();
```

输出：
```
PID  PPID  STATE      COUNTER  PRIORITY
0    0     RUNNING    10       15
```

### 打印内存统计

```java
memoryManager.printStats();
```

输出：
```
[MM] Memory: 256/4096 pages used, 1MB/15MB free
```

### 查看进程信息

```java
System.out.println(task);
```

输出：
```
Task[pid=0, ppid=0, state=RUNNING, counter=10, priority=15]
```

## 项目结构

```
Jinux/
├── src/jinux/          # 源代码
│   ├── boot/          # 启动
│   ├── kernel/        # 内核
│   ├── mm/            # 内存
│   ├── fs/            # 文件系统
│   ├── drivers/       # 驱动
│   ├── include/       # 定义
│   └── init/          # 初始化
├── bin/               # 编译输出
├── run.sh             # 启动脚本
├── JINUX_OVERVIEW.md  # 项目概览
└── PROJECT_SUMMARY.md # 实施总结
```

## 常见问题

### Q: 如何修改内存大小？

A: 修改 `include/Const.java` 中的 `MEMORY_SIZE` 常量。

### Q: 如何修改进程数量？

A: 修改 `include/Const.java` 中的 `NR_TASKS` 常量。

### Q: 如何修改时钟频率？

A: 修改 `include/Const.java` 中的 `HZ` 常量（默认 100 HZ）。

### Q: 磁盘镜像文件在哪里？

A: 默认在项目根目录下的 `jinux-disk.img` 文件。

### Q: 如何添加新的系统调用？

A: 
1. 在 `include/Syscalls.java` 中添加系统调用号
2. 在 `SystemCallDispatcher.java` 中注册处理器
3. 实现系统调用处理方法

## 性能参数

- **调度频率**: 100 HZ（每 10ms 一次）
- **默认时间片**: 10 个 tick
- **默认优先级**: 15
- **最大进程数**: 64
- **物理内存**: 16 MB
- **虚拟地址空间**: 64 MB/进程
- **页面大小**: 4 KB
- **磁盘大小**: 10 MB（可配置）

## 扩展开发

### 添加新设备

1. 继承 `Device`、`CharDevice` 或 `BlockDevice`
2. 实现必要的方法
3. 在 `Kernel.init()` 中初始化

### 实现新系统调用

1. 定义系统调用号（`Syscalls.java`）
2. 实现处理器（`SystemCallDispatcher.java`）
3. 注册到 handlers 映射

### 创建用户进程

1. 实现 `Runnable` 接口
2. 创建 `Task` 并设置 executable
3. 添加到调度器

---

**更多信息**: 查看 JINUX_OVERVIEW.md 和 PROJECT_SUMMARY.md
