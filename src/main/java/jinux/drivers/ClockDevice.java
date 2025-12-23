package jinux.drivers;

import jinux.include.Const;
import jinux.kernel.Scheduler;

import java.util.Timer;
import java.util.TimerTask;

/**
 * 时钟设备
 * 对应 Linux 0.01 中的时钟中断 (kernel/sched.c 的 timer_interrupt)
 * 
 * 使用 Java Timer 模拟定时器中断
 * 
 * @author Jinux Project
 */
public class ClockDevice extends Device {
    
    /** 调度器引用 */
    private final Scheduler scheduler;
    
    /** Java 定时器 */
    private Timer timer;
    
    /** 是否运行中 */
    private volatile boolean running;
    
    /**
     * 构造时钟设备
     * 
     * @param scheduler 调度器
     */
    public ClockDevice(Scheduler scheduler) {
        super("clock", 0, 0);
        this.scheduler = scheduler;
        this.running = false;
    }
    
    @Override
    public void init() {
        System.out.println("[CLOCK] Clock device initialized: " + this);
        System.out.println("[CLOCK] Timer frequency: " + Const.HZ + " HZ (" + Const.TICK_MS + "ms per tick)");
    }
    
    /**
     * 启动时钟中断
     */
    public void start() {
        if (running) {
            return;
        }
        
        running = true;
        timer = new Timer("ClockInterrupt", true);
        
        // 定时触发时钟中断
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                timerInterrupt();
            }
        }, Const.TICK_MS, Const.TICK_MS);
        
        System.out.println("[CLOCK] Timer started");
    }
    
    /**
     * 停止时钟中断
     */
    public void stop() {
        if (!running) {
            return;
        }
        
        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        
        System.out.println("[CLOCK] Timer stopped");
    }
    
    /**
     * 时钟中断处理
     */
    private void timerInterrupt() {
        // 调用调度器的时钟中断处理
        scheduler.timerInterrupt();
    }
    
    /**
     * 是否正在运行
     */
    public boolean isRunning() {
        return running;
    }
}
