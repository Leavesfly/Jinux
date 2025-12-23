package jinux.include;

/**
 * Jinux 操作系统类型定义
 * 对应 Linux 0.01 中的基本类型定义
 * 
 * @author Jinux Project
 */
public class Types {
    
    /**
     * 设备号类型
     * Linux 中：主设备号(8位) + 次设备号(8位)
     */
    public static class DeviceId {
        private final int major;
        private final int minor;
        
        public DeviceId(int dev) {
            this.major = (dev >> 8) & 0xFF;
            this.minor = dev & 0xFF;
        }
        
        public DeviceId(int major, int minor) {
            this.major = major & 0xFF;
            this.minor = minor & 0xFF;
        }
        
        public int getMajor() {
            return major;
        }
        
        public int getMinor() {
            return minor;
        }
        
        public int toInt() {
            return (major << 8) | minor;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DeviceId)) return false;
            DeviceId other = (DeviceId) obj;
            return this.major == other.major && this.minor == other.minor;
        }
        
        @Override
        public int hashCode() {
            return toInt();
        }
        
        @Override
        public String toString() {
            return String.format("DeviceId(%d,%d)", major, minor);
        }
    }
    
    /**
     * inode 号类型
     */
    public static class InodeNum {
        private final int value;
        
        public InodeNum(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof InodeNum)) return false;
            return this.value == ((InodeNum) obj).value;
        }
        
        @Override
        public int hashCode() {
            return value;
        }
        
        @Override
        public String toString() {
            return "Inode#" + value;
        }
    }
    
    /**
     * 时间类型（Unix 时间戳，秒）
     */
    public static class TimeT {
        private final long seconds;
        
        public TimeT() {
            this.seconds = System.currentTimeMillis() / 1000;
        }
        
        public TimeT(long seconds) {
            this.seconds = seconds;
        }
        
        public long getSeconds() {
            return seconds;
        }
        
        @Override
        public String toString() {
            return "Time(" + seconds + "s)";
        }
    }
    
    /**
     * 文件状态结构体
     * 对应 Linux 0.01 中的 struct stat
     */
    public static class Stat {
        /** 设备号 */
        public int st_dev;
        
        /** inode 号 */
        public int st_ino;
        
        /** 文件类型和权限 */
        public int st_mode;
        
        /** 硬链接数 */
        public int st_nlink;
        
        /** 用户 ID */
        public int st_uid;
        
        /** 组 ID */
        public int st_gid;
        
        /** 设备号（如果是设备文件） */
        public int st_rdev;
        
        /** 文件大小 */
        public long st_size;
        
        /** 访问时间 */
        public long st_atime;
        
        /** 修改时间 */
        public long st_mtime;
        
        /** 状态改变时间 */
        public long st_ctime;
        
        /**
         * 将 stat 结构序列化为字节数组（用于写入用户空间）
         * Linux 0.01 stat 结构大小约 64 字节
         */
        public byte[] toBytes() {
            byte[] buf = new byte[64];
            int offset = 0;
            
            // st_dev (4 bytes)
            writeInt(buf, offset, st_dev);
            offset += 4;
            
            // st_ino (2 bytes)
            writeShort(buf, offset, (short) st_ino);
            offset += 2;
            
            // st_mode (2 bytes)
            writeShort(buf, offset, (short) st_mode);
            offset += 2;
            
            // st_nlink (2 bytes)
            writeShort(buf, offset, (short) st_nlink);
            offset += 2;
            
            // st_uid (2 bytes)
            writeShort(buf, offset, (short) st_uid);
            offset += 2;
            
            // st_gid (2 bytes)
            writeShort(buf, offset, (short) st_gid);
            offset += 2;
            
            // st_rdev (4 bytes)
            writeInt(buf, offset, st_rdev);
            offset += 4;
            
            // st_size (4 bytes)
            writeInt(buf, offset, (int) st_size);
            offset += 4;
            
            // st_atime (4 bytes)
            writeInt(buf, offset, (int) st_atime);
            offset += 4;
            
            // st_mtime (4 bytes)
            writeInt(buf, offset, (int) st_mtime);
            offset += 4;
            
            // st_ctime (4 bytes)
            writeInt(buf, offset, (int) st_ctime);
            offset += 4;
            
            // 剩余字节填充为 0
            while (offset < buf.length) {
                buf[offset++] = 0;
            }
            
            return buf;
        }
        
        private void writeInt(byte[] buf, int offset, int value) {
            buf[offset] = (byte) (value & 0xFF);
            buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
            buf[offset + 2] = (byte) ((value >> 16) & 0xFF);
            buf[offset + 3] = (byte) ((value >> 24) & 0xFF);
        }
        
        private void writeShort(byte[] buf, int offset, short value) {
            buf[offset] = (byte) (value & 0xFF);
            buf[offset + 1] = (byte) ((value >> 8) & 0xFF);
        }
    }
    
    /**
     * 进程时间结构体
     * 对应 Linux 0.01 中的 struct tms
     */
    public static class Tms {
        /** 用户态 CPU 时间（时钟滴答数） */
        public long tms_utime;
        
        /** 内核态 CPU 时间（时钟滴答数） */
        public long tms_stime;
        
        /** 子进程用户态 CPU 时间 */
        public long tms_cutime;
        
        /** 子进程内核态 CPU 时间 */
        public long tms_cstime;
        
        /**
         * 将 tms 结构序列化为字节数组
         */
        public byte[] toBytes() {
            byte[] buf = new byte[32]; // 4个 long，每个8字节
            int offset = 0;
            
            writeLong(buf, offset, tms_utime);
            offset += 8;
            writeLong(buf, offset, tms_stime);
            offset += 8;
            writeLong(buf, offset, tms_cutime);
            offset += 8;
            writeLong(buf, offset, tms_cstime);
            
            return buf;
        }
        
        private void writeLong(byte[] buf, int offset, long value) {
            for (int i = 0; i < 8; i++) {
                buf[offset + i] = (byte) (value & 0xFF);
                value >>= 8;
            }
        }
    }
}
