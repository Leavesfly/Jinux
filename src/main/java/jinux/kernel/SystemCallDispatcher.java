package jinux.kernel;

import jinux.include.Const;
import jinux.include.Syscalls;
import jinux.include.Types;
import jinux.mm.MemoryManager;
import jinux.fs.VirtualFileSystem;
import jinux.fs.File;
import jinux.fs.Inode;
import jinux.ipc.Pipe;
import jinux.ipc.PipeFile;
import jinux.exec.ProgramLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统调用表和分发器
 * 对应 Linux 0.01 中的 kernel/system_call.s 和各系统调用实现
 * 
 * @author Jinux Project
 */
public class SystemCallDispatcher {
    
    /** 调度器 */
    private final Scheduler scheduler;
    
    /** 内存管理器 */
    private final MemoryManager memoryManager;
    
    /** 文件系统 */
    private VirtualFileSystem vfs;
    
    /** 系统调用处理器映射 */
    private final Map<Integer, SystemCallHandler> handlers;
    
    /**
     * 系统调用处理器接口
     */
    @FunctionalInterface
    public interface SystemCallHandler {
        long handle(Task task, long arg1, long arg2, long arg3);
    }
    
    /**
     * 构造系统调用分发器
     */
    public SystemCallDispatcher(Scheduler scheduler, MemoryManager memoryManager) {
        this.scheduler = scheduler;
        this.memoryManager = memoryManager;
        this.handlers = new HashMap<>();
        
        registerSystemCalls();
    }
    
    /**
     * 设置文件系统
     */
    public void setVfs(VirtualFileSystem vfs) {
        this.vfs = vfs;
    }
    
    /**
     * 注册所有系统调用
     */
    private void registerSystemCalls() {
        // 进程管理
        handlers.put(Syscalls.SYS_FORK, this::sysFork);
        handlers.put(Syscalls.SYS_EXIT, this::sysExit);
        handlers.put(Syscalls.SYS_WAIT, this::sysWait);
        handlers.put(Syscalls.SYS_EXECVE, this::sysExecve);
        handlers.put(Syscalls.SYS_GETPID, this::sysGetpid);
        handlers.put(Syscalls.SYS_GETPPID, this::sysGetppid);
        handlers.put(Syscalls.SYS_PAUSE, this::sysPause);
        
        // 信号管理
        handlers.put(Syscalls.SYS_SIGNAL, this::sysSignal);
        handlers.put(Syscalls.SYS_KILL, this::sysKill);
        
        // 进程间通信
        handlers.put(Syscalls.SYS_PIPE, this::sysPipe);
        
        // 文件操作
        handlers.put(Syscalls.SYS_READ, this::sysRead);
        handlers.put(Syscalls.SYS_WRITE, this::sysWrite);
        handlers.put(Syscalls.SYS_OPEN, this::sysOpen);
        handlers.put(Syscalls.SYS_CLOSE, this::sysClose);
        handlers.put(Syscalls.SYS_LSEEK, this::sysLseek);
        handlers.put(Syscalls.SYS_CREAT, this::sysCreat);
        handlers.put(Syscalls.SYS_UNLINK, this::sysUnlink);
        handlers.put(Syscalls.SYS_STAT, this::sysStat);
        handlers.put(Syscalls.SYS_FSTAT, this::sysFstat);
        
        // 目录操作
        handlers.put(Syscalls.SYS_CHDIR, this::sysChdir);
        handlers.put(Syscalls.SYS_MKDIR, this::sysMkdir);
        handlers.put(Syscalls.SYS_RMDIR, this::sysRmdir);
        
        // 内存管理
        handlers.put(Syscalls.SYS_BRK, this::sysBrk);
        
        // 时间
        handlers.put(Syscalls.SYS_TIME, this::sysTime);
        handlers.put(Syscalls.SYS_TIMES, this::sysTimes);
        
        // 文件系统
        handlers.put(Syscalls.SYS_SYNC, this::sysSync);
        
        // 文件描述符操作
        handlers.put(Syscalls.SYS_DUP, this::sysDup);
        handlers.put(Syscalls.SYS_DUP2, this::sysDup2);
    }
    
    // ==================== 用户空间数据拷贝辅助函数 ====================
    
