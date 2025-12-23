package jinux.drivers;

/**
 * 设备抽象基类
 * 对应 Linux 0.01 中的设备驱动接口
 * 
 * @author Jinux Project
 */
public abstract class Device {
    
    /** 设备名称 */
    protected final String name;
    
    /** 主设备号 */
    protected final int major;
    
    /** 次设备号 */
    protected final int minor;
    
    /**
     * 构造设备
     * 
     * @param name 设备名称
     * @param major 主设备号
     * @param minor 次设备号
     */
    public Device(String name, int major, int minor) {
        this.name = name;
        this.major = major;
        this.minor = minor;
    }
    
    /**
     * 设备初始化
     */
    public abstract void init();
    
    /**
     * 获取设备名称
     */
    public String getName() {
        return name;
    }
    
    /**
     * 获取主设备号
     */
    public int getMajor() {
        return major;
    }
    
    /**
     * 获取次设备号
     */
    public int getMinor() {
        return minor;
    }
    
    @Override
    public String toString() {
        return String.format("Device[%s, %d:%d]", name, major, minor);
    }
}
