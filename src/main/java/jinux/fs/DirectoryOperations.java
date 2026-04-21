package jinux.fs;

/**
 * 目录操作接口
 * 负责目录的创建、查找和删除
 */
public interface DirectoryOperations {
    /**
     * 创建新目录
     * 
     * @param path 目录路径
     * @param parentDir 父目录 Inode
     * @param mode 目录权限模式
     * @return 创建的目录 Inode，如果创建失败则返回 null
     */
    Inode createDirectory(String path, Inode parentDir, int mode);

    /**
     * 从目录中删除指定的目录项
     * 
     * @param dir 目录 Inode
     * @param name 要删除的文件或目录名称
     * @return 是否成功删除
     */
    boolean removeDirectoryEntry(Inode dir, String name);
}
