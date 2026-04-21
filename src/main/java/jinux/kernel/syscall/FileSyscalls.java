package jinux.kernel.syscall;

import jinux.include.FileSystemConstants;
import jinux.include.ErrorCode;
import jinux.include.Syscalls;
import jinux.include.Types;
import jinux.kernel.Task;
import jinux.kernel.SystemCallDispatcher;
import jinux.fs.VirtualFileSystem;
import jinux.fs.File;
import jinux.fs.Inode;

import java.util.Map;

/**
 * 文件操作系统调用
 * 从 SystemCallDispatcher 中拆分出的文件操作相关系统调用实现
 * 
 * @author Jinux Project
 */
public class FileSyscalls {

    private VirtualFileSystem vfs;



    public FileSyscalls() {
    }

    public void setVfs(VirtualFileSystem vfs) {
        this.vfs = vfs;
    }

    /**
     * 注册文件操作相关的系统调用处理器
     */
    public void registerHandlers(Map<Integer, SystemCallDispatcher.SystemCallHandler> handlers) {
        handlers.put(Syscalls.SYS_READ, this::sysRead);
        handlers.put(Syscalls.SYS_WRITE, this::sysWrite);
        handlers.put(Syscalls.SYS_OPEN, this::sysOpen);
        handlers.put(Syscalls.SYS_CLOSE, this::sysClose);
        handlers.put(Syscalls.SYS_LSEEK, this::sysLseek);
        handlers.put(Syscalls.SYS_CREAT, this::sysCreat);
        handlers.put(Syscalls.SYS_UNLINK, this::sysUnlink);
        handlers.put(Syscalls.SYS_STAT, this::sysStat);
        handlers.put(Syscalls.SYS_FSTAT, this::sysFstat);
        handlers.put(Syscalls.SYS_CHDIR, this::sysChdir);
        handlers.put(Syscalls.SYS_MKDIR, this::sysMkdir);
        handlers.put(Syscalls.SYS_RMDIR, this::sysRmdir);
        handlers.put(Syscalls.SYS_DUP, this::sysDup);
        handlers.put(Syscalls.SYS_DUP2, this::sysDup2);
        handlers.put(Syscalls.SYS_SYNC, this::sysSync);
    }

    // ==================== 辅助方法 ====================

    /**
     * 解析当前工作目录 inode（消除重复代码）
     */
    private Inode resolveCurrentDir(Task task) {
        Inode currentDir = null;
        int cwdIno = task.getCurrentWorkingDir();
        if (cwdIno > 0) {
            currentDir = vfs.getInode(FileSystemConstants.ROOT_DEV, cwdIno);
        }
        if (currentDir == null) {
            currentDir = vfs.getRootInode();
        }
        return currentDir;
    }

    private String copyStringFromUser(Task task, long userPtr, int maxLen) {
        return UserSpaceCopier.copyStringFromUser(task, userPtr, maxLen);
    }

    private int copyFromUser(Task task, long userPtr, byte[] buf, int offset, int len) {
        return UserSpaceCopier.copyFromUser(task, userPtr, buf, offset, len);
    }

    private int copyToUser(Task task, long userPtr, byte[] buf, int offset, int len) {
        return UserSpaceCopier.copyToUser(task, userPtr, buf, offset, len);
    }

    // ==================== 系统调用实现 ====================

    private long sysRead(Task task, long fd, long bufPtr, long count) {
        if (fd == 0) {
            System.out.println("[SYSCALL] read(fd=0, count=" + count + ") - stub");
            return 0;
        }

        File file = task.getFdTable().get((int) fd);
        if (file == null) {
            System.err.println("[SYSCALL] read: invalid file descriptor " + fd);
            return -ErrorCode.EBADF;
        }

        if (count <= 0) {
            return 0;
        }

        byte[] kernelBuf = new byte[(int) count];

        int bytesRead = 0;
        if (vfs != null && file.getInode() != null) {
            bytesRead = vfs.readFileData(file.getInode(), file.getPosition(),
                    kernelBuf, 0, (int) count);
            if (bytesRead < 0) {
                return -ErrorCode.EIO;
            }
            file.setPosition(file.getPosition() + bytesRead);
        } else {
            bytesRead = file.read(kernelBuf, (int) count);
            if (bytesRead < 0) {
                return bytesRead;
            }
        }

        if (bytesRead > 0 && bufPtr != 0) {
            int copied = copyToUser(task, bufPtr, kernelBuf, 0, bytesRead);
            if (copied < 0) {
                System.err.println("[SYSCALL] read: failed to copy to user space");
                return -ErrorCode.EFAULT;
            }
        }

        System.out.println("[SYSCALL] read(fd=" + fd + ", count=" + count +
                ") returned " + bytesRead + " bytes");
        return bytesRead;
    }

