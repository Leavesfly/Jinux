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
}
