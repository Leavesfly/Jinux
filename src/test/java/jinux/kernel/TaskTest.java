package jinux.kernel;

import jinux.include.Const;
import jinux.mm.AddressSpace;
import jinux.mm.MemoryManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Task类的单元测试
 */
public class TaskTest {
    
    private MemoryManager memoryManager;
    private AddressSpace addressSpace;
    
    @BeforeEach
    void setUp() {
        memoryManager = new MemoryManager();
        addressSpace = memoryManager.createAddressSpace();
    }
    
    @Test
    void testTaskCreation() {
        Task task = new Task(1, 0, addressSpace);
        
        assertEquals(1, task.getPid());
        assertEquals(0, task.getPpid());
        assertEquals(Const.TASK_RUNNING, task.getState());
        assertEquals(Const.DEF_PRIORITY, task.getPriority());
        assertEquals(Const.DEF_COUNTER, task.getCounter());
        assertNotNull(task.getFdTable());
        assertEquals(1, task.getCurrentWorkingDir());
        assertEquals(0, task.getExitCode());
    }
    
    @Test
    void testStateTransitions() {
        Task task = new Task(1, 0, addressSpace);
        
        // 测试切换到睡眠状态
        task.sleep(true);
        assertEquals(Const.TASK_INTERRUPTIBLE, task.getState());
        
        task.sleep(false);
        assertEquals(Const.TASK_UNINTERRUPTIBLE, task.getState());
        
        // 测试唤醒
        task.wakeUp();
        assertEquals(Const.TASK_RUNNING, task.getState());
    }
    
    @Test
    void testCounterOperations() {
        Task task = new Task(1, 0, addressSpace);
        int initialCounter = task.getCounter();
        
        // 测试减少时间片
        task.decrementCounter();
        assertEquals(initialCounter - 1, task.getCounter());
        
        // 测试重置时间片
        task.resetCounter();
        assertEquals(task.getPriority(), task.getCounter());
    }
    
    @Test
    void testIsRunnable() {
        Task task = new Task(1, 0, addressSpace);
        
        // 初始状态应该是可运行的
        assertTrue(task.isRunnable());
        
        // 时间片用完时不可运行
        task.setCounter(0);
        assertFalse(task.isRunnable());
        
        // 睡眠状态不可运行
        task.setCounter(10);
        task.sleep(true);
        assertFalse(task.isRunnable());
    }
    
    @Test
    void testExit() {
        Task task = new Task(1, 0, addressSpace);
        int exitCode = 42;
        
        task.exit(exitCode);
        
        assertEquals(exitCode, task.getExitCode());
        assertEquals(Const.TASK_ZOMBIE, task.getState());
    }
    
    @Test
    void testResetForExec() {
        Task task = new Task(1, 0, addressSpace);
        
        // 设置一些状态
        task.setPriority(20);
        task.sendSignal(Signal.SIGTERM);
        
        // 重置用于exec
        task.resetForExec();
        
        // 信号应该被清除
        assertFalse(task.hasPendingSignals());
        assertEquals(0, task.getSignalPending());
        assertEquals(0, task.getSignalBlocked());
    }
    
    @Test
    void testSignalOperations() {
        Task task = new Task(1, 0, addressSpace);
        
        // 发送信号
        task.sendSignal(Signal.SIGTERM);
        assertTrue(task.hasPendingSignals());
        
        // 获取下一个信号
        int signum = task.getNextSignal();
        assertEquals(Signal.SIGTERM, signum);
        
        // 清除信号
        task.clearSignal(Signal.SIGTERM);
        assertFalse(task.hasPendingSignals());
    }
    
    @Test
    void testSignalKill() {
        Task task = new Task(1, 0, addressSpace);
        task.sleep(true);
        
        // SIGKILL应该立即唤醒进程
        task.sendSignal(Signal.SIGKILL);
        assertEquals(Const.TASK_RUNNING, task.getState());
    }
    
    @Test
    void testSignalCont() {
        Task task = new Task(1, 0, addressSpace);
        task.setState(Const.TASK_STOPPED);
        
        // SIGCONT应该唤醒停止的进程
        task.sendSignal(Signal.SIGCONT);
        assertEquals(Const.TASK_RUNNING, task.getState());
    }
    
    @Test
    void testTimeTracking() {
        Task task = new Task(1, 0, addressSpace);
        
        assertEquals(0, task.getUtime());
        assertEquals(0, task.getStime());
        
        task.addUtime(10);
        task.addStime(5);
        
        assertEquals(10, task.getUtime());
        assertEquals(5, task.getStime());
    }
    
    @Test
    void testGetStateName() {
        Task task = new Task(1, 0, addressSpace);
        
        assertEquals("RUNNING", task.getStateName());
        
        task.sleep(true);
        assertEquals("INTERRUPTIBLE", task.getStateName());
        
        task.sleep(false);
        assertEquals("UNINTERRUPTIBLE", task.getStateName());
        
        task.exit(0);
        assertEquals("ZOMBIE", task.getStateName());
        
        task.setState(Const.TASK_STOPPED);
        assertEquals("STOPPED", task.getStateName());
    }
    
    @Test
    void testToString() {
        Task task = new Task(1, 0, addressSpace);
        String str = task.toString();
        
        assertTrue(str.contains("pid=1"));
        assertTrue(str.contains("ppid=0"));
        assertTrue(str.contains("RUNNING"));
    }
}

