package jinux.fs;

import jinux.drivers.VirtualDiskDevice;
import jinux.include.Const;
import java.util.HashMap;
import java.util.Map;

/**
 * 虚拟文件系统
 * 对应 Linux 0.01 中的 VFS 层
 * 
 * @author Jinux Project
 */
public class VirtualFileSystem {
    
    /** 超级块表（设备号 -> SuperBlock） */
    private final Map<Integer, SuperBlock> superBlocks;
    
    /** Inode 缓存（ino -> Inode） */
    private final Map<Integer, Inode> inodeCache;
    
    /** 缓冲区缓存 */
    private final Map<String, BufferCache> bufferCache;
    
    /** 系统文件表 */
    private final File[] fileTable;
    
    /** 虚拟磁盘设备 */
    private VirtualDiskDevice disk;
    
    /** 根文件系统超级块 */
    private SuperBlock rootSuperBlock;
    
    /**
     * 构造 VFS
     */
    public VirtualFileSystem() {
        this.superBlocks = new HashMap<>();
        this.inodeCache = new HashMap<>();
        this.bufferCache = new HashMap<>();
        this.fileTable = new File[Const.NR_FILE];
    }
    
    /**
     * 设置虚拟磁盘设备
     */
    public void setDisk(VirtualDiskDevice disk) {
        this.disk = disk;
    }
    
    /**
     * 初始化文件系统
     */
    public void init() {
        System.out.println("[VFS] Initializing virtual file system...");
        
        // 创建根文件系统
        int rootDev = Const.ROOT_DEV;
        rootSuperBlock = new SuperBlock(rootDev);
        rootSuperBlock.initNewFileSystem(1024, 10240); // 1024 inodes, 10240 blocks
        rootSuperBlock.setMounted(true);
        
        superBlocks.put(rootDev, rootSuperBlock);
        
        // 创建根目录 inode
        int rootIno = rootSuperBlock.allocateInode();
        if (rootIno >= 0) {
            Inode rootInode = new Inode(rootIno, rootDev);
            rootInode.setMode(Inode.S_IFDIR | 0755);
            rootInode.setNlink(2); // . 和 .. 
            rootInode.setSize(0);
            rootInode.setLoaded(true);
            
            rootSuperBlock.setRootInode(rootInode);
            inodeCache.put(rootIno, rootInode);
            
            System.out.println("[VFS] Created root directory: " + rootInode);
        }
        
        System.out.println("[VFS] Virtual file system initialized");
        System.out.println("[VFS] Root super block: " + rootSuperBlock);
    }
    
    /**
     * 获取根 inode
     */
    public Inode getRootInode() {
        if (rootSuperBlock != null) {
            return rootSuperBlock.getRootInode();
        }
        return null;
    }
    
    /**
     * 路径解析：将路径名转换为 inode
     * 对应 Linux 0.01 中的 namei() 函数
     * 
     * @param path 路径名（如 "/home/user/file.txt" 或 "file.txt"）
     * @param currentDir 当前目录 inode（用于相对路径）
     * @return 找到的 inode，失败返回 null
     */
    public Inode namei(String path, Inode currentDir) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        
        // 处理绝对路径和相对路径
        Inode dir;
        if (path.startsWith("/")) {
            // 绝对路径：从根目录开始
            dir = getRootInode();
            if (dir == null) {
                return null;
            }
            path = path.substring(1); // 去掉开头的 '/'
        } else {
            // 相对路径：从当前目录开始
            dir = currentDir != null ? currentDir : getRootInode();
            if (dir == null) {
                return null;
            }
        }
        
        // 如果路径为空，返回目录本身
        if (path.isEmpty()) {
            dir.incrementRef();
            return dir;
        }
        
        // 分割路径组件
        String[] components = path.split("/");
        
