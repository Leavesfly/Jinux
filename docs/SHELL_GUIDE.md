# Jinux Shell 使用指南

## 简介

Jinux Shell 是一个简单的命令行解释器，提供交互式界面来操作 Jinux 操作系统。虽然功能有限（暂不支持外部程序执行），但提供了丰富的内置命令来管理和查询系统。

## 启动 Shell

运行 Jinux 后，系统会自动启动 Shell：

```bash
cd Jinux
mvn exec:java
```

系统完成初始化和演示后，会显示 Shell 提示符：

```
========================================
   Jinux Simple Shell v0.0.1-alpha
========================================
Type 'help' for available commands
Type 'exit' to quit

jinux$ _
```

## 内置命令详解

### 1. help - 显示帮助信息

显示所有可用命令的列表和简要说明。

**语法：**
```bash
jinux$ help
```

**输出示例：**
```
Available commands:
  help          - Show this help message
  ps            - List all processes
  mem           - Show memory statistics
  time          - Show current system time
  ...
```

---

### 2. ps - 显示进程列表

显示当前系统中运行的进程信息。

**语法：**
```bash
jinux$ ps
```

**输出示例：**
```
PID	STATE		PRIORITY	COUNTER
----------------------------------------------------
0	RUNNING		15		15
```

**字段说明：**
- `PID`: 进程 ID
- `STATE`: 进程状态（RUNNING, SLEEPING, STOPPED, ZOMBIE）
- `PRIORITY`: 进程优先级
- `COUNTER`: 剩余时间片

---

### 3. mem - 显示内存统计

显示物理内存使用情况。

**语法：**
```bash
jinux$ mem
```

**输出示例：**
```
========== Physical Memory Statistics ==========
Total memory: 16 MB (4096 pages)
Free pages: 3836
Used pages: 260
Memory usage: 6.35%
=================================================
```

---

### 4. time - 显示系统时间

显示当前系统时间（Unix 时间戳和可读日期）。

**语法：**
```bash
jinux$ time
```

**输出示例：**
```
System time: 1703260800 seconds since epoch
Date: Fri Dec 22 20:00:00 CST 2023
```

---

### 5. signal / kill - 发送信号

向指定进程发送信号。

**语法：**
```bash
jinux$ signal <pid> <signum>
jinux$ kill <pid> <signum>
```

**参数：**
- `<pid>`: 目标进程 ID
- `<signum>`: 信号编号

**常用信号：**
| 编号 | 信号名 | 说明 |
|------|--------|------|
| 1 | SIGHUP | 挂起 |
| 2 | SIGINT | 中断（Ctrl+C） |
| 9 | SIGKILL | 强制终止（不可捕获） |
| 15 | SIGTERM | 终止 |
| 17 | SIGCHLD | 子进程状态改变 |

**示例：**
```bash
# 发送 SIGINT 到进程 0
jinux$ signal 0 2
Signal 2 sent to process 0

# 发送 SIGTERM 到进程 1
jinux$ kill 1 15
Signal 15 sent to process 1
```

---

### 6. demo - 运行演示

运行系统功能演示程序。

**语法：**
```bash
jinux$ demo [type]
```

**参数：**
- `signal`: 信号机制演示
- `pipe`: 管道机制演示
- `libc`: LibC 库演示
- `all`: 运行所有演示（默认）

**示例：**
```bash
# 运行信号演示
jinux$ demo signal

# 运行所有演示
jinux$ demo
```

---

### 7. echo - 打印文本

打印文本到控制台。

**语法：**
```bash
jinux$ echo <text>
```

**示例：**
```bash
jinux$ echo Hello Jinux!
Hello Jinux!

jinux$ echo This is a test
This is a test
```

---

### 8. clear - 清屏

清除屏幕内容。

**语法：**
```bash
jinux$ clear
```

**说明：** 通过打印多个换行符实现简单清屏效果。

---

### 9. uptime - 显示系统运行时间

