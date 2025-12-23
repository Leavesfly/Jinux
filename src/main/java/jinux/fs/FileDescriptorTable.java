package jinux.fs;

import jinux.include.Const;

/**
 * 文件描述符表
 * 对应 Linux 0.01 中每个进程的文件描述符数组
 * 
 * @author Jinux Project
 */
public class FileDescriptorTable {
    
    /** 文件描述符数组 */
    private final File[] files;
    
    /**
     * 构造文件描述符表
     */
    public FileDescriptorTable() {
        this.files = new File[Const.NR_OPEN];
    }
    
    /**
     * 分配文件描述符
     * 
     * @param file 文件对象
     * @return 文件描述符号，失败返回 -1
     */
    public synchronized int allocate(File file) {
        for (int i = 0; i < files.length; i++) {
            if (files[i] == null) {
                files[i] = file;
                return i;
            }
        }
        return -1; // 文件描述符用尽
    }
    
    /**
     * 获取文件对象
     * 
     * @param fd 文件描述符
     * @return 文件对象，不存在返回 null
     */
    public File get(int fd) {
        if (fd < 0 || fd >= files.length) {
            return null;
        }
        return files[fd];
    }
    
    /**
     * 关闭文件描述符
     * 
     * @param fd 文件描述符
     */
    public synchronized void close(int fd) {
        if (fd >= 0 && fd < files.length) {
            File file = files[fd];
            if (file != null) {
                file.decrementRef();
            }
            files[fd] = null;
        }
    }
    
    /**
     * 设置文件描述符（用于 dup2）
     * 
     * @param fd 文件描述符
     * @param file 文件对象
     * @return 是否成功
     */
    public synchronized boolean set(int fd, File file) {
        if (fd < 0 || fd >= files.length) {
            return false;
        }
        
        // 如果该位置已有文件，先关闭
        if (files[fd] != null) {
            files[fd].decrementRef();
        }
        
        files[fd] = file;
        return true;
    }
    
    /**
     * 关闭所有文件描述符
     */
    public synchronized void closeAll() {
        for (int i = 0; i < files.length; i++) {
            if (files[i] != null) {
                files[i] = null;
            }
        }
    }
    
    /**
     * 复制文件描述符表（用于 fork）
     * 
     * @return 新的文件描述符表
     */
    public FileDescriptorTable copy() {
        FileDescriptorTable newTable = new FileDescriptorTable();
        System.arraycopy(this.files, 0, newTable.files, 0, files.length);
        return newTable;
    }
}
