package jinux.drivers;

import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 虚拟磁盘设备
 * 对应 Linux 0.01 中的硬盘驱动 (drivers/blk_drv/hd.c)
 * 
 * 使用文件模拟磁盘镜像
 * 
 * @author Jinux Project
 */
public class VirtualDiskDevice extends BlockDevice {
    
    /** 磁盘镜像文件路径 */
    private final String imagePath;
    
    /** 随机访问文件 */
    private RandomAccessFile file;
    
    /** 磁盘总块数 */
    private final int totalBlocks;
    
    /**
     * 构造虚拟磁盘设备
     * 
     * @param imagePath 磁盘镜像文件路径
     * @param sizeInMB 磁盘大小（MB）
     */
    public VirtualDiskDevice(String imagePath, int sizeInMB) {
        super("vdisk", 3, 0); // 主设备号 3（IDE 硬盘）
        this.imagePath = imagePath;
        this.totalBlocks = (sizeInMB * 1024 * 1024) / blockSize;
    }
    
    @Override
    public void init() {
        try {
            Path path = Paths.get(imagePath);
            
            // 如果镜像文件不存在，创建它
            if (!Files.exists(path)) {
                System.out.println("[DISK] Creating disk image: " + imagePath);
                file = new RandomAccessFile(imagePath, "rw");
                
                // 预分配空间
                long size = (long) totalBlocks * blockSize;
                file.setLength(size);
                
                System.out.println("[DISK] Disk image created: " + (size / 1024 / 1024) + "MB, " +
                    totalBlocks + " blocks");
            } else {
                file = new RandomAccessFile(imagePath, "rw");
                System.out.println("[DISK] Disk image loaded: " + imagePath);
            }
            
            System.out.println("[DISK] Virtual disk initialized: " + this);
            
        } catch (Exception e) {
            System.err.println("[DISK] ERROR: Failed to initialize disk: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @Override
    public int readBlock(int blockNo, byte[] buf, int offset) {
        if (blockNo < 0 || blockNo >= totalBlocks) {
            System.err.println("[DISK] ERROR: Invalid block number: " + blockNo);
            return -1;
        }
        
        try {
            long pos = (long) blockNo * blockSize;
            file.seek(pos);
            return file.read(buf, offset, blockSize);
            
        } catch (Exception e) {
            System.err.println("[DISK] ERROR: Read block " + blockNo + " failed: " + e.getMessage());
            return -1;
        }
    }
    
    @Override
    public int writeBlock(int blockNo, byte[] buf, int offset) {
        if (blockNo < 0 || blockNo >= totalBlocks) {
            System.err.println("[DISK] ERROR: Invalid block number: " + blockNo);
            return -1;
        }
        
        try {
            long pos = (long) blockNo * blockSize;
            file.seek(pos);
            file.write(buf, offset, blockSize);
            return blockSize;
            
        } catch (Exception e) {
            System.err.println("[DISK] ERROR: Write block " + blockNo + " failed: " + e.getMessage());
            return -1;
        }
    }
    
    @Override
    public void sync() {
        try {
            if (file != null) {
                file.getFD().sync();
            }
        } catch (Exception e) {
            System.err.println("[DISK] ERROR: Sync failed: " + e.getMessage());
        }
    }
    
    /**
     * 关闭磁盘设备
     */
    public void close() {
        try {
            if (file != null) {
                sync();
                file.close();
                System.out.println("[DISK] Virtual disk closed");
            }
        } catch (Exception e) {
            System.err.println("[DISK] ERROR: Close failed: " + e.getMessage());
        }
    }
    
    /**
     * 获取总块数
     */
    public int getTotalBlocks() {
        return totalBlocks;
    }
}
