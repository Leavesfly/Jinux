package jinux.fs;

import jinux.include.Const;

/**
 * 缓冲区缓存
 * 对应 Linux 0.01 中的 struct buffer_head
 * 
 * 用于缓存磁盘块数据
 * 
 * @author Jinux Project
 */
public class BufferCache {
    
    /** 设备号 */
    private int dev;
    
    /** 块号 */
    private int blockNo;
    
    /** 缓冲区数据 */
    private byte[] data;
    
    /** 是否有效（已从磁盘读取） */
    private boolean valid;
    
    /** 是否被修改（需要写回磁盘） */
    private boolean dirty;
    
    /** 引用计数 */
    private int refCount;
    
    /** 最后访问时间（用于 LRU） */
    private long lastAccess;
    
    /**
     * 构造缓冲区
     */
    public BufferCache(int dev, int blockNo) {
        this.dev = dev;
        this.blockNo = blockNo;
        this.data = new byte[Const.BLOCK_SIZE];
        this.valid = false;
        this.dirty = false;
        this.refCount = 0;
        this.lastAccess = System.currentTimeMillis();
    }
    
    /**
     * 增加引用计数
     */
    public synchronized void incrementRef() {
        refCount++;
        lastAccess = System.currentTimeMillis();
    }
    
    /**
     * 减少引用计数
     */
    public synchronized void decrementRef() {
        if (refCount > 0) {
            refCount--;
        }
    }
    
    /**
     * 标记为已修改
     */
    public void markDirty() {
        this.dirty = true;
    }
    
    /**
     * 标记为有效
     */
    public void markValid() {
        this.valid = true;
        this.lastAccess = System.currentTimeMillis();
    }
    
    /**
     * 清空缓冲区
     */
    public void clear() {
        this.valid = false;
        this.dirty = false;
        this.refCount = 0;
        for (int i = 0; i < data.length; i++) {
            data[i] = 0;
        }
    }
    
    @Override
    public String toString() {
        return String.format("Buffer[dev=%d, block=%d, valid=%b, dirty=%b, ref=%d]",
            dev, blockNo, valid, dirty, refCount);
    }
    
    // ==================== Getters and Setters ====================
    
    public int getDev() {
        return dev;
    }
    
    public int getBlockNo() {
        return blockNo;
    }
    
    public byte[] getData() {
        return data;
    }
    
    public void setData(byte[] data) {
        if (data.length == Const.BLOCK_SIZE) {
            this.data = data;
            this.valid = true;
            this.dirty = true;
        }
    }
    
    public boolean isValid() {
        return valid;
    }
    
    public void setValid(boolean valid) {
        this.valid = valid;
    }
    
    public boolean isDirty() {
        return dirty;
    }
    
    public void setDirty(boolean dirty) {
        this.dirty = dirty;
    }
    
    public int getRefCount() {
        return refCount;
    }
    
    public long getLastAccess() {
        return lastAccess;
    }
}
