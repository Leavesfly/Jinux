package jinux.kernel;

import jinux.include.ProcessConstants;

/**
 * Linux 0.01 原始调度算法实现
 * 
 * 算法核心：选择 counter 最大的可运行进程。
 * 当所有进程的 counter 为 0 时，按公式重新分配：
 *   counter = counter / 2 + priority
 *
 * @author Jinux Project
 */
public class LinuxSchedulingAlgorithm implements SchedulingAlgorithm {

    /** 时间片衰减因子：重新分配时 counter 除以此值 */
    private static final int COUNTER_DECAY_DIVISOR = 2;

    @Override
    public Task selectNextTask(Task[] taskTable, Task currentTask) {
        Task selectedTask = null;
        int maxCounter = -1;

        for (Task task : taskTable) {
            if (task != null && task.getState() == ProcessConstants.TASK_RUNNING) {
                if (task.getCounter() > maxCounter) {
                    maxCounter = task.getCounter();
                    selectedTask = task;
                }
            }
        }

        if (selectedTask == null || maxCounter == 0) {
            redistributeCounters(taskTable);
            return findHighestCounterTask(taskTable);
        }

        return selectedTask;
    }

    @Override
    public void redistributeCounters(Task[] taskTable) {
        for (Task task : taskTable) {
            if (task != null && task.getState() != ProcessConstants.TASK_ZOMBIE) {
                task.setCounter(task.getCounter() / COUNTER_DECAY_DIVISOR + task.getPriority());
            }
        }
    }

    /**
     * 从进程表中找到 counter 最大的可运行进程
     */
    private Task findHighestCounterTask(Task[] taskTable) {
        Task selectedTask = null;
        int maxCounter = -1;

        for (Task task : taskTable) {
            if (task != null && task.getState() == ProcessConstants.TASK_RUNNING) {
                if (task.getCounter() > maxCounter) {
                    maxCounter = task.getCounter();
                    selectedTask = task;
                }
            }
        }

        return selectedTask;
    }
}
