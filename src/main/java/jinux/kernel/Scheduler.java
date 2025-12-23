package jinux.kernel;

import jinux.include.Const;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 进程调度器
 * 对应 Linux 0.01 中的 kernel/sched.c
 * 
 * 实现进程调度算法（时间片轮转 + 优先级）
 * 
 * @author Jinux Project
 */
public class Scheduler {
    
    /** 进程表（task_struct 数组） */
    private final Task[] taskTable;
    
    /** 当前运行进程 */
    private Task currentTask;
    
    /** 下一个可用的 PID */
    private int nextPid;
    
    /** 调度器锁 */
    private final ReentrantLock schedulerLock;
    
    /** 系统时钟滴答计数 */
    private long jiffies;
    
    /**
     * 构造调度器
     */
    public Scheduler() {
        this.taskTable = new Task[Const.NR_TASKS];
        this.currentTask = null;
        this.nextPid = 0;
        this.schedulerLock = new ReentrantLock();
        this.jiffies = 0;
    }
    
    /**
     * 分配进程槽位
     * 
     * @return 进程槽位索引，失败返回 -1
     */
    private synchronized int allocateTaskSlot() {
        for (int i = 0; i < taskTable.length; i++) {
            if (taskTable[i] == null) {
                return i;
            }
        }
        return -1;
    }
    
    /**
     * 添加进程到进程表
     * 
     * @param task 进程
     * @return 是否成功
     */
    public synchronized boolean addTask(Task task) {
        int slot = allocateTaskSlot();
        if (slot < 0) {
            System.err.println("[SCHED] ERROR: Task table full!");
            return false;
        }
        
        taskTable[slot] = task;
        System.out.println("[SCHED] Task added: " + task);
        return true;
    }
    
    /**
     * 分配新的 PID
     * 
     * @return PID
     */
    public synchronized int allocatePid() {
        return nextPid++;
    }
    
    /**
     * 根据 PID 查找进程
     * 
     * @param pid 进程 ID
     * @return 进程对象，不存在返回 null
     */
    public synchronized Task findTask(int pid) {
        for (Task task : taskTable) {
            if (task != null && task.getPid() == pid) {
                return task;
            }
        }
        return null;
    }
    
    /**
     * 移除进程
     * 
     * @param pid 进程 ID
     */
    public synchronized void removeTask(int pid) {
        for (int i = 0; i < taskTable.length; i++) {
            if (taskTable[i] != null && taskTable[i].getPid() == pid) {
                System.out.println("[SCHED] Task removed: " + taskTable[i]);
                taskTable[i] = null;
                break;
            }
        }
    }
    
    /**
     * 调度算法：选择下一个要运行的进程
     * 对应 Linux 0.01 的 schedule() 函数
     * 
     * 算法：选择 counter 最大且状态为 RUNNING 的进程
     */
    public void schedule() {
        schedulerLock.lock();
        try {
            Task next = null;
            int maxCounter = -1;
            
            // 选择 counter 最大的可运行进程
            for (Task task : taskTable) {
                if (task != null && task.getState() == Const.TASK_RUNNING) {
                    if (task.getCounter() > maxCounter) {
                        maxCounter = task.getCounter();
                        next = task;
                    }
                }
            }
            
            // 如果没有可运行进程，重新分配时间片
            if (next == null || maxCounter == 0) {
                for (Task task : taskTable) {
                    if (task != null && task.getState() != Const.TASK_ZOMBIE) {
                        // 重新计算时间片：counter = counter/2 + priority
                        task.setCounter(task.getCounter() / 2 + task.getPriority());
                    }
                }
                
                // 重新选择
                for (Task task : taskTable) {
                    if (task != null && task.getState() == Const.TASK_RUNNING) {
                        if (task.getCounter() > maxCounter) {
                            maxCounter = task.getCounter();
                            next = task;
                        }
                    }
                }
            }
            
            // 切换进程
            if (next != null && next != currentTask) {
                Task prev = currentTask;
                currentTask = next;
                
                if (prev != null) {
                    System.out.println("[SCHED] Context switch: " + prev.getPid() + " -> " + next.getPid());
                } else {
                    System.out.println("[SCHED] Starting task: " + next.getPid());
                }
            }
            
        } finally {
            schedulerLock.unlock();
        }
    }
    
    /**
     * 时钟中断处理
     * 对应 Linux 0.01 的 do_timer()
     */
    public void timerInterrupt() {
        jiffies++;
        
        schedulerLock.lock();
        try {
            // 更新当前进程的时间统计
            if (currentTask != null) {
                currentTask.addUtime(1); // 简化：假设都在用户态
                currentTask.decrementCounter();
                
                // 时间片用完，重新调度
                if (currentTask.getCounter() <= 0) {
                    schedule();
                }
            }
        } finally {
            schedulerLock.unlock();
        }
    }
    
    /**
     * 唤醒等待指定条件的进程
     * 
     * @param condition 条件（简化：用字符串表示）
     */
    public synchronized void wakeUp(String condition) {
        // 简化实现：唤醒所有睡眠的进程
        // 实际 Linux 有更复杂的等待队列机制
        for (Task task : taskTable) {
            if (task != null && 
                (task.getState() == Const.TASK_INTERRUPTIBLE || 
                 task.getState() == Const.TASK_UNINTERRUPTIBLE)) {
                task.wakeUp();
            }
        }
    }
    
    /**
     * 当前进程睡眠
     * 
     * @param interruptible 是否可中断
     */
    public void sleep(boolean interruptible) {
        schedulerLock.lock();
        try {
            if (currentTask != null) {
                currentTask.sleep(interruptible);
                schedule(); // 立即调度其他进程
            }
        } finally {
            schedulerLock.unlock();
        }
    }
    
    /**
     * 打印进程表
     */
    public synchronized void printProcessList() {
        System.out.println("\n========== Process List ==========");
        System.out.println("PID\tPPID\tSTATE\t\tCOUNTER\tPRIORITY");
        
        for (Task task : taskTable) {
            if (task != null) {
                System.out.printf("%d\t%d\t%s\t%d\t%d\n",
                    task.getPid(),
                    task.getPpid(),
                    task.getStateName(),
                    task.getCounter(),
                    task.getPriority());
            }
        }
        
        System.out.println("==================================\n");
    }
    
    // ==================== Getters ====================
    
    public Task getCurrentTask() {
        return currentTask;
    }
    
    public long getJiffies() {
        return jiffies;
    }
    
    public Task[] getTaskTable() {
        return taskTable;
    }
}
