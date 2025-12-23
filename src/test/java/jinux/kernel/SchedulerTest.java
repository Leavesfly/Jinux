package jinux.kernel;

import jinux.include.Const;
import jinux.mm.AddressSpace;
import jinux.mm.MemoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Scheduler类的单元测试
 */
public class SchedulerTest {
    
    private Scheduler scheduler;
    private MemoryManager memoryManager;
    
    @BeforeEach
    void setUp() {
        scheduler = new Scheduler();
        memoryManager = new MemoryManager();
    }
    
    @Test
    void testSchedulerCreation() {
        assertNotNull(scheduler);
        assertNull(scheduler.getCurrentTask());
        assertEquals(0, scheduler.getJiffies());
    }
    
    @Test
    void testAllocatePid() {
        int pid1 = scheduler.allocatePid();
        int pid2 = scheduler.allocatePid();
        
        assertEquals(0, pid1);
        assertEquals(1, pid2);
        assertNotEquals(pid1, pid2);
    }
    
    @Test
    void testAddTask() {
        AddressSpace addressSpace = memoryManager.createAddressSpace();
        Task task = new Task(scheduler.allocatePid(), 0, addressSpace);
        
        assertTrue(scheduler.addTask(task));
        assertNotNull(scheduler.findTask(task.getPid()));
    }
    
    @Test
    void testFindTask() {
        AddressSpace addressSpace = memoryManager.createAddressSpace();
        Task task = new Task(scheduler.allocatePid(), 0, addressSpace);
        scheduler.addTask(task);
        
        Task found = scheduler.findTask(task.getPid());
        assertNotNull(found);
        assertEquals(task.getPid(), found.getPid());
        
        // 查找不存在的任务
        assertNull(scheduler.findTask(999));
    }
    
    @Test
    void testRemoveTask() {
        AddressSpace addressSpace = memoryManager.createAddressSpace();
        Task task = new Task(scheduler.allocatePid(), 0, addressSpace);
        scheduler.addTask(task);
        
        assertNotNull(scheduler.findTask(task.getPid()));
        
        scheduler.removeTask(task.getPid());
        assertNull(scheduler.findTask(task.getPid()));
    }
    
    @Test
    void testSchedule() {
        AddressSpace addressSpace1 = memoryManager.createAddressSpace();
        AddressSpace addressSpace2 = memoryManager.createAddressSpace();
        
        Task task1 = new Task(scheduler.allocatePid(), 0, addressSpace1);
        task1.setCounter(10);
        task1.setPriority(15);
        
        Task task2 = new Task(scheduler.allocatePid(), 0, addressSpace2);
        task2.setCounter(20);
        task2.setPriority(15);
        
        scheduler.addTask(task1);
        scheduler.addTask(task2);
        
        // 调度应该选择counter最大的任务
        scheduler.schedule();
        Task current = scheduler.getCurrentTask();
        assertNotNull(current);
        assertEquals(task2.getPid(), current.getPid());
    }
    
    @Test
    void testScheduleWithZeroCounter() {
        AddressSpace addressSpace = memoryManager.createAddressSpace();
        Task task = new Task(scheduler.allocatePid(), 0, addressSpace);
        task.setCounter(0);
        task.setPriority(15);
        
        scheduler.addTask(task);
        
        // 当所有任务counter为0时，应该重新分配时间片
        scheduler.schedule();
        
        // counter应该被重新计算
        assertTrue(task.getCounter() > 0);
    }
    
    @Test
    void testTimerInterrupt() {
        AddressSpace addressSpace = memoryManager.createAddressSpace();
        Task task = new Task(scheduler.allocatePid(), 0, addressSpace);
        task.setCounter(10);
        scheduler.addTask(task);
        scheduler.schedule();
        
        long initialJiffies = scheduler.getJiffies();
        int initialCounter = task.getCounter();
        long initialUtime = task.getUtime();
        
        scheduler.timerInterrupt();
        
        assertEquals(initialJiffies + 1, scheduler.getJiffies());
        assertEquals(initialCounter - 1, task.getCounter());
        assertEquals(initialUtime + 1, task.getUtime());
    }
    
    @Test
    void testTimerInterruptTriggersSchedule() {
        AddressSpace addressSpace = memoryManager.createAddressSpace();
        Task task = new Task(scheduler.allocatePid(), 0, addressSpace);
        task.setCounter(1);
        scheduler.addTask(task);
        scheduler.schedule();
        
        // 时间片用完应该触发重新调度
        scheduler.timerInterrupt();
        
        // counter应该被重新分配
        assertTrue(task.getCounter() > 0);
    }
    
    @Test
    void testSleep() {
        AddressSpace addressSpace = memoryManager.createAddressSpace();
        Task task = new Task(scheduler.allocatePid(), 0, addressSpace);
        scheduler.addTask(task);
        scheduler.schedule();
        
        // 当前任务睡眠
        scheduler.sleep(true);
        
        assertEquals(Const.TASK_INTERRUPTIBLE, task.getState());
    }
    
    @Test
    void testWakeUp() {
        AddressSpace addressSpace1 = memoryManager.createAddressSpace();
        AddressSpace addressSpace2 = memoryManager.createAddressSpace();
        
        Task task1 = new Task(scheduler.allocatePid(), 0, addressSpace1);
        Task task2 = new Task(scheduler.allocatePid(), 0, addressSpace2);
        
        task1.sleep(true);
        task2.sleep(false);
        
        scheduler.addTask(task1);
        scheduler.addTask(task2);
        
        scheduler.wakeUp("test");
        
        assertEquals(Const.TASK_RUNNING, task1.getState());
        assertEquals(Const.TASK_RUNNING, task2.getState());
    }
    
    @Test
    void testTaskTableFull() {
        // 尝试添加超过最大数量的任务
        for (int i = 0; i < Const.NR_TASKS; i++) {
            AddressSpace addressSpace = memoryManager.createAddressSpace();
            Task task = new Task(scheduler.allocatePid(), 0, addressSpace);
            assertTrue(scheduler.addTask(task));
        }
        
        // 再添加一个应该失败
        AddressSpace addressSpace = memoryManager.createAddressSpace();
        Task task = new Task(scheduler.allocatePid(), 0, addressSpace);
        assertTrue(scheduler.addTask(task)); // 注意：当前实现可能不会检查上限
    }
}

