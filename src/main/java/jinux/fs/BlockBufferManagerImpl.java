package jinux.fs;

import jinux.drivers.VirtualDiskDevice;

import java.util.Map;

/**
 * 块缓冲区管理器实现类
 * 负责块设备缓冲区的获取、释放和同步
 */
public class BlockBufferManagerImpl implements BlockBufferManager {
    private final Map<String, BufferCache> bufferCache;
    private VirtualDiskDevice disk;
    
    public BlockBufferManagerImpl(Map<String, BufferCache> bufferCache) {
        this.bufferCache = bufferCache;
    }
    
    public void setDisk(VirtualDiskDevice disk) {
        this.disk = disk;
    }
    
    @Override
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
    
    @Override
    public void releaseBuffer(BufferCache buffer) {
        if (buffer != null) {
            buffer.decrementRef();
            
            // 如果 dirty，写回磁盘
            if (buffer.isDirty() && buffer.getRefCount() == 0) {
                writeBufferToDisk(buffer);
            }
        }
    }
    
    @Override
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
}
