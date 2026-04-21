package jinux.kernel.syscall;

import jinux.include.ErrorCode;
import jinux.include.ProcessConstants;
import jinux.include.FileSystemConstants;
import jinux.include.Syscalls;
import jinux.kernel.Task;
import jinux.kernel.Scheduler;
import jinux.kernel.Signal;
import jinux.kernel.SystemCallDispatcher;
import jinux.mm.IMemoryManager;
import jinux.fs.VirtualFileSystem;
import jinux.fs.Inode;
import jinux.exec.ProgramLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 进程管理系统调用
 * 从 SystemCallDispatcher 中拆分出的进程管理相关系统调用实现
 * 
 * @author Jinux Project
 */
public class ProcessSyscalls {

    private final Scheduler scheduler;
    private final IMemoryManager memoryManager;
    private VirtualFileSystem vfs;



    public ProcessSyscalls(Scheduler scheduler, IMemoryManager memoryManager) {
        this.scheduler = scheduler;
        this.memoryManager = memoryManager;
    }

    public void setVfs(VirtualFileSystem vfs) {
        this.vfs = vfs;
    }

    /**
     * 注册进程管理相关的系统调用处理器
     */
    public void registerHandlers(Map<Integer, SystemCallDispatcher.SystemCallHandler> handlers) {
        handlers.put(Syscalls.SYS_FORK, this::sysFork);
        handlers.put(Syscalls.SYS_EXIT, this::sysExit);
        handlers.put(Syscalls.SYS_WAIT, this::sysWait);
        handlers.put(Syscalls.SYS_EXECVE, this::sysExecve);
        handlers.put(Syscalls.SYS_GETPID, this::sysGetpid);
        handlers.put(Syscalls.SYS_GETPPID, this::sysGetppid);
        handlers.put(Syscalls.SYS_PAUSE, this::sysPause);
    }

    // ==================== 系统调用实现 ====================

    /**
     * sys_fork - 创建子进程
     */
    private long sysFork(Task parent, long arg1, long arg2, long arg3) {
        System.out.println("[SYSCALL] fork() called by pid=" + parent.getPid());

        int childPid = scheduler.allocatePid();

        var childAddrSpace = parent.getAddressSpace().copy();

        Task child = new Task(childPid, parent.getPid(), childAddrSpace);
        child.setPriority(parent.getPriority());
        child.setCounter(parent.getCounter());

        child.setFdTable(parent.getFdTable().copy());
        child.setCurrentWorkingDir(parent.getCurrentWorkingDir());

        if (!scheduler.addTask(child)) {
            childAddrSpace.free();
            return -ErrorCode.ENOMEM;
        }

        System.out.println("[SYSCALL] fork() created child pid=" + childPid);
        return childPid;
    }

    /**
     * sys_exit - 退出进程
     */
    private long sysExit(Task task, long exitCode, long arg2, long arg3) {
        System.out.println("[SYSCALL] exit(" + exitCode + ") called by pid=" + task.getPid());

        task.exit((int) exitCode);

        Task parent = scheduler.findTask(task.getPpid());
        if (parent != null && parent.getWaitingForPid() == task.getPid()) {
            parent.wakeUp();
        }

        scheduler.schedule();
        return 0;
    }

