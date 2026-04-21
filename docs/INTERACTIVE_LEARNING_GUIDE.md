# 🎓 Jinux 交互式学习指南

> 通过互动问答、可视化工具、模拟实验和概念解释，轻松掌握操作系统核心知识

---

## 📖 概述

Jinux 项目新增了一套完整的**交互式教学系统**，包含 5 大教学工具，旨在降低操作系统学习门槛，让学习过程更加有趣和高效。

### 新增的 Shell 命令

| 命令 | 功能 | 适合场景 |
|------|------|---------|
| `quiz` | 交互式知识问答 (125题) | 检验和巩固知识 |
| `lab` | 动手模拟实验 (5个实验) | 深入理解原理 |
| `explain [topic]` | 概念解释器 (12个主题) | 初次学习新概念 |
| `visualize` | ASCII 可视化工具 (8种视图) | 直观理解系统结构 |
| `progress` | 学习进度追踪 | 了解学习情况 |

---

## 🚀 快速开始

### 启动 Jinux

```bash
mvn clean compile
mvn exec:java
```

进入 Shell 后，输入以下命令开始学习：

```bash
# 查看所有可用命令
jinux$ help

# 从概念解释开始
jinux$ explain process

# 尝试可视化工具
jinux$ visualize

# 做几道题检验理解
jinux$ quiz

# 动手做实验
jinux$ lab

# 查看学习进度
jinux$ progress
```

---

## 📚 推荐学习路径

### 🌱 路径一：零基础入门（建议 1-2 周）

适合没有操作系统基础的同学。

**第 1 天：认识进程**
```bash
jinux$ explain process        # 了解"进程"的概念（厨师做菜的类比）
jinux$ explain scheduling     # 了解"调度"的概念（旋转木马的类比）
jinux$ visualize              # 选择 1 查看进程状态图
```

**第 2 天：认识内存**
```bash
jinux$ explain memory         # 了解内存管理（公寓楼的类比）
jinux$ explain virtual-memory # 了解虚拟内存（楼层平面图的类比）
jinux$ visualize              # 选择 2 查看内存布局
```

**第 3 天：认识系统调用和文件系统**
```bash
jinux$ explain syscall        # 了解系统调用（银行柜台的类比）
jinux$ explain filesystem     # 了解文件系统（图书馆的类比）
jinux$ visualize              # 选择 4 查看文件系统树
```

**第 4 天：认识信号和管道**
```bash
jinux$ explain signal         # 了解信号（手机通知的类比）
jinux$ explain pipe           # 了解管道（水管的类比）
jinux$ visualize              # 选择 5 查看管道数据流
```

**第 5 天：做题检验**
```bash
jinux$ quiz                   # 选择 2（按难度），选 BEGINNER
jinux$ progress               # 查看学习进度
```

### 📖 路径二：有基础进阶（建议 1 周）

适合学过操作系统课程，想通过代码加深理解的同学。

**Day 1-2：可视化理解系统架构**
```bash
jinux$ visualize              # 逐个查看所有 8 种可视化
jinux$ explain fork           # 深入理解 fork 和 COW
jinux$ explain context-switch # 理解上下文切换
```

**Day 3-4：动手实验**
```bash
jinux$ lab                    # 完成 5 个实验
                              # 重点：进程调度实验和 Fork 过程实验
```

**Day 5：综合测试**
```bash
jinux$ quiz                   # 选择 4（综合测试），挑战所有题目
jinux$ progress               # 查看成就解锁情况
```

### 🏆 路径三：深度学习（建议 2-3 周）

适合想深入理解 Linux 0.01 内核实现的同学。

1. 完成所有 12 个概念解释 → 解锁 📖 知识渊博 成就
2. 完成所有 8 种可视化 → 解锁 🎨 可视化达人 成就
3. 完成所有 5 个实验 → 解锁 🔬 科学家 成就
4. 所有模块正确率 > 80% → 解锁 🏆 操作系统大师 成就

---

## 🎮 功能详解

### 1. 知识问答系统 (`quiz`)

**125 道精心设计的题目**，覆盖 5 大模块 × 5 个难度级别：

| 模块 | 知识点示例 |
|------|-----------|
| **进程管理** | PCB结构、进程状态、fork返回值、调度算法、COW |
| **内存管理** | 页面大小、位图管理、虚拟地址空间、页表、brk |
| **文件系统** | VFS、Inode、SuperBlock、MINIX参数、缓冲区缓存 |
| **信号机制** | 信号类型、处理器设置、信号位图、不可捕获信号 |
| **IPC通信** | 管道概念、缓冲区大小、阻塞行为、SIGPIPE |

**难度级别**：
- 🌱 **BEGINNER** - 基本概念和术语
- 📗 **BASIC** - 基础原理和简单应用
- 📘 **INTERMEDIATE** - 实现细节和算法
- 📙 **ADVANCED** - 高级特性和优化
- 📕 **EXPERT** - 深层原理和边界情况

