package jinux.kernel.syscall;

import jinux.include.ErrorCode;
import jinux.include.Syscalls;
import jinux.kernel.Task;
import jinux.kernel.SystemCallDispatcher;
import jinux.fs.File;
import jinux.ipc.Pipe;
import jinux.ipc.PipeFile;

import java.util.Map;

/**
 * 进程间通信系统调用
 * 从 SystemCallDispatcher 中拆分出的 IPC 相关系统调用实现
 * 
 * @author Jinux Project
 */
public class IpcSyscalls {

    public IpcSyscalls() {
    }

    /**
     * 注册 IPC 相关的系统调用处理器
     */
    public void registerHandlers(Map<Integer, SystemCallDispatcher.SystemCallHandler> handlers) {
        handlers.put(Syscalls.SYS_PIPE, this::sysPipe);
    }

    // ==================== 系统调用实现 ====================

    /**
     * sys_pipe - 创建管道
     */
    private long sysPipe(Task task, long fdArray, long arg2, long arg3) {
        System.out.println("[SYSCALL] pipe() called by pid=" + task.getPid());

        Pipe pipe = new Pipe();

        PipeFile readEnd = new PipeFile(pipe, true);
        PipeFile writeEnd = new PipeFile(pipe, false);

        int readFd = task.getFdTable().allocate(createPipeFileWrapper(readEnd));
        if (readFd < 0) {
            System.err.println("[SYSCALL] pipe: failed to allocate read fd");
            return -ErrorCode.EMFILE;
        }

        int writeFd = task.getFdTable().allocate(createPipeFileWrapper(writeEnd));
        if (writeFd < 0) {
            System.err.println("[SYSCALL] pipe: failed to allocate write fd");
            task.getFdTable().close(readFd);
            return -ErrorCode.EMFILE;
        }

        System.out.println("[SYSCALL] pipe() created: " + pipe +
                ", readFd=" + readFd + ", writeFd=" + writeFd);
        return 0;
    }

    /**
     * 创建管道文件包装器
     */
    private File createPipeFileWrapper(PipeFile pipeFile) {
        return new PipeFileWrapper(pipeFile);
    }

    /**
     * 管道文件包装器类
     * 将 PipeFile 包装成 File 以便在 FileDescriptorTable 中使用
     */
    private static class PipeFileWrapper extends File {
        private final PipeFile pipeFile;

        public PipeFileWrapper(PipeFile pipeFile) {
            super(null, O_RDWR);
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
