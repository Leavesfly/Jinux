# Jinux 演示指南

本文档介绍 Jinux 启动后可以运行的各种演示程序，展示系统的核心能力。

## 概述

Jinux 提供了丰富的演示程序来展示操作系统的各种核心功能，包括：

- **系统能力综合演示** - 全面展示系统各项功能
- **交互式演示** - 模拟实际使用场景
- **模块化演示** - 针对特定功能的深入演示

## 启动时的自动演示

当 Jinux 启动后，系统会自动运行一系列演示：

1. **LibC 用户态库演示** - 展示用户态库的使用
2. **信号机制演示** - 展示信号处理能力
3. **管道机制演示** - 展示进程间通信
4. **进程创建演示** - 展示进程管理
5. **系统调用演示** - 展示系统调用接口
6. **进程管理演示** - 展示进程调度
7. **内存管理演示** - 展示内存分配
8. **系统能力综合演示** - 全面功能展示
9. **交互式演示** - 实际场景模拟

## Shell 中的演示命令

在 Shell 中可以使用 `demo` 命令运行各种演示：

### 基本用法

```bash
jinux$ demo [type]
```

### 可用的演示类型

#### 1. `demo signal` - 信号机制演示

展示信号处理功能：

```bash
jinux$ demo signal
```

**演示内容：**
- 信号的基本概念
- 设置信号处理器（SIG_DFL, SIG_IGN, 自定义）
- 发送和处理信号
- 不可捕获的信号（SIGKILL, SIGSTOP）

#### 2. `demo pipe` - 管道 IPC 演示

展示进程间通信：

```bash
jinux$ demo pipe
```

**演示内容：**
- 创建管道
- 写入和读取数据
- 管道容量测试
- EOF 检测
- 循环缓冲区机制

#### 3. `demo libc` - LibC 库演示

展示用户态库的使用：

```bash
jinux$ demo libc
```

**演示内容：**
- 进程管理函数（getpid, getppid）
- 文件 I/O 函数
- 内存管理函数（brk, malloc）
- 时间函数（time）
- 直接调用 vs LibC 封装对比

#### 4. `demo system` - 系统能力综合演示

全面展示系统各项功能：

```bash
jinux$ demo system
```

**演示内容：**

1. **系统信息展示**
   - 进程信息（PID, PPID）
   - 时间信息
   - 内存统计
   - 文件系统信息

2. **进程调度演示**
   - 当前进程状态
   - 时间片消耗模拟
   - 调度算法说明

3. **内存管理演示**
   - 地址空间布局
   - brk() 系统调用
   - malloc() 演示
   - 内存管理特性

4. **文件系统演示**
   - 文件系统架构
   - 支持的系统调用
   - 文件操作演示

5. **进程间通信演示**
   - 管道创建
   - 数据写入和读取
   - EOF 检测
   - IPC 特性说明

6. **信号处理演示**
   - 常见信号列表
   - 信号处理设置
   - 信号发送和处理
   - 信号特性说明

7. **系统调用性能演示**
   - 系统调用性能测试
   - 可用系统调用列表

#### 5. `demo interactive` - 交互式演示

模拟实际使用场景：

```bash
jinux$ demo interactive
```

**演示内容：**

1. **进程监控模拟**
   - 实时进程状态变化
   - 时间片消耗过程

2. **内存分配模式**
   - 典型的内存分配序列
   - 内存布局展示

3. **管道通信模式**
   - 生产者-消费者模式
   - 数据传输过程

4. **系统调用链**
   - 典型的系统调用序列
   - 状态保持和错误处理

#### 6. `demo all` - 运行所有演示

运行所有可用的演示：

```bash
jinux$ demo all
```

这会依次运行：
- signal 演示
- pipe 演示
- libc 演示
- system 演示
- interactive 演示

#### 7. `demo help` - 显示帮助

显示所有可用的演示类型：

```bash
jinux$ demo help
```

## 演示程序详解

### SystemCapabilitiesDemo

**位置：** `jinux.lib.SystemCapabilitiesDemo`

**功能：** 综合展示系统各项核心能力

**主要方法：**
- `runAllDemos()` - 运行所有演示
- `showSystemInfo()` - 显示系统信息
- `demonstrateScheduling()` - 进程调度演示
- `demonstrateMemoryOperations()` - 内存管理演示
- `demonstrateFileSystem()` - 文件系统演示
- `demonstrateIPC()` - IPC 演示
- `demonstrateSignalHandling()` - 信号处理演示
- `demonstrateSyscallPerformance()` - 系统调用性能演示

### InteractiveDemo

**位置：** `jinux.lib.InteractiveDemo`

**功能：** 交互式场景演示

**主要方法：**
- `runInteractiveDemo()` - 运行交互式演示
- `demonstrateProcessMonitoring()` - 进程监控演示
- `demonstrateMemoryAllocation()` - 内存分配演示
- `demonstratePipeCommunication()` - 管道通信演示
- `demonstrateSyscallChain()` - 系统调用链演示

### 其他演示程序

- **LibCExample** - LibC 使用示例
- **SignalExample** - 信号机制示例
- **PipeExample** - 管道机制示例
- **ProcessExample** - 进程创建示例

## 演示输出示例

### 系统能力综合演示输出

```
╔════════════════════════════════════════════════════════════╗
║                                                            ║
║         Jinux System Capabilities Demonstration            ║
║                                                            ║
╚════════════════════════════════════════════════════════════╝

═══════════════════════════════════════════════════════════
  [1/7] System Information
═══════════════════════════════════════════════════════════

  Process Information:
    PID (Process ID):     0
    PPID (Parent PID):    0

  Time Information:
    System Time:          1703260800 (Unix timestamp)
    Human Readable:       Fri Dec 22 20:00:00 CST 2023

  Memory Information:
    [内存统计信息...]

  File System Information:
    Root FS initialized:  Yes
    Inodes available:     1024
    Data blocks:          10240

...
```

### 交互式演示输出

```
╔════════════════════════════════════════════════════════════╗
║              Interactive Demonstration Menu               ║
╚════════════════════════════════════════════════════════════╝

[DEMO] Process Monitoring Simulation:
  Simulating process state changes over time...

  Tick 1:
    PID:     0
    State:   RUNNING
    Counter: 15 -> 13
  ...
```

## 使用建议

1. **首次使用**：运行 `demo all` 了解系统所有功能
2. **深入学习**：针对感兴趣的模块运行特定演示
3. **性能测试**：使用 `demo system` 查看系统调用性能
4. **实际场景**：使用 `demo interactive` 了解实际使用模式

## 扩展演示

你可以通过以下方式扩展演示：

1. **添加新的演示类**：在 `jinux.lib` 包中创建新的演示类
2. **注册到 Shell**：在 `SimpleShell.java` 中添加新的 demo 命令
3. **集成到启动流程**：在 `InitProcess.java` 中调用新演示

## 相关文档

- [Jinux 概述](JINUX_OVERVIEW.md)
- [Shell 使用指南](SHELL_GUIDE.md)
- [核心概念](CORE_CONCEPTS.md)
- [快速参考](QUICK_REFERENCE.md)

---

**注意：** 演示程序主要用于教学和学习目的，展示了 Jinux 的核心功能。在实际使用中，这些功能通过系统调用和 Shell 命令来访问。

