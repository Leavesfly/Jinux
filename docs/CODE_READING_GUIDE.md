# Jinux 代码阅读指南

**版本**: 1.0  
**最后更新**: 2024-12-23

---

## 📚 目录

1. [代码阅读策略](#代码阅读策略)
2. [关键代码路径](#关键代码路径)
3. [调试技巧](#调试技巧)
4. [代码跟踪示例](#代码跟踪示例)
5. [常见代码模式](#常见代码模式)

---

## 代码阅读策略

### 1.1 自上而下 vs 自下而上

**自上而下**（推荐初学者）：
- 从高层接口开始（如系统调用）
- 逐步深入到底层实现
- 适合理解整体流程

**自下而上**（适合进阶）：
- 从底层数据结构开始
- 逐步理解上层抽象
- 适合深入理解实现细节

### 1.2 阅读顺序建议

**第一阶段：理解架构**
1. `boot/Bootstrap.java` - 系统入口
2. `kernel/Kernel.java` - 内核主类
3. `docs/JINUX_ARCHITECTURE.md` - 架构文档

**第二阶段：核心模块**
1. `kernel/Task.java` - 进程控制块
2. `kernel/Scheduler.java` - 进程调度
3. `mm/AddressSpace.java` - 地址空间
4. `kernel/SystemCallDispatcher.java` - 系统调用

**第三阶段：高级特性**
1. `kernel/Signal.java` - 信号机制
2. `ipc/Pipe.java` - 管道 IPC
3. `fs/VirtualFileSystem.java` - 文件系统

### 1.3 阅读技巧

**1. 先看注释和文档**
- 每个类都有 Javadoc 注释
- 理解类的职责和设计意图

**2. 理解数据结构**
- 先理解类的字段含义
- 再理解方法的作用

**3. 跟踪执行流程**
- 选择一个入口点（如系统调用）
- 逐步跟踪代码执行
- 绘制调用关系图

**4. 对比学习**
- 对比不同模块的实现
- 对比 Jinux 和 Linux 0.01

---

## 关键代码路径

### 2.1 系统启动路径

**代码路径**：

```
Bootstrap.main()
  ↓
Kernel()
  ├─ new MemoryManager()
  ├─ new Scheduler()
  ├─ new SystemCallDispatcher()
  ├─ new ConsoleDevice()
  ├─ new VirtualDiskDevice()
  └─ new ClockDevice()
  ↓
kernel.init()
  ├─ console.init()
  ├─ disk.init()
  ├─ clock.init()
  └─ vfs.init()
  ↓
kernel.createInitProcess()
  ├─ scheduler.allocatePid()
  ├─ memoryManager.createAddressSpace()
  ├─ new Task()
  └─ scheduler.addTask()
  ↓
kernel.start()
  ├─ clock.start()
  └─ scheduler.schedule()
```

**关键文件**：
- `boot/Bootstrap.java` - 入口点
- `kernel/Kernel.java` - 内核初始化
- `init/InitProcess.java` - init 进程

### 2.2 Fork 系统调用路径

**代码路径**：

```
用户程序调用 LibC.fork()
  ↓
SystemCallDispatcher.dispatch(SYS_FORK, ...)
  ↓
sysFork()
  ├─ scheduler.allocatePid()          // 分配新 PID
  ├─ memoryManager.createAddressSpace() // 创建地址空间
  ├─ addressSpace.copy()              // 复制地址空间（COW）
  ├─ task.copyFileDescriptorTable()   // 复制文件描述符表
  ├─ new Task(pid, ppid, addressSpace) // 创建子进程
  └─ scheduler.addTask(childTask)     // 加入调度队列
  ↓
返回子进程 PID（父进程）或 0（子进程）
```

**关键文件**：
- `kernel/SystemCallDispatcher.java` - sysFork() 方法
- `mm/AddressSpace.java` - copy() 方法（COW）
- `kernel/Task.java` - 进程创建

**跟踪要点**：
- 理解 PID 分配机制
- 理解地址空间复制（COW）
- 理解返回值设置

### 2.3 进程调度路径

**代码路径**：

```
ClockDevice 定时器触发（每 10ms）
  ↓
ClockDevice.timerInterrupt()
  ↓
Scheduler.timerInterrupt()
  ├─ currentTask.counter--           // 减少时间片
  ├─ 检查待处理信号
  └─ schedule()                       // 调度
      ├─ 查找 counter 最大的可运行进程
      ├─ 如果所有进程 counter=0，重新分配时间片
      └─ 切换到新进程
```

**关键文件**：
- `drivers/ClockDevice.java` - 时钟设备
- `kernel/Scheduler.java` - 调度器
- `kernel/Task.java` - 进程状态

**跟踪要点**：
- 理解时间片递减
- 理解调度算法
- 理解进程切换

### 2.4 内存分配路径

**代码路径**：

```
用户程序调用 brk(address)
  ↓
SystemCallDispatcher.dispatch(SYS_BRK, address, ...)
  ↓
sysBrk(address)
  ├─ Task currentTask = getCurrentTask()
  ├─ AddressSpace as = currentTask.getAddressSpace()
  └─ as.expandBrk(address)
      ├─ 计算需要分配的页面数
      ├─ 分配物理页面
      └─ 映射到虚拟地址空间
```

**关键文件**：
- `kernel/SystemCallDispatcher.java` - sysBrk() 方法
- `mm/AddressSpace.java` - expandBrk() 方法
- `mm/PhysicalMemory.java` - allocPage() 方法

**跟踪要点**：
- 理解堆扩展机制
- 理解页面分配
- 理解地址映射

### 2.5 文件打开路径

**代码路径**：

```
用户程序调用 open(path, flags)
  ↓
SystemCallDispatcher.dispatch(SYS_OPEN, pathPtr, flags, ...)
  ↓
sysOpen(pathPtr, flags, mode)
  ├─ copyStringFromUser(pathPtr)     // 从用户空间读取路径
  ├─ vfs.namei(path)                 // 路径解析
  │   ├─ 解析路径组件
  │   ├─ 查找目录项
  │   └─ 返回 inode
  ├─ 如果文件不存在且 O_CREAT，创建文件
  ├─ new File(inode, flags)          // 创建文件对象
  └─ currentTask.fdTable.allocFd(file) // 分配文件描述符
  ↓
返回文件描述符
```

**关键文件**：
- `kernel/SystemCallDispatcher.java` - sysOpen() 方法
- `fs/VirtualFileSystem.java` - namei() 方法
- `fs/File.java` - 文件对象
- `fs/FileDescriptorTable.java` - 文件描述符表

**跟踪要点**：
- 理解路径解析
- 理解文件创建
- 理解文件描述符分配

---

## 调试技巧

### 3.1 使用 IDE 调试器

**IntelliJ IDEA / Eclipse**：

1. **设置断点**
   - 在关键方法入口设置断点
   - 在关键数据结构访问处设置断点

2. **单步执行**
   - F8：单步跳过
   - F7：单步进入
   - F9：继续执行

3. **查看变量**
   - 查看局部变量
   - 查看对象字段
   - 查看调用栈

**示例**：调试 fork 系统调用

```java
// 在 SystemCallDispatcher.sysFork() 设置断点
public long sysFork(Task currentTask, ...) {
    // 断点 1：查看当前进程信息
    int pid = scheduler.allocatePid();
    // 断点 2：查看新分配的 PID
    AddressSpace childSpace = addressSpace.copy();
    // 断点 3：查看复制的地址空间
    // ...
}
```

### 3.2 添加日志输出

**添加调试日志**：

```java
System.out.println("[DEBUG] Fork: pid=" + pid + ", ppid=" + ppid);
System.out.println("[DEBUG] Address space copied: " + childSpace);
System.out.println("[DEBUG] Task created: " + task);
```

**使用日志级别**：

```java
private static final boolean DEBUG = true;

if (DEBUG) {
    System.out.println("[DEBUG] ...");
}
```

### 3.3 使用 Shell 命令观察

**查看进程状态**：

```bash
jinux$ ps
PID	STATE		PRIORITY	COUNTER
----------------------------------------------------
0	RUNNING		15		15
```

**查看内存使用**：

```bash
jinux$ mem
========== Physical Memory Statistics ==========
Total memory: 16 MB (4096 pages)
Free pages: 3836
Used pages: 260
```

### 3.4 绘制调用关系图

**示例：fork 调用关系图**

```
LibC.fork()
  └─ SystemCallDispatcher.dispatch(SYS_FORK)
      └─ sysFork()
          ├─ Scheduler.allocatePid()
          ├─ MemoryManager.createAddressSpace()
          ├─ AddressSpace.copy()
          │   ├─ PhysicalMemory.allocPage()
          │   └─ PageTable.map()
          ├─ Task.copyFileDescriptorTable()
          └─ Scheduler.addTask()
```

---

## 代码跟踪示例

### 4.1 跟踪 fork 系统调用

**步骤 1：找到入口点**

```java
// lib/LibC.java
public int fork() {
    return (int) syscallDispatcher.dispatch(
        Syscalls.SYS_FORK, 0, 0, 0);
}
```

**步骤 2：跟踪分发器**

```java
// kernel/SystemCallDispatcher.java
public long dispatch(int syscallNr, long arg1, long arg2, long arg3) {
    Task currentTask = scheduler.getCurrentTask();
    SystemCallHandler handler = handlers.get(syscallNr);
    return handler.handle(currentTask, arg1, arg2, arg3);
}
```

**步骤 3：跟踪处理函数**

```java
// kernel/SystemCallDispatcher.java
private long sysFork(Task currentTask, ...) {
    // 1. 分配 PID
    int pid = scheduler.allocatePid();
    
    // 2. 复制地址空间
    AddressSpace childSpace = currentTask.getAddressSpace().copy();
    
    // 3. 复制文件描述符表
    FileDescriptorTable childFdTable = currentTask.getFdTable().copy();
    
    // 4. 创建子进程
    Task childTask = new Task(pid, currentTask.getPid(), childSpace);
    childTask.setFdTable(childFdTable);
    
    // 5. 添加到调度器
    scheduler.addTask(childTask);
    
    // 6. 返回子进程 PID（父进程）或 0（子进程）
    return pid; // 父进程返回子进程 PID
    // 子进程返回 0（在 Task 构造函数中设置）
}
```

**步骤 4：深入理解关键操作**

```java
// mm/AddressSpace.java
public AddressSpace copy() {
    AddressSpace newSpace = new AddressSpace();
    
    // 遍历所有页面映射
    for (Map.Entry<Long, Long> entry : pageTable.mappings.entrySet()) {
        long vaddr = entry.getKey();
        long paddr = entry.getValue();
        
        // COW：共享物理页面
        newSpace.pageTable.map(vaddr, paddr);
        newSpace.pageTable.setFlags(vaddr, 
            PAGE_PRESENT | PAGE_COW | PAGE_USER);
        
        // 增加引用计数
        physicalMemory.incrementPageRef(paddr);
    }
    
    return newSpace;
}
```

### 4.2 跟踪进程调度

**步骤 1：时钟中断触发**

```java
// drivers/ClockDevice.java
public void start() {
    timer = new Timer();
    timer.scheduleAtFixedRate(new TimerTask() {
        @Override
        public void run() {
            scheduler.timerInterrupt();
        }
    }, 0, 10); // 每 10ms 触发一次
}
```

**步骤 2：调度器处理中断**

```java
// kernel/Scheduler.java
public void timerInterrupt() {
    Task current = getCurrentTask();
    if (current != null) {
        // 减少时间片
        current.decrementCounter();
        
        // 检查待处理信号
        if (current.hasPendingSignals()) {
            signal.handlePendingSignals(current);
        }
        
        // 如果时间片用完，调度
        if (current.getCounter() <= 0) {
            schedule();
        }
    }
}
```

**步骤 3：选择下一个进程**

```java
// kernel/Scheduler.java
public void schedule() {
    Task next = null;
    int maxCounter = -1;
    
    // 查找 counter 最大的可运行进程
    for (Task task : tasks) {
        if (task.getState() == Task.TASK_RUNNING) {
            if (task.getCounter() > maxCounter) {
                maxCounter = task.getCounter();
                next = task;
            }
        }
    }
    
    // 如果所有进程 counter=0，重新分配时间片
    if (maxCounter <= 0) {
        for (Task task : tasks) {
            if (task.getState() == Task.TASK_RUNNING) {
                task.setCounter(task.getCounter() / 2 + task.getPriority());
            }
        }
        // 重新选择
        schedule();
        return;
    }
    
    // 切换到新进程
    if (next != null && next != currentTask) {
        switchTo(next);
    }
}
```

---

## 常见代码模式

### 5.1 用户空间数据拷贝

**模式**：系统调用需要从用户空间读取数据

```java
// 读取字符串
String path = copyStringFromUser(pathPtr);

// 读取字节数组
byte[] buf = new byte[length];
copyFromUser(bufPtr, buf, length);

// 写入数据到用户空间
copyToUser(resultPtr, result, resultLength);
```

**关键方法**：
- `copyStringFromUser()` - 读取字符串
- `copyFromUser()` - 读取字节数组
- `copyToUser()` - 写入数据

### 5.2 错误处理模式

**模式**：系统调用返回错误码

```java
// 成功返回非负值，失败返回负错误码
if (result < 0) {
    return -Const.ENOENT; // 返回负错误码
}
return result; // 返回成功值
```

**错误码定义**：
- `E_OK = 0` - 成功
- `ENOENT = 2` - 文件不存在
- `ENOMEM = 12` - 内存不足
- `EINVAL = 22` - 参数无效

### 5.3 资源管理模式

**模式**：分配资源后需要释放

```java
// 分配资源
int pid = scheduler.allocatePid();
AddressSpace as = memoryManager.createAddressSpace();

try {
    // 使用资源
    Task task = new Task(pid, ppid, as);
    scheduler.addTask(task);
} catch (Exception e) {
    // 错误处理：释放资源
    scheduler.freePid(pid);
    memoryManager.destroyAddressSpace(as);
    throw e;
}
```

### 5.4 状态检查模式

**模式**：操作前检查状态

```java
// 检查进程状态
if (task.getState() != Task.TASK_RUNNING) {
    return -Const.EINVAL;
}

// 检查权限
if (!hasPermission(task, operation)) {
    return -Const.EPERM;
}
```

---

## 总结

通过本指南，你应该能够：

1. **理解代码阅读策略**
2. **跟踪关键代码路径**
3. **使用调试技巧**
4. **识别常见代码模式**

**下一步**：
- 选择一个系统调用，完整跟踪其执行流程
- 使用调试器单步执行代码
- 绘制调用关系图
- 阅读 `docs/PRACTICE_EXERCISES.md` 进行实践

---

**文档版本**: 1.0  
**最后更新**: 2024-12-23  
**维护者**: Jinux Project Team

