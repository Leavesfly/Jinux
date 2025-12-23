# Jinux - Java 实现的 Linux 0.01 操作系统

<div align="center">

```
     _ _                  
    | (_)                 
    | |_ _ __  _   ___  __
 _  | | | '_ \| | | \ \/ /
| |_| | | | | | |_| |>  < 
 \___/|_|_| |_|\__,_/_/\_\
```

**用 Java 重现经典 —— Linux 0.01 的现代实现**

[![Java](https://img.shields.io/badge/Java-17-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.x-blue.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

</div>

---

## 📖 项目简介

Jinux 是一个用 **Java 语言**完整实现的操作系统内核，高度还原了 **Linux 0.01** 的核心架构和功能。本项目的目标是通过面向对象的方式，让开发者更容易理解操作系统的工作原理，非常适合用于教学和学习。

### 为什么选择 Jinux？

- 🎓 **教学友好**：清晰的面向对象设计，易于理解和调试
- 🔍 **高度还原**：忠实模拟 Linux 0.01 的核心机制
- 🚀 **开箱即用**：基于 Maven 构建，一键编译运行
- 📚 **详细注释**：每个模块都有完整的中文注释
- 🎯 **功能完整**：实现了进程管理、内存管理、文件系统、信号、IPC 等核心功能

---

## ✨ 核心特性

### 已实现功能

| 功能模块 | 实现状态 | 完成度 | 说明 |
|---------|---------|--------|------|
| **启动引导** | ✅ | 100% | 系统初始化和引导加载 |
| **进程管理** | ✅ | 95% | 进程调度、fork、exit、wait |
| **内存管理** | ✅ | 90% | 物理内存、虚拟地址空间、页表 |
| **信号机制** | ✅ | 90% | 32种信号、signal/kill 系统调用 |
| **文件系统** | ✅ | 65% | VFS、Inode、SuperBlock、缓冲区 |
| **IPC 管道** | ✅ | 85% | 管道创建、读写、EOF检测 |
| **设备驱动** | ✅ | 75% | 控制台、虚拟磁盘、时钟 |
| **系统调用** | ✅ | 90% | 22+ 个系统调用 |
| **用户态库** | ✅ | 90% | LibC 封装、35+ 函数 |

### 系统架构

```
┌─────────────────────────────────────────────────────┐
│              用户态 (User Space)                      │
│  ┌──────────────────────────────────────────┐       │
│  │  LibC 用户态库                            │       │
│  │  - 进程: fork, exit, wait, getpid         │       │
│  │  - 文件: open, read, write, close         │       │
│  │  - 信号: signal, kill                     │       │
│  │  - IPC: pipe                              │       │
│  │  - 内存: brk, malloc                      │       │
│  └──────────────────────────────────────────┘       │
│              ↓ 系统调用接口                           │
├─────────────────────────────────────────────────────┤
│              内核态 (Kernel Space)                    │
│  ┌──────────────────────────────────────────┐       │
│  │  SystemCallDispatcher (系统调用分发)      │       │
│  └──────────────────────────────────────────┘       │
│              ↓                                       │
│  ┌─────────┬──────────┬──────────┬─────────┐       │
│  │Scheduler│MemoryMgr │   VFS    │ Signal  │       │
│  │ (调度)  │ (内存)   │ (文件)   │ (信号)  │       │
│  └─────────┴──────────┴──────────┴─────────┘       │
│              ↓                                       │
│  ┌──────────────────────────────────────────┐       │
│  │  Device Drivers + IPC                     │       │
│  │  Console │ Disk │ Clock │ Pipe            │       │
│  └──────────────────────────────────────────┘       │
├─────────────────────────────────────────────────────┤
│              硬件抽象层 (模拟)                         │
│  ┌──────────────────────────────────────────┐       │
│  │  PhysicalMemory │ Devices                 │       │
│  └──────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────┘
```

---

## 🚀 快速开始

### 环境要求

- **Java**: JDK 17 或更高版本
- **Maven**: 3.x
- **操作系统**: macOS / Linux / Windows

### 克隆项目

```bash
git clone https://github.com/yourusername/jinux.git
cd jinux/Jinux
```

### 编译运行

```bash
# 设置 JAVA_HOME（macOS 示例）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home

# 编译项目
mvn clean compile

# 运行系统
mvn exec:java
```

### 打包

```bash
# 生成可执行 JAR
mvn package

# 运行 JAR
java -jar target/jinux-0.0.1-SNAPSHOT.jar
```

---

## 📂 项目结构

```
Jinux/
├── src/main/java/jinux/
│   ├── boot/              # 启动引导
│   │   └── Bootstrap.java
│   ├── kernel/            # 内核核心
│   │   ├── Kernel.java
│   │   ├── Scheduler.java
│   │   ├── Task.java
│   │   ├── Signal.java
│   │   └── SystemCallDispatcher.java
│   ├── mm/                # 内存管理
│   │   ├── MemoryManager.java
│   │   ├── PhysicalMemory.java
│   │   ├── AddressSpace.java
│   │   └── PageTable.java
│   ├── fs/                # 文件系统
│   │   ├── VirtualFileSystem.java
│   │   ├── Inode.java
│   │   ├── SuperBlock.java
│   │   ├── BufferCache.java
│   │   ├── File.java
│   │   └── FileDescriptorTable.java
│   ├── ipc/               # 进程间通信
│   │   ├── Pipe.java
│   │   └── PipeFile.java
│   ├── drivers/           # 设备驱动
│   │   ├── Device.java
│   │   ├── ConsoleDevice.java
│   │   ├── VirtualDiskDevice.java
│   │   └── ClockDevice.java
│   ├── init/              # 初始化进程
│   │   └── InitProcess.java
│   ├── lib/               # 用户态库
│   │   ├── LibC.java
│   │   ├── LibCExample.java
│   │   ├── SignalExample.java
│   │   └── PipeExample.java
│   └── include/           # 常量定义
│       ├── Const.java
│       ├── Types.java
│       └── Syscalls.java
├── pom.xml
└── README.md
```

---

## 🎯 核心模块详解

### 1. 进程管理（kernel/）

**核心类：**
- `Task.java` - 进程控制块（PCB），包含进程状态、优先级、时间片等
- `Scheduler.java` - 进程调度器，实现时间片轮转 + 优先级调度

**调度算法：**
```java
// Linux 0.01 原始调度算法
// 选择 counter 最大的可运行进程
// 若所有进程 counter=0，重新分配时间片
counter = counter/2 + priority
```

**系统调用：**
- `fork()` - 创建子进程
- `exit()` - 退出进程
- `wait()` - 等待子进程
- `getpid()` / `getppid()` - 获取进程ID

### 2. 内存管理（mm/）

**核心功能：**
- **物理内存管理**：位图方式管理 16MB 物理内存
- **虚拟地址空间**：每个进程独立的 64MB 虚拟空间
- **页表机制**：两级页表实现虚拟地址到物理地址转换
- **堆管理**：支持 `brk()` 系统调用动态扩展堆

**关键参数：**
- 页面大小：4KB
- 物理内存：16MB (3840 页)
- 虚拟空间：64MB/进程

### 3. 信号机制（kernel/Signal.java）

**支持的信号：**
- 进程控制：`SIGTERM`, `SIGKILL`, `SIGSTOP`, `SIGCONT`
- 异常信号：`SIGSEGV`, `SIGILL`, `SIGFPE`
- 用户信号：`SIGINT`, `SIGQUIT`, `SIGHUP`
- 子进程：`SIGCHLD`
- 管道：`SIGPIPE`

**特性：**
- ✅ 支持 SIG_DFL（默认）、SIG_IGN（忽略）、自定义处理器
- ✅ SIGKILL 和 SIGSTOP 不可捕获/忽略
- ✅ 信号位图和屏蔽位图
- ✅ 默认行为：终止、忽略、停止、核心转储

### 4. 文件系统（fs/）

**架构：**
- **VFS 层**：虚拟文件系统抽象
- **Inode**：索引节点，支持文件/目录/设备
- **SuperBlock**：超级块，管理文件系统元数据
- **BufferCache**：块设备缓冲区缓存

**MINIX 文件系统：**
- 1024 个 Inode
- 10240 个数据块
- 位图管理空闲 Inode 和块

### 5. 管道机制（ipc/Pipe.java）

**特性：**
- 4KB 循环缓冲区
- 阻塞式读写
- 读端/写端引用计数
- EOF 检测（写端关闭时读返回 0）
- SIGPIPE 信号（读端关闭时写失败）

**使用示例：**
```java
// 创建管道
Pipe pipe = new Pipe();

// 写入数据
byte[] data = "Hello".getBytes();
pipe.write(data, data.length);

// 读取数据
byte[] buf = new byte[100];
int n = pipe.read(buf, buf.length);

// 关闭写端
pipe.closeWrite();
```

### 6. 设备驱动（drivers/）

**已实现设备：**
- **ConsoleDevice**：控制台输入输出
- **VirtualDiskDevice**：虚拟磁盘（10MB，文件模拟）
- **ClockDevice**：时钟中断（100 HZ）

### 7. Shell 命令解释器（shell/SimpleShell.java）

**功能特性：**
- ✅ 交互式命令行界面
- ✅ 命令行提示符（`jinux$`）
- ✅ 内置命令支持（10+ 命令）
- ✅ 系统信息查询
- ✅ 信号管理功能

**内置命令：**
```bash
help          # 显示帮助信息
ps            # 显示进程列表
mem           # 显示内存统计
time          # 显示系统时间
signal <pid> <signum>  # 发送信号
kill <pid> <signum>    # 发送信号（同 signal）
demo [type]   # 运行演示（signal/pipe/libc/all）
echo <text>   # 打印文本
clear         # 清屏
uptime        # 显示系统运行时间
version       # 显示版本信息
exit          # 退出 Shell
```

**使用示例：**
```bash
jinux$ help
Available commands:
  help          - Show this help message
  ps            - List all processes
  ...

jinux$ version
Jinux Operating System
  Version: 0.01-alpha
  Shell Version: 0.0.1-alpha
  Java Implementation of Linux 0.01

jinux$ ps
PID	STATE		PRIORITY	COUNTER
----------------------------------------------------
0	RUNNING		15		15

jinux$ signal 0 2
Signal 2 sent to process 0

jinux$ exit
Exiting shell...
```

---

## 🎮 运行示例

### 系统启动输出

```
     _ _                  
    | (_)                 
    | |_ _ __  _   ___  __
 _  | | | '_ \| | | \ \/ /
| |_| | | | | | |_| |>  < 
 \___/|_|_| |_|\__,_/_/\_\

Jinux - A Java Implementation of Linux 0.01
Version 0.01-alpha

========================================
        Jinux Operating System
    (Java Implementation of Linux 0.01)
========================================

[MM] Physical memory initialized: 16MB, 3840 pages free
[KERNEL] Starting kernel initialization...

[CONSOLE] Console device initialized
[DISK] Virtual disk initialized: 10MB
[CLOCK] Clock device initialized: 100 HZ
[VFS] Initializing virtual file system...
[FS] Initialized filesystem: 1024 inodes, 10240 zones

[KERNEL] Kernel initialization complete.
[KERNEL] Creating init process (PID 0)...
[KERNEL] Starting Jinux...

[INIT] Init process started!
[INIT] Jinux operating system is now running.

... (演示输出) ...

[INIT] Starting Simple Shell...
[INIT] You can now interact with Jinux!

========================================
   Jinux Simple Shell v0.0.1-alpha
========================================
Type 'help' for available commands
Type 'exit' to quit

jinux$ _

### LibC 演示

```java
LibC libc = new LibC(syscallDispatcher);

// 进程管理
int pid = libc.getpid();
int child = libc.fork();

// 文件操作
int fd = libc.open("/tmp/test", LibC.O_CREAT | LibC.O_RDWR);
libc.write(fd, data, len);
libc.close(fd);

// 信号处理
libc.signal(Signal.SIGINT, Signal.SIG_IGN);
libc.kill(pid, Signal.SIGTERM);

// 管道通信
int[] fds = new int[2];
libc.pipe(fds);
```

---

## 📊 项目统计

- **文件数量**：33 个 Java 文件
- **代码行数**：6044 行（含注释）
- **注释率**：约 35%
- **系统调用**：22 个
- **信号支持**：32 种
- **Shell 命令**：12 个
- **打包大小**：约 70 KB

---

## 🔧 开发指南

### 添加新系统调用

1. 在 `Syscalls.java` 中定义系统调用号
2. 在 `SystemCallDispatcher.java` 中注册处理器
3. 实现系统调用处理方法
4. 在 `LibC.java` 中添加用户态封装

### 调试技巧

```bash
# 编译时显示详细输出
mvn compile

# 运行时查看详细日志
mvn exec:java

# 使用 IDE 调试
在 IntelliJ IDEA 中直接运行 Bootstrap.main()
```

---

## 📚 学习资源

### Linux 0.01 相关
- [Linux 0.01 源码](https://github.com/karottc/linux-0.01)
- 《Linux 内核完全注释》 - 赵炯
- 《操作系统真象还原》

### Java 实现参考
- 本项目高度还原了 Linux 0.01 的 C 实现
- 使用面向对象的方式重构，更易理解
- 适合作为操作系统课程的实验项目

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

### 开发规范
- 代码注释使用中文
- 遵循 Java 命名规范
- 每个类都要有完整的 Javadoc
- 提交前运行 `mvn clean test`

### 待完善功能
- [ ] execve 系统调用（程序加载）
- [ ] 完整的文件读写实现
- [x] **Shell 命令解释器** ✅ 已实现！
- [ ] 更多进程间通信机制
- [ ] 网络驱动（可选）

---

## 📝 版本历史

### v0.0.1-alpha (2024-12-23)
- ✅ 完成核心内核功能
- ✅ 实现进程管理和调度
- ✅ 实现内存管理（物理/虚拟）
- ✅ 实现信号机制（32种信号）
- ✅ 实现管道 IPC
- ✅ 实现文件系统框架
- ✅ 实现设备驱动
- ✅ 实现 22+ 系统调用
- ✅ 完成 LibC 用户态库
- ✅ **实现 Shell 命令解释器** 🎉

---

## 📄 许可证

本项目采用 MIT 许可证。详见 [LICENSE](LICENSE) 文件。

---

## 🙏 致谢

- Linus Torvalds - Linux 0.01 作者
- 赵炯 - 《Linux 内核完全注释》作者
- 所有为开源操作系统做出贡献的开发者

---

## 📮 联系方式

- **项目主页**：https://github.com/yourusername/jinux
- **问题反馈**：https://github.com/yourusername/jinux/issues

---

<div align="center">

**⭐ 如果这个项目对你有帮助，请给一个 Star！**

Made with ❤️ by Jinux Project Team

</div>
