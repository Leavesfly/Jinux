# Jinux 实践练习

**版本**: 1.0  
**最后更新**: 2024-12-23

---

## 📚 目录

1. [基础练习](#基础练习)
2. [进阶练习](#进阶练习)
3. [高级项目](#高级项目)
4. [调试练习](#调试练习)
5. [性能优化](#性能优化)

---

## 基础练习

### 练习 1：修改启动横幅

**目标**：修改系统启动时显示的横幅

**步骤**：
1. 打开 `boot/Bootstrap.java`
2. 找到 `printBanner()` 方法
3. 修改横幅文本
4. 编译并运行，观察效果

**提示**：
```java
private static void printBanner() {
    System.out.println("\n" +
        "     _ _                  \n" +
        "    | (_)                 \n" +
        // 修改这里的文本
        ...
}
```

**扩展**：
- 添加版本号显示
- 添加启动时间显示

---

### 练习 2：添加 Shell 命令

**目标**：在 Shell 中添加一个新命令

**步骤**：
1. 打开 `shell/SimpleShell.java`
2. 找到 `executeCommand()` 方法
3. 添加新的命令处理逻辑
4. 在 `help` 命令中添加说明

**示例**：添加 `whoami` 命令

```java
case "whoami":
    Task current = kernel.getScheduler().getCurrentTask();
    console.println("Current user: root (PID: " + current.getPid() + ")");
    break;
```

**扩展**：
- 添加 `date` 命令显示日期
- 添加 `pwd` 命令显示当前目录

---

### 练习 3：理解进程创建

**目标**：跟踪 fork 系统调用的执行流程

**步骤**：
1. 在 `SystemCallDispatcher.sysFork()` 添加日志输出
2. 在 `AddressSpace.copy()` 添加日志输出
3. 运行系统，观察日志
4. 绘制调用关系图

**日志示例**：
```java
System.out.println("[FORK] Parent PID: " + currentTask.getPid());
System.out.println("[FORK] Allocating new PID: " + pid);
System.out.println("[FORK] Copying address space...");
System.out.println("[FORK] Child PID: " + pid);
```

**扩展**：
- 统计 fork 操作的执行时间
- 统计 fork 操作的内存使用

---

### 练习 4：观察进程调度

**目标**：理解进程调度算法

**步骤**：
1. 创建多个进程（修改 InitProcess）
2. 使用 `ps` 命令观察进程状态
3. 观察时间片的变化
4. 修改优先级，观察调度行为

**代码示例**：
```java
// 创建多个进程
for (int i = 0; i < 3; i++) {
    int pid = kernel.getScheduler().allocatePid();
    AddressSpace as = kernel.getMemoryManager().createAddressSpace();
    Task task = new Task(pid, 0, as);
    task.setPriority(10 + i * 5); // 不同优先级
    kernel.getScheduler().addTask(task);
}
```

**扩展**：
- 实现不同的调度算法（如 FCFS）
- 统计每个进程的运行时间

---

## 进阶练习

### 练习 5：实现 getuid 系统调用

**目标**：添加一个新的系统调用

**步骤**：
1. 在 `include/Syscalls.java` 添加系统调用号：
   ```java
   public static final int SYS_GETUID = 24;
   ```

2. 在 `SystemCallDispatcher.java` 注册处理器：
   ```java
   handlers.put(Syscalls.SYS_GETUID, this::sysGetuid);
   ```

3. 实现处理函数：
   ```java
   private long sysGetuid(Task currentTask, long arg1, long arg2, long arg3) {
       // 简化实现：返回固定值
       return 0; // root 用户
   }
   ```

4. 在 `LibC.java` 添加封装：
   ```java
   public int getuid() {
       return (int) syscallDispatcher.dispatch(
           Syscalls.SYS_GETUID, 0, 0, 0);
   }
   ```

5. 测试系统调用

**扩展**：
- 实现 `setuid()` 系统调用
- 在 Task 中添加 uid 字段

---

### 练习 6：实现简单的内存分配器

**目标**：实现一个简单的 malloc/free

**步骤**：
1. 在 `mm/AddressSpace.java` 添加堆管理
2. 实现 `malloc()` 和 `free()` 方法
3. 使用链表管理空闲块
4. 处理内存碎片

**代码框架**：
```java
class HeapBlock {
    long start;
    long size;
    boolean free;
    HeapBlock next;
}

public long malloc(int size) {
    // 1. 查找合适的空闲块
    // 2. 如果找到，分割块
    // 3. 如果没找到，扩展堆（brk）
    // 4. 返回分配的内存地址
}

public void free(long ptr) {
    // 1. 标记块为空闲
    // 2. 合并相邻的空闲块
}
```

**扩展**：
- 实现最佳适配算法
- 实现内存对齐
- 添加内存泄漏检测

---

### 练习 7：实现信号处理

**目标**：实现自定义信号处理器

**步骤**：
1. 创建一个测试进程
2. 设置 SIGINT 信号处理器
3. 发送信号到进程
4. 观察信号处理

**代码示例**：
```java
// 设置信号处理器
libc.signal(Signal.SIGINT, new Signal.SignalHandler() {
    @Override
    public void handle(int signum) {
        System.out.println("[SIGNAL] Received SIGINT!");
    }
});

// 发送信号
libc.kill(pid, Signal.SIGINT);
```

**扩展**：
- 实现信号屏蔽
- 实现信号队列
- 测试信号的安全性

---

### 练习 8：实现管道通信

**目标**：实现父子进程通过管道通信

**步骤**：
1. 创建管道
2. Fork 子进程
3. 父进程写入数据
4. 子进程读取数据

**代码示例**：
```java
// 创建管道
int[] fds = new int[2];
libc.pipe(fds);

// Fork
int pid = libc.fork();

if (pid == 0) {
    // 子进程：读取
    libc.close(fds[1]); // 关闭写端
    byte[] buf = new byte[100];
    int n = libc.read(fds[0], buf, buf.length);
    System.out.println("Child read: " + new String(buf, 0, n));
} else {
    // 父进程：写入
    libc.close(fds[0]); // 关闭读端
    String msg = "Hello from parent!";
    libc.write(fds[1], msg.getBytes(), msg.length());
    libc.close(fds[1]);
    libc.wait(null);
}
```

**扩展**：
- 实现双向通信（两个管道）
- 实现进程间文件传输
- 测试管道的阻塞机制

---

## 高级项目

### 项目 1：实现 Shell 命令管道

**目标**：实现 `cmd1 | cmd2` 语法

**要求**：
1. 解析命令管道语法
2. 创建管道
3. 第一个命令的输出重定向到管道
4. 第二个命令的输入从管道读取

**实现思路**：
```java
// 解析命令
String[] commands = input.split("\\|");

// 创建管道
int[] fds = new int[2];
libc.pipe(fds);

// 执行第一个命令（输出到管道）
executeCommand(commands[0], null, fds[1]);

// 执行第二个命令（从管道读取）
executeCommand(commands[1], fds[0], null);
```

**扩展**：
- 支持多个命令管道：`cmd1 | cmd2 | cmd3`
- 支持后台执行：`cmd1 | cmd2 &`

---

### 项目 2：实现文件重定向

**目标**：实现 `cmd > file` 和 `cmd < file` 语法

**要求**：
1. 解析重定向语法
2. 打开文件
3. 重定向标准输入/输出

**实现思路**：
```java
// 解析重定向
if (input.contains(">")) {
    String[] parts = input.split(">");
    String cmd = parts[0].trim();
    String file = parts[1].trim();
    
    // 打开文件
    int fd = libc.open(file, LibC.O_CREAT | LibC.O_WRONLY);
    
    // 重定向标准输出
    libc.dup2(fd, 1); // 1 是标准输出
    
    // 执行命令
    executeCommand(cmd);
}
```

**扩展**：
- 支持追加模式：`cmd >> file`
- 支持错误重定向：`cmd 2> file`
- 支持组合：`cmd < input > output`

---

### 项目 3：实现多级反馈队列调度

**目标**：实现 MLFQ 调度算法

**要求**：
1. 实现多个优先级队列
2. 高优先级队列时间片短
3. 低优先级队列时间片长
4. 进程可以升级/降级

**实现思路**：
```java
class MLFQScheduler {
    List<Queue<Task>> queues; // 多个优先级队列
    
    public void schedule() {
        // 从高优先级队列开始查找
        for (int i = 0; i < queues.size(); i++) {
            Queue<Task> queue = queues.get(i);
            if (!queue.isEmpty()) {
                Task next = queue.poll();
                // 根据队列级别设置时间片
                next.setCounter(getTimeSlice(i));
                switchTo(next);
                return;
            }
        }
    }
}
```

**扩展**：
- 实现进程优先级动态调整
- 实现完全公平调度（CFS）

---

### 项目 4：实现简单的文件系统操作

**目标**：实现文件创建、读写、删除

**要求**：
1. 实现 `touch` 命令创建文件
2. 实现 `cat` 命令读取文件
3. 实现 `echo > file` 写入文件
4. 实现 `rm` 命令删除文件

**实现思路**：
```java
// 创建文件
case "touch":
    String filename = args[1];
    int fd = libc.open(filename, LibC.O_CREAT | LibC.O_WRONLY);
    libc.close(fd);
    break;

// 读取文件
case "cat":
    String file = args[1];
    int fd = libc.open(file, LibC.O_RDONLY);
    byte[] buf = new byte[4096];
    int n;
    while ((n = libc.read(fd, buf, buf.length)) > 0) {
        console.write(buf, n);
    }
    libc.close(fd);
    break;
```

**扩展**：
- 实现 `ls` 命令列出目录
- 实现 `mkdir` 和 `rmdir` 命令
- 实现文件权限管理

---

## 调试练习

### 练习 9：调试内存泄漏

**目标**：找出内存泄漏的原因

**场景**：
- 系统运行一段时间后内存耗尽
- 进程退出后内存未释放

**步骤**：
1. 添加内存分配/释放日志
2. 统计每个进程的内存使用
3. 检查进程退出时的内存释放
4. 找出泄漏点

**调试代码**：
```java
// 在内存分配时记录
System.out.println("[MEM] Allocate: " + size + " bytes, total: " + totalAllocated);

// 在内存释放时记录
System.out.println("[MEM] Free: " + size + " bytes, total: " + totalAllocated);
```

---

### 练习 10：调试死锁

**目标**：找出进程死锁的原因

**场景**：
- 多个进程互相等待资源
- 系统无法继续执行

**步骤**：
1. 添加资源获取/释放日志
2. 检测循环等待
3. 实现死锁检测算法
4. 实现死锁恢复机制

**调试代码**：
```java
// 记录资源获取
System.out.println("[LOCK] Process " + pid + " acquiring resource " + resourceId);

// 记录资源释放
System.out.println("[LOCK] Process " + pid + " releasing resource " + resourceId);
```

---

## 性能优化

### 练习 11：优化 Fork 性能

**目标**：减少 fork 操作的时间

**当前问题**：
- fork 操作较慢
- 内存复制开销大

**优化方向**：
1. 优化 COW 实现
2. 延迟页面复制
3. 优化地址空间复制

**测量方法**：
```java
long start = System.currentTimeMillis();
int pid = libc.fork();
long end = System.currentTimeMillis();
System.out.println("Fork time: " + (end - start) + " ms");
```

---

### 练习 12：优化内存分配

**目标**：提高内存分配效率

**当前问题**：
- 内存分配较慢
- 内存碎片严重

**优化方向**：
1. 实现内存池
2. 优化分配算法
3. 实现内存压缩

**测量方法**：
```java
long start = System.currentTimeMillis();
long addr = malloc(size);
long end = System.currentTimeMillis();
System.out.println("Malloc time: " + (end - start) + " ms");
```

---

## 总结

通过这些练习，你将能够：

1. **理解系统实现细节**
2. **掌握代码修改技巧**
3. **具备功能扩展能力**
4. **掌握调试和优化方法**

**建议**：
- 从基础练习开始，逐步深入
- 每个练习都要理解原理
- 记录学习过程和问题
- 参考 Linux 0.01 源码

---

**文档版本**: 1.0  
**最后更新**: 2024-12-23  
**维护者**: Jinux Project Team