    /**
     * 从用户空间读取字符串
     * 
     * @param task 当前进程
     * @param userPtr 用户空间指针
     * @param maxLen 最大长度
     * @return 读取的字符串，失败返回 null
     */
    private String copyStringFromUser(Task task, long userPtr, int maxLen) {
        if (userPtr == 0 || maxLen <= 0) {
            return null;
        }
        
        try {
            byte[] buf = new byte[maxLen];
            task.getAddressSpace().readBytes(userPtr, buf, 0, maxLen);
            
            // 查找字符串结束符
            int len = 0;
            while (len < maxLen && buf[len] != 0) {
                len++;
            }
            
            return new String(buf, 0, len, "UTF-8");
        } catch (Exception e) {
            System.err.println("[SYSCALL] Failed to copy string from user space: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 从用户空间读取数据到内核缓冲区
     * 
     * @param task 当前进程
     * @param userPtr 用户空间指针
     * @param buf 内核缓冲区
     * @param offset 缓冲区偏移
     * @param len 读取长度
     * @return 实际读取的字节数，失败返回 -1
     */
    private int copyFromUser(Task task, long userPtr, byte[] buf, int offset, int len) {
        if (userPtr == 0 || buf == null || len <= 0) {
            return -1;
        }
        
        try {
            task.getAddressSpace().readBytes(userPtr, buf, offset, len);
            return len;
        } catch (Exception e) {
            System.err.println("[SYSCALL] Failed to copy from user space: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * 将数据从内核缓冲区写入用户空间
     * 
     * @param task 当前进程
     * @param userPtr 用户空间指针
     * @param buf 内核缓冲区
     * @param offset 缓冲区偏移
     * @param len 写入长度
     * @return 实际写入的字节数，失败返回 -1
     */
    private int copyToUser(Task task, long userPtr, byte[] buf, int offset, int len) {
        if (userPtr == 0 || buf == null || len <= 0) {
            return -1;
        }
        
        try {
            task.getAddressSpace().writeBytes(userPtr, buf, offset, len);
            return len;
        } catch (Exception e) {
            System.err.println("[SYSCALL] Failed to copy to user space: " + e.getMessage());
            return -1;
        }
    }
    
    /**
     * 分发系统调用
     * 
     * @param nr 系统调用号
     * @param arg1 参数1
     * @param arg2 参数2
     * @param arg3 参数3
     * @return 返回值
     */
    public long dispatch(int nr, long arg1, long arg2, long arg3) {
        Task currentTask = scheduler.getCurrentTask();
        
        if (currentTask == null) {
            System.err.println("[SYSCALL] ERROR: No current task!");
            return -Const.ESRCH;
        }
        
        SystemCallHandler handler = handlers.get(nr);
        if (handler == null) {
            System.err.println("[SYSCALL] ERROR: Unknown syscall: " + nr + " (" + 
                Syscalls.getSyscallName(nr) + ")");
            return -Const.EINVAL;
        }
        
        // 记录进入内核态
        long startTime = System.nanoTime();
        
        try {
            long result = handler.handle(currentTask, arg1, arg2, arg3);
            
            // 统计内核态时间
            long endTime = System.nanoTime();
            currentTask.addStime((endTime - startTime) / 1000000); // 转换为毫秒
            
            return result;
            
        } catch (Exception e) {
            System.err.println("[SYSCALL] Exception in syscall " + Syscalls.getSyscallName(nr) + 
                ": " + e.getMessage());
            e.printStackTrace();
            return -Const.EFAULT;
        }
    }
    
    // ==================== 进程管理系统调用实现 ====================
    
    /**
     * sys_fork - 创建子进程
     */
    private long sysFork(Task parent, long arg1, long arg2, long arg3) {
        System.out.println("[SYSCALL] fork() called by pid=" + parent.getPid());
        
        // 分配新 PID
        int childPid = scheduler.allocatePid();
        
        // 复制地址空间
        var childAddrSpace = parent.getAddressSpace().copy();
        
        // 创建子进程
        Task child = new Task(childPid, parent.getPid(), childAddrSpace);
        child.setPriority(parent.getPriority());
        child.setCounter(parent.getCounter());
        
        // 复制文件描述符表
        child.setFdTable(parent.getFdTable().copy());
        child.setCurrentWorkingDir(parent.getCurrentWorkingDir());
        
        // 添加到调度器
        if (!scheduler.addTask(child)) {
            // 失败，释放资源
            childAddrSpace.free();
            return -Const.ENOMEM;
        }
        
        System.out.println("[SYSCALL] fork() created child pid=" + childPid);
        
        // 父进程返回子进程 PID，子进程返回 0（这里简化处理）
        return childPid;
    }
    
    /**
     * sys_exit - 退出进程
     */
    private long sysExit(Task task, long exitCode, long arg2, long arg3) {
        System.out.println("[SYSCALL] exit(" + exitCode + ") called by pid=" + task.getPid());
        
        task.exit((int) exitCode);
        
        // 唤醒父进程（如果在等待）
        Task parent = scheduler.findTask(task.getPpid());
        if (parent != null && parent.getWaitingForPid() == task.getPid()) {
            parent.wakeUp();
        }
        
        // 重新调度
        scheduler.schedule();
        
        return 0;
    }
    
    /**
     * sys_wait - 等待子进程
     */
    private long sysWait(Task task, long statusPtr, long arg2, long arg3) {
        System.out.println("[SYSCALL] wait() called by pid=" + task.getPid());
        
        // 查找僵尸子进程
        for (Task t : scheduler.getTaskTable()) {
            if (t != null && t.getPpid() == task.getPid() && t.getState() == Const.TASK_ZOMBIE) {
                int childPid = t.getPid();
                int exitCode = t.getExitCode();
                
                // 回收子进程
                scheduler.removeTask(childPid);
                
                System.out.println("[SYSCALL] wait() collected zombie child pid=" + childPid);
                return childPid;
            }
        }
        
        // 没有僵尸子进程，睡眠等待
        task.sleep(true);
        scheduler.schedule();
        
        return -Const.EINTR; // 简化：返回中断
    }
    
    /**
     * sys_getpid - 获取进程 ID
     */
    private long sysGetpid(Task task, long arg1, long arg2, long arg3) {
        return task.getPid();
    }
    
    /**
     * sys_getppid - 获取父进程 ID
     */
    private long sysGetppid(Task task, long arg1, long arg2, long arg3) {
        return task.getPpid();
    }
    
    /**
     * sys_execve - 加载并执行新程序
     * 
     * @param task 当前进程
     * @param filename 程序路径指针（用户空间）
     * @param argv 参数数组指针（用户空间）
     * @param envp 环境变量数组指针（用户空间）
     * @return 0 成功，负数表示错误
     */
    private long sysExecve(Task task, long filename, long argv, long envp) {
        // 从用户空间读取程序路径
        String programPath = copyStringFromUser(task, filename, 256);
        if (programPath == null || programPath.isEmpty()) {
            System.err.println("[SYSCALL] execve: invalid filename pointer");
            return -Const.EFAULT;
        }
        
        System.out.println("[SYSCALL] execve(\"" + programPath + "\") called by pid=" + task.getPid());
        
        // 从用户空间读取参数数组
        String[] args = readStringArrayFromUser(task, argv);
        
        // 从用户空间读取环境变量数组
        String[] env = readStringArrayFromUser(task, envp);
        
        // 尝试从文件系统加载程序
        if (vfs != null) {
            // 获取当前工作目录
            Inode currentDir = null;
            int cwdIno = task.getCurrentWorkingDir();
            if (cwdIno > 0) {
                currentDir = vfs.getInode(Const.ROOT_DEV, cwdIno);
            }
            if (currentDir == null) {
                currentDir = vfs.getRootInode();
            }
            
            // 解析程序路径
            Inode programInode = vfs.namei(programPath, currentDir);
            if (programInode != null && programInode.isRegularFile()) {
                // 找到文件，尝试作为可执行程序加载
                // 简化：检查文件扩展名或使用预注册的程序名
                String programName = extractProgramName(programPath);
                int result = ProgramLoader.loadProgram(task, programName, args, env);
                if (result >= 0) {
                    vfs.putInode(programInode);
                    startExecutedProgram(task);
                    return 0; // execve 成功不返回
                }
                vfs.putInode(programInode);
            }
        }
        
        // 回退到预注册的程序
        String programName = extractProgramName(programPath);
        int result = ProgramLoader.loadProgram(task, programName, args, env);
        
        if (result < 0) {
            System.err.println("[SYSCALL] execve() failed: program not found");
            return -Const.ENOENT;
        }
        
        // execve 成功后不返回（被新程序替换）
        startExecutedProgram(task);
        
        // execve 成功不返回（实际应该永不返回）
        return 0;
    }
    
    /**
     * 从程序路径中提取程序名
     */
    private String extractProgramName(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        
        // 去掉路径，只保留文件名
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        
        return path;
    }
    
    /**
     * 从用户空间读取字符串数组
     * 
     * @param task 当前进程
     * @param arrayPtr 数组指针（用户空间）
     * @return 字符串数组，失败返回 null
     */
    private String[] readStringArrayFromUser(Task task, long arrayPtr) {
        if (arrayPtr == 0) {
            return null;
        }
        
        List<String> strings = new ArrayList<>();
        
        try {
            // 读取指针数组（每个指针 4 字节，简化假设）
            // 实际实现需要根据架构确定指针大小
            int maxElements = 64; // 限制最大元素数
            for (int i = 0; i < maxElements; i++) {
                // 读取指针值（简化：假设指针是 long 类型）
                long ptr = readLongFromUser(task, arrayPtr + i * 8);
                if (ptr == 0) {
                    break; // 数组结束（NULL 指针）
                }
                
                // 读取字符串
                String str = copyStringFromUser(task, ptr, 256);
                if (str != null) {
                    strings.add(str);
                }
            }
        } catch (Exception e) {
            System.err.println("[SYSCALL] Failed to read string array from user space: " + e.getMessage());
            return null;
        }
        
        return strings.toArray(new String[0]);
    }
    
    /**
     * 从用户空间读取 long 值
     */
    private long readLongFromUser(Task task, long userPtr) {
        if (userPtr == 0) {
            return 0;
        }
        
        try {
            byte[] buf = new byte[8];
            int copied = copyFromUser(task, userPtr, buf, 0, 8);
            if (copied < 8) {
                return 0;
            }
            
            // 小端序解析
            long value = 0;
            for (int i = 0; i < 8; i++) {
                value |= ((long) (buf[i] & 0xFF)) << (i * 8);
            }
            return value;
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * 启动已加载的程序
     */
    private void startExecutedProgram(Task task) {
        Runnable executable = task.getExecutable();
        if (executable != null) {
            // 停止旧的执行线程（如果存在）
            Thread oldThread = task.getExecutionThread();
            if (oldThread != null && oldThread.isAlive()) {
                oldThread.interrupt();
            }
            
            // 创建新线程执行
            Thread newThread = new Thread(executable, "task-" + task.getPid());
            task.setExecutionThread(newThread);
            newThread.start();
            
            System.out.println("[SYSCALL] execve() started new program");
        }
    }
    
    // ==================== 文件操作系统调用实现 ====================
    
    /**
     * sys_read - 读取文件
     * 
     * @param task 当前进程
     * @param fd 文件描述符
     * @param bufPtr 用户空间缓冲区指针
     * @param count 要读取的字节数
     * @return 实际读取的字节数，失败返回负数
     */
    private long sysRead(Task task, long fd, long bufPtr, long count) {
        // 标准输入（简化处理）
        if (fd == 0) {
            System.out.println("[SYSCALL] read(fd=0, count=" + count + ") - stub");
            return 0;
        }
        
        // 从文件描述符表获取文件对象
        File file = task.getFdTable().get((int) fd);
        if (file == null) {
            System.err.println("[SYSCALL] read: invalid file descriptor " + fd);
            return -Const.EBADF;
        }
        
        if (count <= 0) {
            return 0;
        }
        
        // 分配内核缓冲区
        byte[] kernelBuf = new byte[(int) count];
        
        // 从 VFS 读取文件数据
        int bytesRead = 0;
        if (vfs != null && file.getInode() != null) {
            bytesRead = vfs.readFileData(file.getInode(), file.getPosition(), 
                kernelBuf, 0, (int) count);
            if (bytesRead < 0) {
                return -Const.EIO;
            }
            
            // 更新文件位置
            file.setPosition(file.getPosition() + bytesRead);
        } else {
            // 回退到 File.read 方法（用于管道等特殊文件）
            bytesRead = file.read(kernelBuf, (int) count);
            if (bytesRead < 0) {
                return bytesRead; // 错误码
            }
        }
        
        // 将数据从内核空间拷贝到用户空间
        if (bytesRead > 0 && bufPtr != 0) {
            int copied = copyToUser(task, bufPtr, kernelBuf, 0, bytesRead);
            if (copied < 0) {
                System.err.println("[SYSCALL] read: failed to copy to user space");
                return -Const.EFAULT;
            }
        }
        
        System.out.println("[SYSCALL] read(fd=" + fd + ", count=" + count + 
            ") returned " + bytesRead + " bytes");
        
        return bytesRead;
    }
    
    /**
     * sys_write - 写入文件
     * 
     * @param task 当前进程
     * @param fd 文件描述符
     * @param bufPtr 用户空间缓冲区指针
     * @param count 要写入的字节数
     * @return 实际写入的字节数，失败返回负数
     */
    private long sysWrite(Task task, long fd, long bufPtr, long count) {
        // 标准输出/错误输出（简化处理）
        if (fd == 1 || fd == 2) {
            if (bufPtr != 0 && count > 0) {
                // 从用户空间读取数据并输出到控制台
                byte[] buf = new byte[(int) count];
                int copied = copyFromUser(task, bufPtr, buf, 0, (int) count);
                if (copied > 0) {
                    String text = new String(buf, 0, copied);
                    if (fd == 1) {
                        System.out.print(text);
                    } else {
                        System.err.print(text);
                    }
                }
            }
            return count;
        }
        
        // 从文件描述符表获取文件对象
        File file = task.getFdTable().get((int) fd);
        if (file == null) {
            System.err.println("[SYSCALL] write: invalid file descriptor " + fd);
            return -Const.EBADF;
        }
        
        if (count <= 0) {
            return 0;
        }
        
        // 从用户空间读取数据到内核缓冲区
        byte[] kernelBuf = new byte[(int) count];
        int copied = copyFromUser(task, bufPtr, kernelBuf, 0, (int) count);
        if (copied < 0) {
            System.err.println("[SYSCALL] write: failed to copy from user space");
            return -Const.EFAULT;
        }
        
        // 写入文件数据到 VFS
        int bytesWritten = 0;
        if (vfs != null && file.getInode() != null) {
            bytesWritten = vfs.writeFileData(file.getInode(), file.getPosition(), 
                kernelBuf, 0, copied);
            if (bytesWritten < 0) {
                return -Const.EIO;
            }
            
            // 更新文件位置
            file.setPosition(file.getPosition() + bytesWritten);
        } else {
            // 回退到 File.write 方法（用于管道等特殊文件）
            bytesWritten = file.write(kernelBuf, copied);
            if (bytesWritten < 0) {
                return bytesWritten; // 错误码
            }
        }
        
        System.out.println("[SYSCALL] write(fd=" + fd + ", count=" + count + 
            ") wrote " + bytesWritten + " bytes");
        
        return bytesWritten;
    }
    
    /**
     * sys_open - 打开文件
     * 
     * @param task 当前进程
     * @param pathPtr 路径名指针（用户空间）
     * @param flags 打开标志（O_RDONLY, O_WRONLY, O_RDWR, O_CREAT, O_TRUNC, O_APPEND）
     * @param mode 创建文件时的权限（仅在 O_CREAT 时有效）
     * @return 文件描述符，失败返回负数
     */
    private long sysOpen(Task task, long pathPtr, long flags, long mode) {
        if (vfs == null) {
            System.err.println("[SYSCALL] open: VFS not initialized");
            return -Const.EINVAL;
        }
        
        // 从用户空间读取路径名
        String path = copyStringFromUser(task, pathPtr, 256);
        if (path == null) {
            System.err.println("[SYSCALL] open: invalid path pointer");
            return -Const.EFAULT;
        }
        
        System.out.println("[SYSCALL] open(\"" + path + "\", flags=0x" + 
            Long.toHexString(flags) + ", mode=0" + Long.toOctalString(mode) + 
            ") called by pid=" + task.getPid());
        
        // 获取当前工作目录
        Inode currentDir = null;
        int cwdIno = task.getCurrentWorkingDir();
        if (cwdIno > 0) {
            currentDir = vfs.getInode(Const.ROOT_DEV, cwdIno);
        }
        if (currentDir == null) {
            currentDir = vfs.getRootInode();
        }
        
        // 解析路径
        Inode inode = vfs.namei(path, currentDir);
        
        // 如果文件不存在且设置了 O_CREAT，创建新文件
        if (inode == null && (flags & File.O_CREAT) != 0) {
            inode = vfs.createFile(path, currentDir, (int) mode);
            if (inode == null) {
                System.err.println("[SYSCALL] open: failed to create file");
                return -Const.ENOENT;
            }
        }
        
        if (inode == null) {
            System.err.println("[SYSCALL] open: file not found: " + path);
            return -Const.ENOENT;
        }
        
        // 检查权限（简化：暂时跳过）
        
        // 如果是 O_TRUNC，截断文件
        if ((flags & File.O_TRUNC) != 0 && inode.isRegularFile()) {
            inode.setSize(0);
            // 清空数据块（简化：只清空第一个直接块）
            int[] directBlocks = inode.getDirectBlocks();
            if (directBlocks[0] != 0) {
                vfs.freeBlock(directBlocks[0]);
                directBlocks[0] = 0;
            }
        }
        
        // 创建文件对象
        File file = new File(inode, (int) flags);
        
        // 分配文件描述符
        int fd = task.getFdTable().allocate(file);
        if (fd < 0) {
            vfs.putInode(inode);
            System.err.println("[SYSCALL] open: no free file descriptor");
            return -Const.EMFILE;
        }
        
        System.out.println("[SYSCALL] open() returned fd=" + fd);
        return fd;
    }
    
    /**
     * sys_close - 关闭文件
     */
    private long sysClose(Task task, long fd, long arg2, long arg3) {
        System.out.println("[SYSCALL] close(fd=" + fd + ")");
        task.getFdTable().close((int) fd);
        return 0;
    }
    
    /**
     * sys_lseek - 定位文件指针
     * 
     * @param task 当前进程
     * @param fd 文件描述符
     * @param offset 偏移量
     * @param whence 起始位置（0=SEEK_SET, 1=SEEK_CUR, 2=SEEK_END）
     * @return 新的文件位置，失败返回 -1
     */
    private long sysLseek(Task task, long fd, long offset, long whence) {
        File file = task.getFdTable().get((int) fd);
        if (file == null) {
            System.err.println("[SYSCALL] lseek: invalid file descriptor " + fd);
            return -Const.EBADF;
        }
        
        long newPos = file.lseek(offset, (int) whence);
        System.out.println("[SYSCALL] lseek(fd=" + fd + ", offset=" + offset + 
            ", whence=" + whence + ") = " + newPos);
        return newPos;
    }
    
    /**
     * sys_creat - 创建文件
     * 等价于 open(path, O_CREAT | O_WRONLY | O_TRUNC, mode)
     * 
     * @param task 当前进程
     * @param pathPtr 路径名指针
     * @param mode 文件权限
     * @return 文件描述符，失败返回负数
     */
    private long sysCreat(Task task, long pathPtr, long mode, long arg3) {
        return sysOpen(task, pathPtr, File.O_CREAT | File.O_WRONLY | File.O_TRUNC, mode);
    }
    
    /**
     * sys_unlink - 删除文件
     * 
     * @param task 当前进程
     * @param pathPtr 路径名指针
     * @return 0 成功，失败返回负数
     */
    private long sysUnlink(Task task, long pathPtr, long arg2, long arg3) {
        if (vfs == null) {
            return -Const.EINVAL;
        }
        
        // 从用户空间读取路径名
        String path = copyStringFromUser(task, pathPtr, 256);
        if (path == null) {
            return -Const.EFAULT;
        }
        
        System.out.println("[SYSCALL] unlink(\"" + path + "\") called by pid=" + task.getPid());
        
        // 获取当前工作目录
        Inode currentDir = null;
        int cwdIno = task.getCurrentWorkingDir();
        if (cwdIno > 0) {
            currentDir = vfs.getInode(Const.ROOT_DEV, cwdIno);
        }
        if (currentDir == null) {
            currentDir = vfs.getRootInode();
        }
        
        // 解析路径
        Inode inode = vfs.namei(path, currentDir);
        if (inode == null) {
            return -Const.ENOENT;
        }
        
        // 检查是否为目录
        if (inode.isDirectory()) {
            vfs.putInode(inode);
            return -Const.EISDIR; // 不能删除目录
        }
        
        // 删除文件
        if (vfs.unlink(path, currentDir, inode)) {
            vfs.putInode(inode);
            System.out.println("[SYSCALL] unlink() succeeded");
            return 0;
        } else {
            vfs.putInode(inode);
            return -Const.EIO;
        }
    }
    
    /**
     * sys_chdir - 改变当前工作目录
     * 
     * @param task 当前进程
     * @param pathPtr 路径名指针
     * @return 0 成功，失败返回负数
     */
    private long sysChdir(Task task, long pathPtr, long arg2, long arg3) {
        if (vfs == null) {
            return -Const.EINVAL;
        }
        
        // 从用户空间读取路径名
        String path = copyStringFromUser(task, pathPtr, 256);
        if (path == null) {
            return -Const.EFAULT;
        }
        
        System.out.println("[SYSCALL] chdir(\"" + path + "\") called by pid=" + task.getPid());
        
        // 获取当前工作目录
        Inode currentDir = null;
        int cwdIno = task.getCurrentWorkingDir();
        if (cwdIno > 0) {
            currentDir = vfs.getInode(Const.ROOT_DEV, cwdIno);
        }
        if (currentDir == null) {
            currentDir = vfs.getRootInode();
        }
        
        // 解析路径
        Inode newDir = vfs.namei(path, currentDir);
        if (newDir == null) {
            return -Const.ENOENT;
        }
        
        // 检查是否为目录
        if (!newDir.isDirectory()) {
            vfs.putInode(newDir);
            return -Const.ENOTDIR;
        }
        
        // 设置新的当前工作目录
        task.setCurrentWorkingDir(newDir.getIno());
        vfs.putInode(newDir);
        
        System.out.println("[SYSCALL] chdir() succeeded, new cwd ino=" + newDir.getIno());
        return 0;
    }
    
    /**
     * sys_mkdir - 创建目录
     * 
     * @param task 当前进程
     * @param pathPtr 路径名指针
     * @param mode 目录权限
     * @return 0 成功，失败返回负数
     */
    private long sysMkdir(Task task, long pathPtr, long mode, long arg3) {
        if (vfs == null) {
            return -Const.EINVAL;
        }
        
        // 从用户空间读取路径名
        String path = copyStringFromUser(task, pathPtr, 256);
        if (path == null) {
            return -Const.EFAULT;
        }
        
        System.out.println("[SYSCALL] mkdir(\"" + path + "\", mode=0" + 
            Long.toOctalString(mode) + ") called by pid=" + task.getPid());
        
        // 获取当前工作目录
        Inode currentDir = null;
        int cwdIno = task.getCurrentWorkingDir();
        if (cwdIno > 0) {
            currentDir = vfs.getInode(Const.ROOT_DEV, cwdIno);
        }
        if (currentDir == null) {
            currentDir = vfs.getRootInode();
        }
        
        // 创建目录
        Inode newDir = vfs.createDirectory(path, currentDir, (int) mode);
        if (newDir == null) {
            return -Const.EEXIST; // 目录已存在或其他错误
        }
        
        vfs.putInode(newDir);
        System.out.println("[SYSCALL] mkdir() succeeded");
        return 0;
    }
    
    /**
     * sys_rmdir - 删除目录
     * 
     * @param task 当前进程
     * @param pathPtr 路径名指针
     * @return 0 成功，失败返回负数
     */
    private long sysRmdir(Task task, long pathPtr, long arg2, long arg3) {
        if (vfs == null) {
            return -Const.EINVAL;
        }
        
        // 从用户空间读取路径名
        String path = copyStringFromUser(task, pathPtr, 256);
        if (path == null) {
            return -Const.EFAULT;
        }
        
        System.out.println("[SYSCALL] rmdir(\"" + path + "\") called by pid=" + task.getPid());
        
        // 获取当前工作目录
        Inode currentDir = null;
        int cwdIno = task.getCurrentWorkingDir();
        if (cwdIno > 0) {
            currentDir = vfs.getInode(Const.ROOT_DEV, cwdIno);
        }
        if (currentDir == null) {
            currentDir = vfs.getRootInode();
        }
        
        // 解析路径
        Inode dir = vfs.namei(path, currentDir);
        if (dir == null) {
            return -Const.ENOENT;
        }
        
        // 检查是否为目录
        if (!dir.isDirectory()) {
            vfs.putInode(dir);
            return -Const.ENOTDIR;
        }
        
        // 检查目录是否为空（简化：只检查是否有 . 和 ..）
        // 实际应该检查是否有其他目录项
        
        // 删除目录
        if (vfs.unlink(path, currentDir, dir)) {
            vfs.putInode(dir);
            System.out.println("[SYSCALL] rmdir() succeeded");
            return 0;
        } else {
            vfs.putInode(dir);
            return -Const.EIO; // 目录非空或其他错误
        }
    }
    
    /**
     * sys_stat - 获取文件状态
     * 
     * @param task 当前进程
     * @param pathPtr 路径名指针
     * @param statPtr 用户空间 stat 结构指针
     * @return 0 成功，失败返回负数
     */
    private long sysStat(Task task, long pathPtr, long statPtr, long arg3) {
        if (vfs == null) {
            return -Const.EINVAL;
        }
        
        // 从用户空间读取路径名
        String path = copyStringFromUser(task, pathPtr, 256);
        if (path == null) {
            return -Const.EFAULT;
        }
        
        System.out.println("[SYSCALL] stat(\"" + path + "\") called by pid=" + task.getPid());
        
        // 获取当前工作目录
        Inode currentDir = null;
        int cwdIno = task.getCurrentWorkingDir();
        if (cwdIno > 0) {
            currentDir = vfs.getInode(Const.ROOT_DEV, cwdIno);
        }
        if (currentDir == null) {
            currentDir = vfs.getRootInode();
        }
        
        // 解析路径
        Inode inode = vfs.namei(path, currentDir);
        if (inode == null) {
            return -Const.ENOENT;
        }
        
        // 填充 stat 结构
        Types.Stat stat = new Types.Stat();
        stat.st_dev = inode.getDev();
        stat.st_ino = inode.getIno();
        stat.st_mode = inode.getMode();
        stat.st_nlink = inode.getNlink();
        stat.st_uid = inode.getUid();
        stat.st_gid = inode.getGid();
        stat.st_rdev = 0; // 非设备文件
        stat.st_size = inode.getSize();
        stat.st_atime = inode.getAtime();
        stat.st_mtime = inode.getMtime();
        stat.st_ctime = inode.getCtime();
        
        // 将 stat 结构写入用户空间
        byte[] statBytes = stat.toBytes();
        int copied = copyToUser(task, statPtr, statBytes, 0, statBytes.length);
        if (copied < 0) {
            vfs.putInode(inode);
            return -Const.EFAULT;
        }
        
        vfs.putInode(inode);
        System.out.println("[SYSCALL] stat() succeeded");
        return 0;
    }
    
    /**
     * sys_fstat - 通过文件描述符获取文件状态
     * 
     * @param task 当前进程
     * @param fd 文件描述符
     * @param statPtr 用户空间 stat 结构指针
     * @return 0 成功，失败返回负数
     */
    private long sysFstat(Task task, long fd, long statPtr, long arg3) {
        // 从文件描述符表获取文件对象
        File file = task.getFdTable().get((int) fd);
        if (file == null) {
            System.err.println("[SYSCALL] fstat: invalid file descriptor " + fd);
            return -Const.EBADF;
        }
        
        Inode inode = file.getInode();
        if (inode == null) {
            return -Const.EBADF;
        }
        
        System.out.println("[SYSCALL] fstat(fd=" + fd + ") called by pid=" + task.getPid());
        
        // 填充 stat 结构
        Types.Stat stat = new Types.Stat();
        stat.st_dev = inode.getDev();
        stat.st_ino = inode.getIno();
        stat.st_mode = inode.getMode();
        stat.st_nlink = inode.getNlink();
        stat.st_uid = inode.getUid();
        stat.st_gid = inode.getGid();
        stat.st_rdev = 0; // 非设备文件
        stat.st_size = inode.getSize();
        stat.st_atime = inode.getAtime();
        stat.st_mtime = inode.getMtime();
        stat.st_ctime = inode.getCtime();
        
        // 将 stat 结构写入用户空间
        byte[] statBytes = stat.toBytes();
        int copied = copyToUser(task, statPtr, statBytes, 0, statBytes.length);
        if (copied < 0) {
            return -Const.EFAULT;
        }
        
        System.out.println("[SYSCALL] fstat() succeeded");
        return 0;
    }
    
    // ==================== 内存管理系统调用实现 ====================
    
    /**
     * sys_brk - 设置堆结束地址
     */
    private long sysBrk(Task task, long newBrk, long arg2, long arg3) {
        System.out.println("[SYSCALL] brk(0x" + Long.toHexString(newBrk) + ") called by pid=" + task.getPid());
        
        long result = task.getAddressSpace().expandBrk(newBrk);
        
        System.out.println("[SYSCALL] brk() returned 0x" + Long.toHexString(result));
        return result;
    }
    
    // ==================== 时间系统调用实现 ====================
    
    /**
     * sys_time - 获取系统时间
     */
    private long sysTime(Task task, long timePtr, long arg2, long arg3) {
        long currentTime = System.currentTimeMillis() / 1000; // Unix 时间戳（秒）
        return currentTime;
    }
    
    /**
     * sys_sync - 同步文件系统缓冲区
     * 
     * @param task 当前进程
     * @return 0 成功
     */
    private long sysSync(Task task, long arg1, long arg2, long arg3) {
        System.out.println("[SYSCALL] sync() called by pid=" + task.getPid());
        if (vfs != null) {
            vfs.sync();
        }
        return 0;
    }
    
    /**
     * sys_times - 获取进程时间
     * 
     * @param task 当前进程
     * @param tmsPtr 用户空间 tms 结构指针
     * @return 从系统启动到现在的时钟滴答数
     */
    private long sysTimes(Task task, long tmsPtr, long arg2, long arg3) {
        System.out.println("[SYSCALL] times() called by pid=" + task.getPid());
        
        // 填充 tms 结构
        Types.Tms tms = new Types.Tms();
        tms.tms_utime = task.getUtime(); // 用户态时间
        tms.tms_stime = task.getStime(); // 内核态时间
        tms.tms_cutime = 0; // 简化：子进程时间暂不统计
        tms.tms_cstime = 0;
        
        // 将 tms 结构写入用户空间
        if (tmsPtr != 0) {
            byte[] tmsBytes = tms.toBytes();
            int copied = copyToUser(task, tmsPtr, tmsBytes, 0, tmsBytes.length);
            if (copied < 0) {
                return -Const.EFAULT;
            }
        }
        
        // 返回从系统启动到现在的时钟滴答数（简化：使用毫秒数）
        long currentTime = System.currentTimeMillis();
        long startTime = task.getStartTime();
        long ticks = (currentTime - startTime) / 10; // 假设 100 HZ，每 10ms 一个滴答
        
        System.out.println("[SYSCALL] times() returned " + ticks + " ticks");
        return ticks;
    }
    
    /**
     * sys_dup - 复制文件描述符
     * 返回新的文件描述符，指向同一个文件
     * 
     * @param task 当前进程
     * @param oldfd 旧的文件描述符
     * @return 新的文件描述符，失败返回负数
     */
    private long sysDup(Task task, long oldfd, long arg2, long arg3) {
        File oldFile = task.getFdTable().get((int) oldfd);
        if (oldFile == null) {
            System.err.println("[SYSCALL] dup: invalid file descriptor " + oldfd);
            return -Const.EBADF;
        }
        
        // 分配新的文件描述符
        int newfd = task.getFdTable().allocate(oldFile);
        if (newfd < 0) {
            System.err.println("[SYSCALL] dup: no free file descriptor");
            return -Const.EMFILE;
        }
        
        // 增加文件引用计数
        oldFile.incrementRef();
        
        System.out.println("[SYSCALL] dup(" + oldfd + ") = " + newfd);
        return newfd;
    }
    
    /**
     * sys_dup2 - 复制文件描述符到指定编号
     * 
     * @param task 当前进程
     * @param oldfd 旧的文件描述符
     * @param newfd 新的文件描述符编号
     * @return 新的文件描述符，失败返回负数
     */
    private long sysDup2(Task task, long oldfd, long newfd, long arg3) {
        File oldFile = task.getFdTable().get((int) oldfd);
        if (oldFile == null) {
            System.err.println("[SYSCALL] dup2: invalid file descriptor " + oldfd);
            return -Const.EBADF;
        }
        
        // 如果 newfd 已经打开，先关闭它
        File existingFile = task.getFdTable().get((int) newfd);
        if (existingFile != null) {
            task.getFdTable().close((int) newfd);
        }
        
        // 在指定位置分配文件描述符
        if (!task.getFdTable().set((int) newfd, oldFile)) {
            System.err.println("[SYSCALL] dup2: failed to set file descriptor " + newfd);
            return -Const.EBADF;
        }
        
        // 增加文件引用计数
        oldFile.incrementRef();
        
        System.out.println("[SYSCALL] dup2(" + oldfd + ", " + newfd + ") = " + newfd);
        return newfd;
    }
    
    // ==================== 信号系统调用实现 ====================
    
    /**
     * sys_pause - 暂停进程直到收到信号
     */
    private long sysPause(Task task, long arg1, long arg2, long arg3) {
        System.out.println("[SYSCALL] pause() called by pid=" + task.getPid());
        
        // 设置进程为可中断睡眠
        task.sleep(true);
        scheduler.schedule();
        
        // pause 总是返回 -1，错误码为 EINTR
        return -Const.EINTR;
    }
    
    /**
     * sys_signal - 设置信号处理器
     * 
     * @param task 当前进程
     * @param signum 信号编号
     * @param handler 处理器地址（SIG_DFL、SIG_IGN 或自定义地址）
     * @return 旧的处理器地址
     */
    private long sysSignal(Task task, long signum, long handler, long arg3) {
        System.out.println("[SYSCALL] signal(" + signum + ", " + handler + ") called by pid=" + task.getPid());
        
        // 检查信号编号有效性
        if (signum < 1 || signum >= Signal.NSIG) {
            System.err.println("[SYSCALL] signal: invalid signal number " + signum);
            return -Const.EINVAL;
        }
        
        // SIGKILL 和 SIGSTOP 不能被捕获或忽略
        if (signum == Signal.SIGKILL || signum == Signal.SIGSTOP) {
            System.err.println("[SYSCALL] signal: cannot catch or ignore SIGKILL/SIGSTOP");
            return -Const.EINVAL;
        }
        
        // 获取旧的处理器
        Task.SignalHandlerEntry[] handlers = task.getSignalHandlers();
        long oldHandler = handlers[(int) signum].getHandler();
        
        // 设置新的处理器
        handlers[(int) signum].setHandler(handler);
        handlers[(int) signum].setCustomHandler(null);
        
        System.out.println("[SYSCALL] signal() set handler for " + Signal.getSignalName((int) signum) + 
            ", old=" + oldHandler + ", new=" + handler);
        
        return oldHandler;
    }
    
    /**
     * sys_kill - 发送信号到进程
     * 
     * @param task 当前进程
     * @param pid 目标进程 PID
     * @param signum 信号编号
     * @return 0 成功，-1 失败
     */
    private long sysKill(Task task, long pid, long signum, long arg3) {
        System.out.println("[SYSCALL] kill(" + pid + ", " + Signal.getSignalName((int) signum) + 
            ") called by pid=" + task.getPid());
        
        // 检查信号编号有效性
        if (signum < 0 || signum >= Signal.NSIG) {
            System.err.println("[SYSCALL] kill: invalid signal number " + signum);
            return -Const.EINVAL;
        }
        
        // 查找目标进程
        Task target = scheduler.findTask((int) pid);
        if (target == null) {
            System.err.println("[SYSCALL] kill: process " + pid + " not found");
            return -Const.ESRCH;
        }
        
        // 发送信号
        if (signum > 0) {
            target.sendSignal((int) signum);
            System.out.println("[SYSCALL] kill() sent " + Signal.getSignalName((int) signum) + 
                " to pid=" + pid);
        }
        
        return 0;
    }
    
    /**
     * 处理进程的待处理信号
     * 应在系统调用返回前或调度器中调用
     * 
     * @param task 要检查的进程
     */
    public void processSignals(Task task) {
        if (task == null || !task.hasPendingSignals()) {
            return;
        }
        
        while (task.hasPendingSignals()) {
            int signum = task.getNextSignal();
            if (signum < 0) {
                break;
            }
            
            // 清除信号位
            task.clearSignal(signum);
            
            // 获取处理器
            Task.SignalHandlerEntry[] handlers = task.getSignalHandlers();
            long handler = handlers[signum].getHandler();
            Signal.SignalHandler customHandler = handlers[signum].getCustomHandler();
            
            System.out.println("[SIGNAL] Processing " + Signal.getSignalName(signum) + 
                " for pid=" + task.getPid() + ", handler=" + handler);
            
            // 执行处理
            if (handler == Signal.SIG_IGN) {
                // 忽略信号
                System.out.println("[SIGNAL] Ignored " + Signal.getSignalName(signum));
                continue;
                
            } else if (handler == Signal.SIG_DFL) {
                // 默认处理
                Signal.SignalAction action = Signal.getDefaultAction(signum);
                handleDefaultSignalAction(task, signum, action);
                
            } else if (customHandler != null) {
                // 自定义处理器
                try {
                    customHandler.handle(signum);
                } catch (Exception e) {
                    System.err.println("[SIGNAL] Exception in custom handler: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 执行信号的默认行为
     */
    private void handleDefaultSignalAction(Task task, int signum, Signal.SignalAction action) {
        switch (action) {
            case IGNORE:
                System.out.println("[SIGNAL] Default action: ignore " + Signal.getSignalName(signum));
                break;
                
            case TERMINATE:
                System.out.println("[SIGNAL] Default action: terminate pid=" + task.getPid() + 
                    " by " + Signal.getSignalName(signum));
                task.exit(128 + signum); // 约定：信号终止的退出码 = 128 + signum
                scheduler.schedule();
                break;
                
            case STOP:
                System.out.println("[SIGNAL] Default action: stop pid=" + task.getPid());
                task.setState(Const.TASK_STOPPED);
                scheduler.schedule();
                break;
                
            case CORE_DUMP:
                System.out.println("[SIGNAL] Default action: core dump pid=" + task.getPid() + 
                    " by " + Signal.getSignalName(signum));
                // 简化：不实际生成 core 文件，直接终止
                task.exit(128 + signum);
                scheduler.schedule();
                break;
                
            case CONTINUE:
                System.out.println("[SIGNAL] Default action: continue pid=" + task.getPid());
                if (task.getState() == Const.TASK_STOPPED) {
                    task.setState(Const.TASK_RUNNING);
                }
                break;
        }
    }
    
    // ==================== 进程间通信系统调用 ====================
    
    /**
     * sys_pipe - 创建管道
     * 
     * @param task 当前进程
     * @param fdArray 文件描述符数组指针（简化：返回值编码 fd[0] 和 fd[1]）
     * @return 0 成功，-1 失败
     */
    private long sysPipe(Task task, long fdArray, long arg2, long arg3) {
        System.out.println("[SYSCALL] pipe() called by pid=" + task.getPid());
        
        // 创建管道
        Pipe pipe = new Pipe();
        
        // 创建读端和写端文件描述符
        PipeFile readEnd = new PipeFile(pipe, true);
        PipeFile writeEnd = new PipeFile(pipe, false);
        
        // 分配文件描述符
        // 由于FileDescriptorTable只支持File类型，我们需要创建一个包装
        // 简化方案：创建一个PipeFileWrapper继承或包装File
        // 更简单的方案：修改FileDescriptorTable支持Object类型
        
        // 当前简化实现：创建两个特殊的File对象来包装PipeFile
        // 注意：这需要File类支持PipeFile，或者我们需要修改架构
        // 为了不破坏现有代码，这里先创建一个简单的包装
        
        // 分配读端文件描述符
        int readFd = task.getFdTable().allocate(createPipeFileWrapper(readEnd));
        if (readFd < 0) {
            System.err.println("[SYSCALL] pipe: failed to allocate read fd");
            return -Const.EMFILE;
        }
        
        // 分配写端文件描述符
        int writeFd = task.getFdTable().allocate(createPipeFileWrapper(writeEnd));
        if (writeFd < 0) {
            System.err.println("[SYSCALL] pipe: failed to allocate write fd");
            task.getFdTable().close(readFd);
            return -Const.EMFILE;
        }
        
        // 将文件描述符写入用户空间（简化：这里不实际写入）
        // 实际实现需要将readFd和writeFd写入task地址空间的fdArray位置
        
        System.out.println("[SYSCALL] pipe() created: " + pipe + 
            ", readFd=" + readFd + ", writeFd=" + writeFd);
        
        // 简化：返回 0 表示成功
        // 实际上应该返回 fd[0] 和 fd[1]，但需要修改系统调用接口
        return 0;
    }
    
    /**
     * 创建管道文件包装器
     * 将PipeFile包装成File对象以便FileDescriptorTable使用
     * 
     * @param pipeFile 管道文件
     * @return File对象包装
     */
    private File createPipeFileWrapper(PipeFile pipeFile) {
        // 创建一个特殊的File对象，内部持有PipeFile引用
        // 由于File类没有PipeFile字段，我们需要扩展它
        // 简化方案：创建一个PipeFileFile类继承File
        
        // 为了不修改File类，我们创建一个匿名内部类或使用反射
        // 更简单的方案：修改FileDescriptorTable使其支持Object类型
        // 但为了保持兼容性，这里创建一个包装File
        
        // 返回一个特殊的File对象，其read/write方法会委托给PipeFile
        return new PipeFileWrapper(pipeFile);
    }
    
    /**
     * 管道文件包装器类
     * 将PipeFile包装成File以便在FileDescriptorTable中使用
     */
    private static class PipeFileWrapper extends File {
        private final PipeFile pipeFile;
        
        public PipeFileWrapper(PipeFile pipeFile) {
            super(null, O_RDWR); // 管道支持读写
            this.pipeFile = pipeFile;
        }
        
        @Override
        public int read(byte[] buf, int count) {
            return pipeFile.read(buf, count);
        }
        
        @Override
        public int write(byte[] buf, int count) {
            return pipeFile.write(buf, count);
        }
        
        @Override
        public String toString() {
            return "PipeFileWrapper[" + pipeFile + "]";
        }
    }
}