        // 遍历路径组件
        for (String component : components) {
            if (component.isEmpty()) {
                continue; // 跳过空组件（如 "//"）
            }
            
            // 查找目录项
            Inode next = lookup(dir, component);
            if (next == null) {
                // 找不到，释放当前目录引用
                putInode(dir);
                return null;
            }
            
            // 释放前一个目录的引用
            putInode(dir);
            dir = next;
            
            // 检查是否为目录（如果不是最后一个组件，必须是目录）
            if (!dir.isDirectory() && !component.equals(components[components.length - 1])) {
                putInode(dir);
                return null; // 路径错误
            }
        }
        
        return dir;
    }
    
    /**
     * 在目录中查找文件/目录
     * 对应 Linux 0.01 中的 dir_lookup() 函数
     * 
     * @param dir 目录 inode
     * @param name 文件名
     * @return 找到的 inode，失败返回 null
     */
    public Inode lookup(Inode dir, String name) {
        if (dir == null || name == null || !dir.isDirectory()) {
            return null;
        }
        
        // 读取目录内容
        byte[] dirData = readDirectory(dir);
        if (dirData == null) {
            return null;
        }
        
        // 解析目录项（简化：每个目录项 16 字节：2字节 inode号 + 14字节文件名）
        int entrySize = 16;
        int maxEntries = dirData.length / entrySize;
        
        for (int i = 0; i < maxEntries; i++) {
            int offset = i * entrySize;
            
            // 读取 inode 号（2字节，小端序）
            int ino = (dirData[offset] & 0xFF) | ((dirData[offset + 1] & 0xFF) << 8);
            
            if (ino == 0) {
                continue; // 空目录项
            }
            
            // 读取文件名（14字节，以0结尾）
            int nameLen = 0;
            while (nameLen < 14 && dirData[offset + 2 + nameLen] != 0) {
                nameLen++;
            }
            
            String entryName = new String(dirData, offset + 2, nameLen);
            
            // 比较文件名
            if (entryName.equals(name)) {
                // 找到匹配的目录项，获取对应的 inode
                return getInode(dir.getDev(), ino);
            }
        }
        
        return null; // 未找到
    }
    
    /**
     * 读取目录内容
     * 
     * @param dir 目录 inode
     * @return 目录数据（字节数组），失败返回 null
     */
    private byte[] readDirectory(Inode dir) {
        if (dir == null || !dir.isDirectory()) {
            return null;
        }
        
        // 简化实现：如果目录为空，返回空数组
        if (dir.getSize() == 0) {
            return new byte[0];
        }
        
        // 读取目录的数据块
        // 简化：只读取第一个直接块
        int[] directBlocks = dir.getDirectBlocks();
        if (directBlocks[0] == 0) {
            return new byte[0];
        }
        
        // 从缓冲区缓存读取
        BufferCache buffer = getBuffer(dir.getDev(), directBlocks[0]);
        if (buffer == null) {
            return null;
        }
        
        byte[] data = buffer.getData();
        byte[] result = new byte[Math.min(data.length, (int) dir.getSize())];
        System.arraycopy(data, 0, result, 0, result.length);
        
        releaseBuffer(buffer);
        return result;
    }
    
    /**
     * 获取或创建 inode
     */
    public Inode getInode(int dev, int ino) {
        // 从缓存查找
        Inode inode = inodeCache.get(ino);
        if (inode != null) {
            inode.incrementRef();
            return inode;
        }
        
        // 创建新 inode
        inode = new Inode(ino, dev);
        inodeCache.put(ino, inode);
        inode.incrementRef();
        
        return inode;
    }
    
    /**
     * 释放 inode
     */
    public void putInode(Inode inode) {
        if (inode == null) {
            return;
        }
        
        inode.decrementRef();
        
        // 如果引用计数为 0，从缓存中移除
        if (inode.getRefCount() == 0) {
            if (inode.isDirty()) {
                writeInodeToDisk(inode);
            }
            inodeCache.remove(inode.getIno());
        }
    }
    
    /**
     * 获取缓冲区
     */
    public BufferCache getBuffer(int dev, int blockNo) {
        String key = dev + ":" + blockNo;
        BufferCache buffer = bufferCache.get(key);
        
        if (buffer == null) {
            // 创建新缓冲区
            buffer = new BufferCache(dev, blockNo);
            bufferCache.put(key, buffer);
            
            // 从磁盘读取
            readBufferFromDisk(buffer);
        }
        
        buffer.incrementRef();
        return buffer;
    }
    
    /**
     * 释放缓冲区
     */
    public void releaseBuffer(BufferCache buffer) {
        if (buffer != null) {
            buffer.decrementRef();
            
            // 如果 dirty，写回磁盘
            if (buffer.isDirty() && buffer.getRefCount() == 0) {
                writeBufferToDisk(buffer);
            }
        }
    }
    
    /**
     * 同步所有缓冲区
     */
    public void sync() {
        System.out.println("[VFS] Syncing all buffers...");
        
        int synced = 0;
        for (BufferCache buffer : bufferCache.values()) {
            if (buffer.isDirty()) {
                writeBufferToDisk(buffer);
                buffer.setDirty(false);
                synced++;
            }
        }
        
        System.out.println("[VFS] Synced " + synced + " dirty buffers");
    }
    
    /**
     * 创建新文件
     * 
     * @param path 文件路径
     * @param parentDir 父目录 inode
     * @param mode 文件权限
     * @return 创建的 inode，失败返回 null
     */
    public Inode createFile(String path, Inode parentDir, int mode) {
        if (path == null || parentDir == null || !parentDir.isDirectory()) {
            return null;
        }
        
        // 解析文件名（最后一个组件）
        String[] components = path.split("/");
        String fileName = components[components.length - 1];
        
        if (fileName.isEmpty() || fileName.length() > 14) {
            return null; // 文件名太长（MINIX 限制为 14 字节）
        }
        
        // 检查文件是否已存在
        Inode existing = lookup(parentDir, fileName);
        if (existing != null) {
            putInode(existing);
            return null; // 文件已存在
        }
        
        // 获取超级块
        SuperBlock sb = superBlocks.get(parentDir.getDev());
        if (sb == null) {
            return null;
        }
        
        // 分配新的 inode
        int ino = sb.allocateInode();
        if (ino < 0) {
            return null;
        }
        
        // 创建 inode
        Inode inode = new Inode(ino, parentDir.getDev());
        inode.setMode(Inode.S_IFREG | (mode & 0777));
        inode.setNlink(1);
        inode.setSize(0);
        inode.setUid(0); // 简化：使用 root
        inode.setGid(0);
        inode.setLoaded(true);
        
        // 添加到缓存
        inodeCache.put(ino, inode);
        inode.incrementRef();
        
        // 在父目录中添加目录项
        if (!addDirectoryEntry(parentDir, fileName, ino)) {
            // 失败，释放 inode
            sb.freeInode(ino);
            inodeCache.remove(ino);
            putInode(inode);
            return null;
        }
        
        System.out.println("[VFS] Created file: " + fileName + " (ino=" + ino + ")");
        return inode;
    }
    
    /**
     * 在目录中添加目录项
     * 
     * @param dir 目录 inode
     * @param name 文件名
     * @param ino inode 号
     * @return 是否成功
     */
    private boolean addDirectoryEntry(Inode dir, String name, int ino) {
        if (dir == null || !dir.isDirectory() || name == null || name.length() > 14) {
            return false;
        }
        
        // 读取目录内容
        byte[] dirData = readDirectory(dir);
        if (dirData == null) {
            dirData = new byte[Const.BLOCK_SIZE];
        }
        
        // 查找空闲目录项
        int entrySize = 16;
        int maxEntries = dirData.length / entrySize;
        int freeEntry = -1;
        
        for (int i = 0; i < maxEntries; i++) {
            int offset = i * entrySize;
            int entryIno = (dirData[offset] & 0xFF) | ((dirData[offset + 1] & 0xFF) << 8);
            if (entryIno == 0) {
                freeEntry = i;
                break;
            }
        }
        
        // 如果没有空闲项，扩展目录
        if (freeEntry < 0) {
            // 分配新的数据块
            SuperBlock sb = superBlocks.get(dir.getDev());
            if (sb == null) {
                return false;
            }
            
            int newBlock = sb.allocateBlock();
            if (newBlock < 0) {
                return false;
            }
            
            // 扩展目录数据
            byte[] newDirData = new byte[dirData.length + Const.BLOCK_SIZE];
            System.arraycopy(dirData, 0, newDirData, 0, dirData.length);
            dirData = newDirData;
            freeEntry = maxEntries;
            maxEntries++;
            
            // 更新 inode 的数据块指针
            int[] directBlocks = dir.getDirectBlocks();
            for (int i = 0; i < directBlocks.length; i++) {
                if (directBlocks[i] == 0) {
                    directBlocks[i] = newBlock;
                    break;
                }
            }
        }
        
        // 写入目录项
        int offset = freeEntry * entrySize;
        dirData[offset] = (byte) (ino & 0xFF);
        dirData[offset + 1] = (byte) ((ino >> 8) & 0xFF);
        
        byte[] nameBytes = name.getBytes();
        int nameLen = Math.min(nameBytes.length, 14);
        System.arraycopy(nameBytes, 0, dirData, offset + 2, nameLen);
        // 剩余字节填充为 0
        for (int i = nameLen; i < 14; i++) {
            dirData[offset + 2 + i] = 0;
        }
        
        // 写回目录数据
        writeDirectory(dir, dirData);
        
        // 更新目录大小和链接数
        dir.setSize(dirData.length);
        dir.setNlink(dir.getNlink() + 1);
        dir.markDirty();
        
        return true;
    }
    
    /**
     * 写入目录内容
     * 
     * @param dir 目录 inode
     * @param data 目录数据
     */
    private void writeDirectory(Inode dir, byte[] data) {
        if (dir == null || !dir.isDirectory() || data == null) {
            return;
        }
        
        // 简化：只写入第一个直接块
        int[] directBlocks = dir.getDirectBlocks();
        if (directBlocks[0] == 0) {
            // 分配新块
            SuperBlock sb = superBlocks.get(dir.getDev());
            if (sb == null) {
                return;
            }
            directBlocks[0] = sb.allocateBlock();
            if (directBlocks[0] < 0) {
                return;
            }
        }
        
        // 获取缓冲区并写入
        BufferCache buffer = getBuffer(dir.getDev(), directBlocks[0]);
        if (buffer != null) {
            byte[] bufData = buffer.getData();
            int len = Math.min(data.length, bufData.length);
            System.arraycopy(data, 0, bufData, 0, len);
            buffer.markDirty();
            buffer.setDirty(true);
            releaseBuffer(buffer);
        }
    }
    
    /**
     * 释放数据块
     * 
     * @param blockNo 块号
     */
    public void freeBlock(int blockNo) {
        if (rootSuperBlock != null) {
            rootSuperBlock.freeBlock(blockNo);
        }
    }
    
    /**
     * 分配数据块
     * 
     * @return 块号，失败返回 -1
     */
    public int allocateBlock() {
        if (rootSuperBlock != null) {
            return rootSuperBlock.allocateBlock();
        }
        return -1;
    }
    
    /**
     * 读取文件数据
     * 
     * @param inode 文件 inode
     * @param position 文件位置
     * @param buf 目标缓冲区
     * @param offset 缓冲区偏移
     * @param count 要读取的字节数
     * @return 实际读取的字节数，失败返回 -1
     */
    public int readFileData(Inode inode, long position, byte[] buf, int offset, int count) {
        if (inode == null || buf == null || count <= 0) {
            return -1;
        }
        
        // 检查是否超出文件大小
        if (position >= inode.getSize()) {
            return 0; // EOF
        }
        
        // 计算实际可读取的字节数
        long remaining = inode.getSize() - position;
        int toRead = (int) Math.min(count, remaining);
        if (toRead > buf.length - offset) {
            toRead = buf.length - offset;
        }
        
        if (toRead <= 0) {
            return 0;
        }
        
        // 计算起始块号和块内偏移
        int blockSize = Const.BLOCK_SIZE;
        int startBlock = (int) (position / blockSize);
        int blockOffset = (int) (position % blockSize);
        
        int[] directBlocks = inode.getDirectBlocks();
        int bytesRead = 0;
        int currentBlock = startBlock;
        
        while (bytesRead < toRead && currentBlock < directBlocks.length) {
            int blockNo = directBlocks[currentBlock];
            if (blockNo == 0) {
                break; // 没有更多数据块
            }
            
            // 读取数据块
            BufferCache buffer = getBuffer(inode.getDev(), blockNo);
            if (buffer == null) {
                break;
            }
            
            byte[] blockData = buffer.getData();
            int bytesFromBlock = Math.min(toRead - bytesRead, blockSize - blockOffset);
            System.arraycopy(blockData, blockOffset, buf, offset + bytesRead, bytesFromBlock);
            
            releaseBuffer(buffer);
            
            bytesRead += bytesFromBlock;
            blockOffset = 0; // 后续块从偏移 0 开始
            currentBlock++;
        }
        
        return bytesRead;
    }
    
    /**
     * 写入文件数据
     * 
     * @param inode 文件 inode
     * @param position 文件位置
     * @param buf 源缓冲区
     * @param offset 缓冲区偏移
     * @param count 要写入的字节数
     * @return 实际写入的字节数，失败返回 -1
     */
    public int writeFileData(Inode inode, long position, byte[] buf, int offset, int count) {
        if (inode == null || buf == null || count <= 0) {
            return -1;
        }
        
        int toWrite = Math.min(count, buf.length - offset);
        if (toWrite <= 0) {
            return 0;
        }
        
        // 计算起始块号和块内偏移
        int blockSize = Const.BLOCK_SIZE;
        int startBlock = (int) (position / blockSize);
        int blockOffset = (int) (position % blockSize);
        
        int[] directBlocks = inode.getDirectBlocks();
        int bytesWritten = 0;
        int currentBlock = startBlock;
        
        // 确保有足够的数据块
        while (bytesWritten < toWrite) {
            // 如果当前块不存在，分配新块
            if (currentBlock >= directBlocks.length) {
                // 简化：只支持直接块，不支持间接块
                break;
            }
            
            if (directBlocks[currentBlock] == 0) {
                // 分配新数据块
                int newBlock = allocateBlock();
                if (newBlock < 0) {
                    break; // 无法分配更多块
                }
                directBlocks[currentBlock] = newBlock;
                inode.markDirty();
            }
            
            // 获取或创建缓冲区
            BufferCache buffer = getBuffer(inode.getDev(), directBlocks[currentBlock]);
            if (buffer == null) {
                break;
            }
            
            byte[] blockData = buffer.getData();
            int bytesToBlock = Math.min(toWrite - bytesWritten, blockSize - blockOffset);
            System.arraycopy(buf, offset + bytesWritten, blockData, blockOffset, bytesToBlock);
            
            buffer.markDirty();
            buffer.setDirty(true);
            releaseBuffer(buffer);
            
            bytesWritten += bytesToBlock;
            blockOffset = 0; // 后续块从偏移 0 开始
            currentBlock++;
        }
        
        // 更新文件大小
        long newSize = position + bytesWritten;
        if (newSize > inode.getSize()) {
            inode.setSize(newSize);
        }
        
        // 更新修改时间
        inode.setMtime(System.currentTimeMillis() / 1000);
        inode.markDirty();
        
        return bytesWritten;
    }
    
    /**
     * 创建目录
     * 
     * @param path 目录路径
     * @param parentDir 父目录 inode
     * @param mode 目录权限
     * @return 创建的 inode，失败返回 null
     */
    public Inode createDirectory(String path, Inode parentDir, int mode) {
        if (path == null || parentDir == null || !parentDir.isDirectory()) {
            return null;
        }
        
        // 解析目录名（最后一个组件）
        String[] components = path.split("/");
        String dirName = components[components.length - 1];
        
        if (dirName.isEmpty() || dirName.length() > 14) {
            return null; // 目录名太长
        }
        
        // 检查目录是否已存在
        Inode existing = lookup(parentDir, dirName);
        if (existing != null) {
            putInode(existing);
            return null; // 目录已存在
        }
        
        // 获取超级块
        SuperBlock sb = superBlocks.get(parentDir.getDev());
        if (sb == null) {
            return null;
        }
        
        // 分配新的 inode
        int ino = sb.allocateInode();
        if (ino < 0) {
            return null;
        }
        
        // 创建目录 inode
        Inode dir = new Inode(ino, parentDir.getDev());
        dir.setMode(Inode.S_IFDIR | (mode & 0777));
        dir.setNlink(2); // . 和 ..
        dir.setSize(0);
        dir.setUid(0);
        dir.setGid(0);
        dir.setLoaded(true);
        
        // 添加到缓存
        inodeCache.put(ino, dir);
        dir.incrementRef();
        
        // 在父目录中添加目录项
        if (!addDirectoryEntry(parentDir, dirName, ino)) {
            sb.freeInode(ino);
            inodeCache.remove(ino);
            putInode(dir);
            return null;
        }
        
        // 创建目录的初始内容：. 和 ..
        byte[] dirData = new byte[Const.BLOCK_SIZE];
        int offset = 0;
        
        // . 目录项
        dirData[offset++] = (byte) (ino & 0xFF);
        dirData[offset++] = (byte) ((ino >> 8) & 0xFF);
        dirData[offset++] = '.';
        for (int i = 1; i < 14; i++) {
            dirData[offset++] = 0;
        }
        
        // .. 目录项
        dirData[offset++] = (byte) (parentDir.getIno() & 0xFF);
        dirData[offset++] = (byte) ((parentDir.getIno() >> 8) & 0xFF);
        dirData[offset++] = '.';
        dirData[offset++] = '.';
        for (int i = 2; i < 14; i++) {
            dirData[offset++] = 0;
        }
        
        // 分配数据块并写入
        int blockNo = sb.allocateBlock();
        if (blockNo < 0) {
            sb.freeInode(ino);
            inodeCache.remove(ino);
            putInode(dir);
            return null;
        }
        
        dir.getDirectBlocks()[0] = blockNo;
        writeDirectory(dir, dirData);
        dir.setSize(32); // . 和 .. 两个目录项
        
        System.out.println("[VFS] Created directory: " + dirName + " (ino=" + ino + ")");
        return dir;
    }
    
    /**
     * 删除文件或目录
     * 
     * @param path 路径
     * @param parentDir 父目录
     * @param inode 要删除的 inode
     * @return 是否成功
     */
    public boolean unlink(String path, Inode parentDir, Inode inode) {
        if (path == null || parentDir == null || inode == null) {
            return false;
        }
        
        // 解析文件名
        String[] components = path.split("/");
        String name = components[components.length - 1];
        
        // 从父目录中删除目录项
        if (!removeDirectoryEntry(parentDir, name)) {
            return false;
        }
        
        // 减少链接数
        int newNlink = inode.getNlink() - 1;
        inode.setNlink(newNlink);
        
        // 如果链接数为 0，释放 inode 和数据块
        if (newNlink == 0) {
            // 释放数据块
            int[] directBlocks = inode.getDirectBlocks();
            for (int block : directBlocks) {
                if (block != 0) {
                    freeBlock(block);
                }
            }
            
            // 释放 inode
            SuperBlock sb = superBlocks.get(inode.getDev());
            if (sb != null) {
                sb.freeInode(inode.getIno());
            }
            
            // 从缓存中移除
            inodeCache.remove(inode.getIno());
        }
        
        System.out.println("[VFS] Unlinked: " + name);
        return true;
    }
    
    /**
     * 从目录中删除目录项
     * 
     * @param dir 目录 inode
     * @param name 文件名
     * @return 是否成功
     */
    private boolean removeDirectoryEntry(Inode dir, String name) {
        if (dir == null || !dir.isDirectory() || name == null) {
            return false;
        }
        
        // 读取目录内容
        byte[] dirData = readDirectory(dir);
        if (dirData == null) {
            return false;
        }
        
        // 查找目录项
        int entrySize = 16;
        int maxEntries = dirData.length / entrySize;
        
        for (int i = 0; i < maxEntries; i++) {
            int offset = i * entrySize;
            int ino = (dirData[offset] & 0xFF) | ((dirData[offset + 1] & 0xFF) << 8);
            
            if (ino == 0) {
                continue;
            }
            
            // 读取文件名
            int nameLen = 0;
            while (nameLen < 14 && dirData[offset + 2 + nameLen] != 0) {
                nameLen++;
            }
            
            String entryName = new String(dirData, offset + 2, nameLen);
            
            // 找到匹配的目录项
            if (entryName.equals(name)) {
                // 清空目录项
                dirData[offset] = 0;
                dirData[offset + 1] = 0;
                for (int j = 2; j < entrySize; j++) {
                    dirData[offset + j] = 0;
                }
                
                // 写回目录数据
                writeDirectory(dir, dirData);
                
                // 更新目录大小和链接数
                dir.setNlink(dir.getNlink() - 1);
                dir.markDirty();
                
                return true;
            }
        }
        
        return false; // 未找到
    }
    
    /**
     * 打印统计信息
     */
    public void printStats() {
        System.out.println("\n[VFS] File System Statistics:");
        System.out.println("  Mounted super blocks: " + superBlocks.size());
        System.out.println("  Cached inodes: " + inodeCache.size());
        System.out.println("  Buffer cache entries: " + bufferCache.size());
        
        int openFiles = 0;
        for (File file : fileTable) {
            if (file != null) {
                openFiles++;
            }
        }
        System.out.println("  Open files: " + openFiles + "/" + fileTable.length);
        
        if (rootSuperBlock != null) {
            System.out.println("  Root filesystem: " + rootSuperBlock);
        }
    }
    
    // ==================== Getters ====================
    
    public SuperBlock getRootSuperBlock() {
        return rootSuperBlock;
    }
    
    public File[] getFileTable() {
        return fileTable;
    }
    
    // ==================== 磁盘 I/O 操作 ====================
    
    /**
     * 从磁盘读取缓冲区
     * 
     * @param buffer 缓冲区
     */
    private void readBufferFromDisk(BufferCache buffer) {
        if (disk == null) {
            // 没有磁盘设备，使用空数据
            buffer.markValid();
            return;
        }
        
        try {
            byte[] data = buffer.getData();
            int result = disk.readBlock(buffer.getBlockNo(), data, 0);
            
            if (result > 0) {
                buffer.markValid();
            } else {
                System.err.println("[VFS] Failed to read block " + buffer.getBlockNo() + 
                    " from disk");
            }
        } catch (Exception e) {
            System.err.println("[VFS] Exception reading block " + buffer.getBlockNo() + 
                ": " + e.getMessage());
        }
    }
    
    /**
     * 将缓冲区写回磁盘
     * 
     * @param buffer 缓冲区
     */
    private void writeBufferToDisk(BufferCache buffer) {
        if (disk == null) {
            System.err.println("[VFS] No disk device for writing block " + buffer.getBlockNo());
            return;
        }
        
        if (!buffer.isValid()) {
            // 无效的缓冲区不需要写回
            return;
        }
        
        try {
            byte[] data = buffer.getData();
            int result = disk.writeBlock(buffer.getBlockNo(), data, 0);
            
            if (result > 0) {
                buffer.setDirty(false);
            } else {
                System.err.println("[VFS] Failed to write block " + buffer.getBlockNo() + 
                    " to disk");
            }
        } catch (Exception e) {
            System.err.println("[VFS] Exception writing block " + buffer.getBlockNo() + 
                ": " + e.getMessage());
        }
    }
    
    /**
     * 将 inode 写回磁盘
     * 
     * @param inode inode 对象
     */
    private void writeInodeToDisk(Inode inode) {
        if (disk == null) {
            System.err.println("[VFS] No disk device for writing inode " + inode.getIno());
            return;
        }
        
        // 获取超级块
        SuperBlock sb = superBlocks.get(inode.getDev());
        if (sb == null) {
            System.err.println("[VFS] No super block for device " + inode.getDev());
            return;
        }
        
        // 计算 inode 在磁盘上的位置
        // 布局：超级块(1) | inode位图 | zone位图 | inode表 | 数据区
        // inode 表从块号 2 + imapBlocks + zmapBlocks 开始
        int inodeTableStart = 2 + sb.getImapBlocks() + sb.getZmapBlocks();
        int inodeSize = 32; // Linux 0.01 inode 大小为 32 字节
        int inodesPerBlock = Const.BLOCK_SIZE / inodeSize;
        int inodeIndex = inode.getIno() - 1; // inode 编号从 1 开始
        
        int blockNo = inodeTableStart + (inodeIndex / inodesPerBlock);
        int offsetInBlock = (inodeIndex % inodesPerBlock) * inodeSize;
        
        try {
            // 读取包含该 inode 的块
            BufferCache buffer = getBuffer(inode.getDev(), blockNo);
            byte[] blockData = buffer.getData();
            
            // 将 inode 数据序列化到缓冲区
            serializeInode(inode, blockData, offsetInBlock);
            
            // 标记缓冲区为脏
            buffer.markDirty();
            buffer.setDirty(true);
            
            // 写回缓冲区
            writeBufferToDisk(buffer);
            releaseBuffer(buffer);
            
            inode.setDirty(false);
            
        } catch (Exception e) {
            System.err.println("[VFS] Exception writing inode " + inode.getIno() + 
                ": " + e.getMessage());
        }
    }
    
    /**
     * 序列化 inode 到字节数组
     * 简化实现：只序列化关键字段
     * 
     * @param inode inode 对象
     * @param buf 目标缓冲区
     * @param offset 偏移量
     */
    private void serializeInode(Inode inode, byte[] buf, int offset) {
        // 简化序列化：只写入关键字段
        // 实际 Linux 0.01 的 inode 结构更复杂
        int pos = offset;
        
        // mode (2 bytes)
        buf[pos++] = (byte) (inode.getMode() & 0xFF);
        buf[pos++] = (byte) ((inode.getMode() >> 8) & 0xFF);
        
        // uid (2 bytes)
        buf[pos++] = (byte) (inode.getUid() & 0xFF);
        buf[pos++] = (byte) ((inode.getUid() >> 8) & 0xFF);
        
        // size (4 bytes)
        long size = inode.getSize();
        buf[pos++] = (byte) (size & 0xFF);
        buf[pos++] = (byte) ((size >> 8) & 0xFF);
        buf[pos++] = (byte) ((size >> 16) & 0xFF);
        buf[pos++] = (byte) ((size >> 24) & 0xFF);
        
        // mtime (4 bytes)
        long mtime = inode.getMtime();
        buf[pos++] = (byte) (mtime & 0xFF);
        buf[pos++] = (byte) ((mtime >> 8) & 0xFF);
        buf[pos++] = (byte) ((mtime >> 16) & 0xFF);
        buf[pos++] = (byte) ((mtime >> 24) & 0xFF);
        
        // 直接块指针 (10 * 2 = 20 bytes)
        int[] directBlocks = inode.getDirectBlocks();
        for (int i = 0; i < directBlocks.length && i < 10; i++) {
            int block = directBlocks[i];
            buf[pos++] = (byte) (block & 0xFF);
            buf[pos++] = (byte) ((block >> 8) & 0xFF);
        }
        
        // 剩余字节填充为 0
        while (pos < offset + 32) {
            buf[pos++] = 0;
        }
    }
}