    /**
     * sys_wait - 等待子进程
     */
    private long sysWait(Task task, long statusPtr, long arg2, long arg3) {
        System.out.println("[SYSCALL] wait() called by pid=" + task.getPid());

        for (Task candidate : scheduler.getTaskTable()) {
            if (candidate != null && candidate.getPpid() == task.getPid()
                    && candidate.getState() == ProcessConstants.TASK_ZOMBIE) {
                int childPid = candidate.getPid();
                scheduler.removeTask(childPid);
                System.out.println("[SYSCALL] wait() collected zombie child pid=" + childPid);
                return childPid;
            }
        }

        task.sleep(true);
        scheduler.schedule();
        return -ErrorCode.EINTR;
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
     */
    private long sysExecve(Task task, long filename, long argv, long envp) {
        String programPath = copyStringFromUser(task, filename, 256);
        if (programPath == null || programPath.isEmpty()) {
            System.err.println("[SYSCALL] execve: invalid filename pointer");
            return -ErrorCode.EFAULT;
        }

        System.out.println("[SYSCALL] execve(\"" + programPath + "\") called by pid=" + task.getPid());

        String[] args = readStringArrayFromUser(task, argv);
        String[] env = readStringArrayFromUser(task, envp);

        if (vfs != null) {
            Inode currentDir = resolveCurrentDir(task);
            Inode programInode = vfs.resolve(programPath, currentDir);
            if (programInode != null && programInode.isRegularFile()) {
                String programName = extractProgramName(programPath);
                int result = ProgramLoader.loadProgram(task, programName, args, env);
                if (result >= 0) {
                    vfs.putInode(programInode);
                    startExecutedProgram(task);
                    return 0;
                }
                vfs.putInode(programInode);
            }
        }

        String programName = extractProgramName(programPath);
        int result = ProgramLoader.loadProgram(task, programName, args, env);

        if (result < 0) {
            System.err.println("[SYSCALL] execve() failed: program not found");
            return -ErrorCode.ENOENT;
        }

        startExecutedProgram(task);
        return 0;
    }

    /**
     * sys_pause - 暂停进程直到收到信号
     */
    private long sysPause(Task task, long arg1, long arg2, long arg3) {
        System.out.println("[SYSCALL] pause() called by pid=" + task.getPid());
        task.sleep(true);
        scheduler.schedule();
        return -ErrorCode.EINTR;
    }

    // ==================== 辅助方法 ====================

    private Inode resolveCurrentDir(Task task) {
        Inode currentDir = null;
        int cwdIno = task.getCurrentWorkingDir();
        if (cwdIno > 0 && vfs != null) {
            currentDir = vfs.getInode(FileSystemConstants.ROOT_DEV, cwdIno);
        }
        if (currentDir == null && vfs != null) {
            currentDir = vfs.getRootInode();
        }
        return currentDir;
    }

    private String extractProgramName(String path) {
        if (path == null || path.isEmpty()) {
            return "unknown";
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash >= 0 && lastSlash < path.length() - 1) {
            return path.substring(lastSlash + 1);
        }
        return path;
    }

    private String[] readStringArrayFromUser(Task task, long arrayPtr) {
        if (arrayPtr == 0) {
            return null;
        }

        List<String> strings = new ArrayList<>();
        try {
            for (int i = 0; i < ProcessConstants.MAX_EXEC_ARGS; i++) {
                long ptr = readLongFromUser(task, arrayPtr + i * 8);
                if (ptr == 0) {
                    break;
                }
                String str = copyStringFromUser(task, ptr, FileSystemConstants.MAX_PATH_LENGTH);
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
            long value = 0;
            for (int i = 0; i < 8; i++) {
                value |= ((long) (buf[i] & 0xFF)) << (i * 8);
            }
            return value;
        } catch (Exception e) {
            return 0;
        }
    }

    private void startExecutedProgram(Task task) {
        Runnable executable = task.getExecutable();
        if (executable != null) {
            Thread oldThread = task.getExecutionThread();
            if (oldThread != null && oldThread.isAlive()) {
                oldThread.interrupt();
            }
            Thread newThread = new Thread(executable, "task-" + task.getPid());
            task.setExecutionThread(newThread);
            newThread.start();
            System.out.println("[SYSCALL] execve() started new program");
        }
    }

    String copyStringFromUser(Task task, long userPtr, int maxLen) {
        return UserSpaceCopier.copyStringFromUser(task, userPtr, maxLen);
    }

    int copyFromUser(Task task, long userPtr, byte[] buf, int offset, int len) {
        return UserSpaceCopier.copyFromUser(task, userPtr, buf, offset, len);
    }

    int copyToUser(Task task, long userPtr, byte[] buf, int offset, int len) {
        return UserSpaceCopier.copyToUser(task, userPtr, buf, offset, len);
    }
}
