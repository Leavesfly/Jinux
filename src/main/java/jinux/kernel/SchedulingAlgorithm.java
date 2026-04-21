package jinux.kernel;

/**
 * 调度算法策略接口
 * 
 * 遵循开闭原则（OCP）和策略模式，将调度算法从 Scheduler 中解耦。
 * 新增调度算法只需实现此接口，无需修改 Scheduler 源码。
 *
 * @author Jinux Project
 */
public interface SchedulingAlgorithm {

    /**
     * 从进程表中选择下一个要运行的进程
     *
     * @param taskTable 进程表
     * @param currentTask 当前运行的进程（可能为 null）
     * @return 选中的下一个进程，如果没有可运行进程返回 null
     */
    Task selectNextTask(Task[] taskTable, Task currentTask);

    /**
     * 当所有可运行进程的时间片耗尽时，重新分配时间片
     *
     * @param taskTable 进程表
     */
    void redistributeCounters(Task[] taskTable);
}
