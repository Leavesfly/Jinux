package jinux.fs;

/**
 * 路径解析器接口
 * 负责将文件路径字符串解析为对应的 Inode
 */
public interface PathResolver {
    /**
     * 解析路径为对应的 Inode
     * 
     * @param path 文件路径字符串
     * @param currentDir 当前目录的 Inode
     * @return 解析得到的 Inode，如果解析失败则返回 null
     */
    Inode resolve(String path, Inode currentDir);

    /**
     * 在指定目录下查找指定名称的文件或子目录
     * 
     * @param dir 目录 Inode
     * @param name 文件或目录名称
     * @return 查找到的 Inode，如果未找到则返回 null
     */
    Inode lookup(Inode dir, String name);
}