**答题模式**：
1. 按模块答题 - 专注某个知识领域
2. 按难度答题 - 逐步提升难度
3. 随机挑战 - 随机抽 10 题
4. 综合测试 - 全部题目

### 2. 模拟实验 (`lab`)

**5 个动手实验**，让你亲自操作理解原理：

| 实验 | 内容 | 学到什么 |
|------|------|---------|
| **进程调度** | 手动模拟调度算法 | counter选择、时间片重算 |
| **内存分配** | 操作物理内存位图 | 分配/释放、碎片率 |
| **管道通信** | 操作真实Pipe对象 | 读写、缓冲区、EOF |
| **信号处理** | 操作信号位图 | 发送/屏蔽/处理信号 |
| **Fork过程** | 逐步模拟fork | PCB复制、COW、返回值 |

### 3. 概念解释器 (`explain`)

**12 个核心概念**，每个都包含：
- 🎯 **生活类比** - 用日常例子帮助理解
- 📖 **技术定义** - 准确的学术描述
- 💻 **代码实现** - 在 Jinux 中如何实现
- 📂 **代码指引** - 应该阅读哪些源文件

```bash
# 查看所有主题
jinux$ explain

# 查看特定主题
jinux$ explain process
jinux$ explain scheduling
jinux$ explain memory
jinux$ explain virtual-memory
jinux$ explain syscall
jinux$ explain signal
jinux$ explain pipe
jinux$ explain filesystem
jinux$ explain fork
jinux$ explain inode
jinux$ explain page-table
jinux$ explain context-switch
```

### 4. 可视化工具 (`visualize`)

**8 种 ASCII 可视化视图**：

| 视图 | 展示内容 |
|------|---------|
| 进程状态图 | 状态转换关系 + 当前进程表 |
| 内存布局 | 物理内存进度条 + 虚拟地址空间 |
| 调度时间线 | 多进程调度的时间片分配 |
| 文件系统树 | 目录结构的树形展示 |
| 管道数据流 | 写进程→缓冲区→读进程 |
| 系统调用流程 | 用户态→内核态的完整路径 |
| 信号处理流程 | 信号发送→位图→处理器 |
| 系统仪表盘 | 综合状态一览 |

### 5. 学习进度 (`progress`)

**追踪你的学习旅程**：
- 📊 **进度总览** - 各模块掌握度进度条
- 📋 **详细报告** - 最强/最弱模块分析
- 💡 **个性化建议** - 根据数据推荐下一步
- 🏆 **成就系统** - 10 个成就等你解锁

**成就列表**：
| 成就 | 条件 |
|------|------|
| 🌱 初学者 | 完成第一道题 |
| 📚 好学生 | 答对 10 道题 |
| 🎯 神枪手 | 连续答对 5 道题 |
| 💯 满分王 | 某模块正确率 100% |
| 🧪 实验家 | 完成第一个实验 |
| 🔬 科学家 | 完成所有 5 个实验 |
| 📖 知识渊博 | 查看所有 12 个概念 |
| 🎨 可视化达人 | 使用所有 8 个可视化 |
| 🌟 全能选手 | 所有模块正确率 > 60% |
| 🏆 操作系统大师 | 所有模块 > 80% + 全部实验 |

---

## 🏗️ 技术架构

### 新增文件

```
src/main/java/jinux/demo/
├── QuizSystem.java        # 知识问答系统 (125题)
├── Visualizer.java        # ASCII 可视化工具 (8种视图)
├── LabSimulator.java      # 模拟实验系统 (5个实验)
├── ConceptExplainer.java  # 概念解释器 (12个主题)
└── LearningTracker.java   # 学习进度追踪器 (单例)
```

### 修改文件

```
src/main/java/jinux/shell/SimpleShell.java  # 新增 5 个命令
```

### 设计原则

1. **零外部依赖** - 所有功能仅使用 Java 标准库
2. **非侵入式** - 新增文件不修改任何核心内核代码
3. **即插即用** - 通过 Shell 命令直接访问所有功能
4. **渐进难度** - 从类比理解到动手实验，层层深入

---

## ❓ 常见问题

**Q: 学习进度会保存吗？**
A: 当前版本的学习进度保存在内存中，重启后会重置。这是因为 Jinux 是一个模拟操作系统，每次启动都是全新的会话。

**Q: 题目答错了怎么办？**
A: 每道题答完后都会立即显示正确答案和详细解释，帮助你理解。建议先用 `explain` 命令学习概念，再用 `quiz` 检验。

**Q: 实验会影响系统状态吗？**
A: 大部分实验使用模拟数据，不会影响 Jinux 内核的实际状态。管道实验会创建真实的 Pipe 对象，但不会影响其他进程。

**Q: 如何扩展新的题目或实验？**
A: 在 `QuizSystem.java` 的 static 初始化块中添加新的 Question 对象即可添加题目。在 `LabSimulator.java` 中添加新的实验方法并注册到菜单中。

---

<div align="center">

**🎯 开始你的操作系统学习之旅吧！**

```bash
jinux$ explain process
```

</div>