显示系统启动后的运行时间。

**语法：**
```bash
jinux$ uptime
```

**输出示例：**
```
System uptime: 100 seconds
  0 hours, 1 minutes, 40 seconds
```

---

### 10. version - 显示版本信息

显示 Jinux 和 Shell 的版本信息。

**语法：**
```bash
jinux$ version
```

**输出示例：**
```
Jinux Operating System
  Version: 0.01-alpha
  Shell Version: 0.0.1-alpha
  Java Implementation of Linux 0.01
```

---

### 11. exit / quit - 退出 Shell

退出 Shell 并关闭系统。

**语法：**
```bash
jinux$ exit
jinux$ quit
```

**输出：**
```
Exiting shell...
[INIT] Shell exited.
[INIT] Init process exiting...
```

---

## 使用技巧

### 1. 组合命令

可以依次输入多个命令：

```bash
jinux$ version
jinux$ mem
jinux$ ps
jinux$ exit
```

### 2. 查看进程并发送信号

```bash
# 查看当前进程
jinux$ ps
PID	STATE		PRIORITY	COUNTER
----------------------------------------------------
0	RUNNING		15		15

# 发送信号到该进程
jinux$ signal 0 2
Signal 2 sent to process 0
```

### 3. 快速演示系统功能

```bash
jinux$ demo all
```

这会运行所有演示程序，展示信号、管道、LibC 等功能。

---

## 限制和已知问题

### 当前限制

1. **不支持外部程序执行**
   - 缺少 `execve()` 系统调用
   - 只能运行内置命令

2. **不支持命令管道**
   - 无法使用 `cmd1 | cmd2` 语法
   - 需要完善的进程间通信机制

3. **不支持文件重定向**
   - 无法使用 `>` 和 `<` 重定向
   - 需要完整的文件系统实现

4. **不支持后台执行**
   - 无法使用 `&` 后台运行
   - 所有命令都是前台执行

5. **命令历史和编辑**
   - 不支持上下箭头查看历史
   - 不支持 Tab 补全

### 未来改进方向

- [ ] 实现 `execve()` 支持外部程序
- [ ] 添加命令管道 `|` 支持
- [ ] 添加文件重定向 `>` `<` 支持
- [ ] 添加后台执行 `&` 支持
- [ ] 命令历史和自动补全
- [ ] 环境变量支持
- [ ] 脚本执行支持

---

## 故障排除

### 问题：Shell 提示符不显示

**原因：** 系统还在运行初始化演示程序。

**解决：** 等待所有演示完成，Shell 会自动启动。

### 问题：命令执行后无响应

**原因：** 命令可能拼写错误或不支持。

**解决：** 输入 `help` 查看可用命令列表。

### 问题：如何退出卡死的系统

**解决：** 使用 `Ctrl+C` 强制退出程序。

---

## 示例会话

完整的使用示例：

```bash
$ mvn exec:java

... (系统初始化) ...

jinux$ help
Available commands:
  help          - Show this help message
  ...

jinux$ version
Jinux Operating System
  Version: 0.01-alpha
  ...

jinux$ mem

========== Physical Memory Statistics ==========
Total memory: 16 MB (4096 pages)
...

jinux$ ps
PID	STATE		PRIORITY	COUNTER
----------------------------------------------------
0	RUNNING		15		15

jinux$ echo Welcome to Jinux!
Welcome to Jinux!

jinux$ demo signal

[DEMO] Signal mechanism demonstration:
  Current PID: 0
  ...

jinux$ exit
Exiting shell...
```

---

## 总结

Jinux Shell 虽然功能简单，但提供了足够的命令来探索和学习操作系统的基本概念。通过这些内置命令，你可以：

- ✅ 查看系统状态（内存、进程、时间）
- ✅ 管理信号和进程
- ✅ 运行系统演示
- ✅ 与操作系统交互

这是一个很好的起点，未来会继续扩展更多功能！

**Happy exploring! 🚀**
