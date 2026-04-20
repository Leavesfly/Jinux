package jinux.kernel;

import jinux.include.Const;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 进程调度器
 * 对应 Linux 0.01 中的 kernel/sched.c
 * 
 * 实现进程调度算法（时间片轮转 + 优先级）
 * 统一使用 ReentrantLock 作为锁策略，增加 PID→Task 的 HashMap 索引
 * 
 * @author Jinux Project
 */
public class Scheduler {
    
    /** 进程表（task_struct 数组） */
    private final Task[] taskTable;
    
    /** PID 到 Task 的快速索引（O(1) 查找） */
    private final Map<Integer, Task> pidIndex;
    
    /** 当前运行进程 */
    private Task currentTask;
    
    /** 下一个可用的 PID */
    private int nextPid;
    
    /** 统一的调度器锁 */
    private final ReentrantLock schedulerLock;
    
    /** 系统时钟滴答计数（使用 AtomicLong 减少锁竞争） */
    private final AtomicLong jiffies;
    
    /**
     * 构造调度器
     */
    public Scheduler() {
        this.taskTable = new Task[Const.NR_TASKS];
        this.pidIndex = new HashMap<>();
        this.currentTask = null;
        this.nextPid = 0;
        this.schedulerLock = new ReentrantLock();
        this.jiffies = new AtomicLong(0);
    }
    
    /**
     * 分配进程槽位
     * 
     * @return 进程槽位索引，失败返回 -1
     */
    private int allocateTaskSlot() {
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
    public boolean addTask(Task task) {
        schedulerLock.lock();
        try {
            int slot = allocateTaskSlot();
            if (slot < 0) {
                System.err.println("[SCHED] ERROR: Task table full!");
                return false;
            }
            
            taskTable[slot] = task;
            pidIndex.put(task.getPid(), task);
            System.out.println("[SCHED] Task added: " + task);
            return true;
        } finally {
            schedulerLock.unlock();
        }
    }
    
    /**
     * 分配新的 PID
     * 
     * @return PID
     */
    public int allocatePid() {
        schedulerLock.lock();
        try {
            return nextPid++;
        } finally {
            schedulerLock.unlock();
        }
    }
    
    /**
     * 根据 PID 查找进程（O(1) 复杂度）
     * 
     * @param pid 进程 ID
     * @return 进程对象，不存在返回 null
     */
    public Task findTask(int pid) {
        schedulerLock.lock();
        try {
            return pidIndex.get(pid);
        } finally {
            schedulerLock.unlock();
        }
    }
    
    /**
     * 移除进程
     * 
     * @param pid 进程 ID
     */
    public void removeTask(int pid) {
        schedulerLock.lock();
        try {
            for (int i = 0; i < taskTable.length; i++) {
                if (taskTable[i] != null && taskTable[i].getPid() == pid) {
                    System.out.println("[SCHED] Task removed: " + taskTable[i]);
                    taskTable[i] = null;
                    break;
                }
            }
            pidIndex.remove(pid);
        } finally {
            schedulerLock.unlock();
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
            
            for (Task task : taskTable) {
                if (task != null && task.getState() == Const.TASK_RUNNING) {
                    if (task.getCounter() > maxCounter) {
                        maxCounter = task.getCounter();
                        next = task;
                    }
                }
            }
            
            // 如果没有可运行进程或所有时间片用完，重新分配
            if (next == null || maxCounter == 0) {
                for (Task task : taskTable) {
                    if (task != null && task.getState() != Const.TASK_ZOMBIE) {
                        task.setCounter(task.getCounter() / 2 + task.getPriority());
                    }
                }
                
                maxCounter = -1;
                for (Task task : taskTable) {
                    if (task != null && task.getState() == Const.TASK_RUNNING) {
                        if (task.getCounter() > maxCounter) {
                            maxCounter = task.getCounter();
                            next = task;
                        }
                    }
                }
            }
            
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
     * jiffies 使用 AtomicLong 无锁递增，减少高频锁竞争
     */
    public void timerInterrupt() {
        jiffies.incrementAndGet();
        
        schedulerLock.lock();
        try {
            if (currentTask != null) {
                currentTask.addUtime(1);
                currentTask.decrementCounter();
                
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
    public void wakeUp(String condition) {
        schedulerLock.lock();
        try {
            for (Task task : taskTable) {
                if (task != null && 
                    (task.getState() == Const.TASK_INTERRUPTIBLE || 
                     task.getState() == Const.TASK_UNINTERRUPTIBLE)) {
                    task.wakeUp();
                }
            }
        } finally {
            schedulerLock.unlock();
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
                schedule();
            }
        } finally {
            schedulerLock.unlock();
        }
    }
    
    /**
     * 打印进程表
     */
    public void printProcessList() {
        schedulerLock.lock();
        try {
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
        } finally {
            schedulerLock.unlock();
        }
    }
    
    // ==================== Getters ====================
    
    public Task getCurrentTask() {
        return currentTask;
    }
    
    public long getJiffies() {
        return jiffies.get();
    }
    
    public Task[] getTaskTable() {
        return taskTable;
    }
}