    private long sysWrite(Task task, long fd, long bufPtr, long count) {
        if (fd == 1 || fd == 2) {
            if (bufPtr != 0 && count > 0) {
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

        File file = task.getFdTable().get((int) fd);
        if (file == null) {
            System.err.println("[SYSCALL] write: invalid file descriptor " + fd);
            return -ErrorCode.EBADF;
        }

        if (count <= 0) {
            return 0;
        }

        byte[] kernelBuf = new byte[(int) count];
        int copied = copyFromUser(task, bufPtr, kernelBuf, 0, (int) count);
        if (copied < 0) {
            System.err.println("[SYSCALL] write: failed to copy from user space");
            return -ErrorCode.EFAULT;
        }

        int bytesWritten = 0;
        if (vfs != null && file.getInode() != null) {
            bytesWritten = vfs.writeFileData(file.getInode(), file.getPosition(),
                    kernelBuf, 0, copied);
            if (bytesWritten < 0) {
                return -ErrorCode.EIO;
            }
            file.setPosition(file.getPosition() + bytesWritten);
        } else {
            bytesWritten = file.write(kernelBuf, copied);
            if (bytesWritten < 0) {
                return bytesWritten;
            }
        }

        System.out.println("[SYSCALL] write(fd=" + fd + ", count=" + count +
                ") wrote " + bytesWritten + " bytes");
        return bytesWritten;
    }

    private long sysOpen(Task task, long pathPtr, long flags, long mode) {
        if (vfs == null) {
            System.err.println("[SYSCALL] open: VFS not initialized");
            return -ErrorCode.EINVAL;
        }

        String path = copyStringFromUser(task, pathPtr, FileSystemConstants.MAX_PATH_LENGTH);
        if (path == null) {
            System.err.println("[SYSCALL] open: invalid path pointer");
            return -ErrorCode.EFAULT;
        }

        System.out.println("[SYSCALL] open(\"" + path + "\", flags=0x" +
                Long.toHexString(flags) + ", mode=0" + Long.toOctalString(mode) +
                ") called by pid=" + task.getPid());

        Inode currentDir = resolveCurrentDir(task);
        Inode inode = vfs.resolve(path, currentDir);

        if (inode == null && (flags & File.O_CREAT) != 0) {
            inode = vfs.createFile(path, currentDir, (int) mode);
            if (inode == null) {
                System.err.println("[SYSCALL] open: failed to create file");
                return -ErrorCode.ENOENT;
            }
        }

        if (inode == null) {
            System.err.println("[SYSCALL] open: file not found: " + path);
            return -ErrorCode.ENOENT;
        }

        if ((flags & File.O_TRUNC) != 0 && inode.isRegularFile()) {
            inode.setSize(0);
            int[] directBlocks = inode.getDirectBlocks();
            if (directBlocks[0] != 0) {
                vfs.freeBlock(directBlocks[0]);
                directBlocks[0] = 0;
            }
        }

        File file = new File(inode, (int) flags);

        int fd = task.getFdTable().allocate(file);
        if (fd < 0) {
            vfs.putInode(inode);
            System.err.println("[SYSCALL] open: no free file descriptor");
            return -ErrorCode.EMFILE;
        }

        System.out.println("[SYSCALL] open() returned fd=" + fd);
        return fd;
    }

    private long sysClose(Task task, long fd, long arg2, long arg3) {
        System.out.println("[SYSCALL] close(fd=" + fd + ")");
        task.getFdTable().close((int) fd);
        return 0;
    }

    private long sysLseek(Task task, long fd, long offset, long whence) {
        File file = task.getFdTable().get((int) fd);
        if (file == null) {
            System.err.println("[SYSCALL] lseek: invalid file descriptor " + fd);
            return -ErrorCode.EBADF;
        }

        long newPos = file.lseek(offset, (int) whence);
        System.out.println("[SYSCALL] lseek(fd=" + fd + ", offset=" + offset +
                ", whence=" + whence + ") = " + newPos);
        return newPos;
    }

    private long sysCreat(Task task, long pathPtr, long mode, long arg3) {
        return sysOpen(task, pathPtr, File.O_CREAT | File.O_WRONLY | File.O_TRUNC, mode);
    }

    private long sysUnlink(Task task, long pathPtr, long arg2, long arg3) {
        if (vfs == null) {
            return -ErrorCode.EINVAL;
        }

        String path = copyStringFromUser(task, pathPtr, FileSystemConstants.MAX_PATH_LENGTH);
        if (path == null) {
            return -ErrorCode.EFAULT;
        }

        System.out.println("[SYSCALL] unlink(\"" + path + "\") called by pid=" + task.getPid());

        Inode currentDir = resolveCurrentDir(task);
        Inode inode = vfs.resolve(path, currentDir);
        if (inode == null) {
            return -ErrorCode.ENOENT;
        }

        if (inode.isDirectory()) {
            vfs.putInode(inode);
            return -ErrorCode.EISDIR;
        }

        if (vfs.unlink(path, currentDir, inode)) {
            vfs.putInode(inode);
            System.out.println("[SYSCALL] unlink() succeeded");
            return 0;
        } else {
            vfs.putInode(inode);
            return -ErrorCode.EIO;
        }
    }

    private long sysChdir(Task task, long pathPtr, long arg2, long arg3) {
        if (vfs == null) {
            return -ErrorCode.EINVAL;
        }

        String path = copyStringFromUser(task, pathPtr, FileSystemConstants.MAX_PATH_LENGTH);
        if (path == null) {
            return -ErrorCode.EFAULT;
        }

        System.out.println("[SYSCALL] chdir(\"" + path + "\") called by pid=" + task.getPid());

        Inode currentDir = resolveCurrentDir(task);
        Inode newDir = vfs.resolve(path, currentDir);
        if (newDir == null) {
            return -ErrorCode.ENOENT;
        }

        if (!newDir.isDirectory()) {
            vfs.putInode(newDir);
            return -ErrorCode.ENOTDIR;
        }

        task.setCurrentWorkingDir(newDir.getIno());
        vfs.putInode(newDir);

        System.out.println("[SYSCALL] chdir() succeeded, new cwd ino=" + newDir.getIno());
        return 0;
    }

    private long sysMkdir(Task task, long pathPtr, long mode, long arg3) {
        if (vfs == null) {
            return -ErrorCode.EINVAL;
        }

        String path = copyStringFromUser(task, pathPtr, FileSystemConstants.MAX_PATH_LENGTH);
        if (path == null) {
            return -ErrorCode.EFAULT;
        }

        System.out.println("[SYSCALL] mkdir(\"" + path + "\", mode=0" +
                Long.toOctalString(mode) + ") called by pid=" + task.getPid());

        Inode currentDir = resolveCurrentDir(task);
        Inode newDir = vfs.createDirectory(path, currentDir, (int) mode);
        if (newDir == null) {
            return -ErrorCode.EEXIST;
        }

        vfs.putInode(newDir);
        System.out.println("[SYSCALL] mkdir() succeeded");
        return 0;
    }

    private long sysRmdir(Task task, long pathPtr, long arg2, long arg3) {
        if (vfs == null) {
            return -ErrorCode.EINVAL;
        }

        String path = copyStringFromUser(task, pathPtr, FileSystemConstants.MAX_PATH_LENGTH);
        if (path == null) {
            return -ErrorCode.EFAULT;
        }

        System.out.println("[SYSCALL] rmdir(\"" + path + "\") called by pid=" + task.getPid());

        Inode currentDir = resolveCurrentDir(task);
        Inode dir = vfs.resolve(path, currentDir);
        if (dir == null) {
            return -ErrorCode.ENOENT;
        }

        if (!dir.isDirectory()) {
            vfs.putInode(dir);
            return -ErrorCode.ENOTDIR;
        }

        if (vfs.unlink(path, currentDir, dir)) {
            vfs.putInode(dir);
            System.out.println("[SYSCALL] rmdir() succeeded");
            return 0;
        } else {
            vfs.putInode(dir);
            return -ErrorCode.EIO;
        }
    }

    private long sysStat(Task task, long pathPtr, long statPtr, long arg3) {
        if (vfs == null) {
            return -ErrorCode.EINVAL;
        }

        String path = copyStringFromUser(task, pathPtr, FileSystemConstants.MAX_PATH_LENGTH);
        if (path == null) {
            return -ErrorCode.EFAULT;
        }

        System.out.println("[SYSCALL] stat(\"" + path + "\") called by pid=" + task.getPid());

        Inode currentDir = resolveCurrentDir(task);
        Inode inode = vfs.resolve(path, currentDir);
        if (inode == null) {
            return -ErrorCode.ENOENT;
        }

        Types.Stat stat = fillStat(inode);
        byte[] statBytes = stat.toBytes();
        int copied = copyToUser(task, statPtr, statBytes, 0, statBytes.length);
        if (copied < 0) {
            vfs.putInode(inode);
            return -ErrorCode.EFAULT;
        }

        vfs.putInode(inode);
        System.out.println("[SYSCALL] stat() succeeded");
        return 0;
    }

    private long sysFstat(Task task, long fd, long statPtr, long arg3) {
        File file = task.getFdTable().get((int) fd);
        if (file == null) {
            System.err.println("[SYSCALL] fstat: invalid file descriptor " + fd);
            return -ErrorCode.EBADF;
        }

        Inode inode = file.getInode();
        if (inode == null) {
            return -ErrorCode.EBADF;
        }

        System.out.println("[SYSCALL] fstat(fd=" + fd + ") called by pid=" + task.getPid());

        Types.Stat stat = fillStat(inode);
        byte[] statBytes = stat.toBytes();
        int copied = copyToUser(task, statPtr, statBytes, 0, statBytes.length);
        if (copied < 0) {
            return -ErrorCode.EFAULT;
        }

        System.out.println("[SYSCALL] fstat() succeeded");
        return 0;
    }

    private long sysDup(Task task, long oldfd, long arg2, long arg3) {
        File oldFile = task.getFdTable().get((int) oldfd);
        if (oldFile == null) {
            System.err.println("[SYSCALL] dup: invalid file descriptor " + oldfd);
            return -ErrorCode.EBADF;
        }

        int newfd = task.getFdTable().allocate(oldFile);
        if (newfd < 0) {
            System.err.println("[SYSCALL] dup: no free file descriptor");
            return -ErrorCode.EMFILE;
        }

        oldFile.incrementRef();
        System.out.println("[SYSCALL] dup(" + oldfd + ") = " + newfd);
        return newfd;
    }

    private long sysDup2(Task task, long oldfd, long newfd, long arg3) {
        File oldFile = task.getFdTable().get((int) oldfd);
        if (oldFile == null) {
            System.err.println("[SYSCALL] dup2: invalid file descriptor " + oldfd);
            return -ErrorCode.EBADF;
        }

        File existingFile = task.getFdTable().get((int) newfd);
        if (existingFile != null) {
            task.getFdTable().close((int) newfd);
        }

        if (!task.getFdTable().set((int) newfd, oldFile)) {
            System.err.println("[SYSCALL] dup2: failed to set file descriptor " + newfd);
            return -ErrorCode.EBADF;
        }

        oldFile.incrementRef();
        System.out.println("[SYSCALL] dup2(" + oldfd + ", " + newfd + ") = " + newfd);
        return newfd;
    }

    private long sysSync(Task task, long arg1, long arg2, long arg3) {
        System.out.println("[SYSCALL] sync() called by pid=" + task.getPid());
        if (vfs != null) {
            vfs.sync();
        }
        return 0;
    }

    /**
     * 填充 stat 结构（消除 sysStat/sysFstat 中的重复代码）
     */
    private Types.Stat fillStat(Inode inode) {
        Types.Stat stat = new Types.Stat();
        stat.st_dev = inode.getDev();
        stat.st_ino = inode.getIno();
        stat.st_mode = inode.getMode();
        stat.st_nlink = inode.getNlink();
        stat.st_uid = inode.getUid();
        stat.st_gid = inode.getGid();
        stat.st_rdev = 0;
        stat.st_size = inode.getSize();
        stat.st_atime = inode.getAtime();
        stat.st_mtime = inode.getMtime();
        stat.st_ctime = inode.getCtime();
        return stat;
    }
}
