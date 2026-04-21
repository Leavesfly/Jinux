package jinux.demo;

import jinux.kernel.Kernel;
import jinux.kernel.Task;
import jinux.kernel.Scheduler;
import jinux.kernel.Signal;
import jinux.mm.IMemoryManager;
import jinux.ipc.Pipe;
import jinux.lib.LibC;
import jinux.drivers.ConsoleDevice;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Jinux 操作系统教学项目 - 交互式模拟实验系统
 * 让学生通过动手操作来理解操作系统核心概念
 */
public class LabSimulator {

    // ANSI 颜色常量
    private static final String ANSI_RESET = "\033[0m";
    private static final String ANSI_RED = "\033[31m";
    private static final String ANSI_GREEN = "\033[32m";
    private static final String ANSI_YELLOW = "\033[33m";
    private static final String ANSI_BLUE = "\033[34m";
    private static final String ANSI_CYAN = "\033[36m";
    private static final String ANSI_BOLD = "\033[1m";

    private static BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    /**
     * 主入口 - 显示实验菜单
     */
    public static void runLab(Kernel kernel) {
        while (true) {
            kernel.getConsole().println("\n" + ANSI_BOLD + ANSI_CYAN + "╔════════════════════════════════════════╗");
            kernel.getConsole().println("║   Jinux 操作系统交互式实验系统       ║");
            kernel.getConsole().println("╚════════════════════════════════════════╝" + ANSI_RESET);
            kernel.getConsole().println("\n请选择实验：");
            kernel.getConsole().println(ANSI_GREEN + "  1. 进程调度实验" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  2. 内存分配实验" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  3. 管道通信实验" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  4. 信号处理实验" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  5. Fork过程实验" + ANSI_RESET);
            kernel.getConsole().println(ANSI_RED + "  6. 返回主菜单" + ANSI_RESET);
            kernel.getConsole().print("\n请输入选项 (1-6): ");

            try {
                String input = reader.readLine();
                if (input == null) break;
                int choice = Integer.parseInt(input.trim());

                switch (choice) {
                    case 1:
                        labProcessScheduling(kernel);
                        break;
                    case 2:
                        labMemoryAllocation(kernel);
                        break;
                    case 3:
                        labPipeCommunication(kernel);
                        break;
                    case 4:
                        labSignalHandling(kernel);
                        break;
                    case 5:
                        labForkProcess(kernel);
                        break;
                    case 6:
                        kernel.getConsole().println(ANSI_YELLOW + "返回主菜单..." + ANSI_RESET);
                        return;
                    default:
                        kernel.getConsole().println(ANSI_RED + "无效选项，请输入 1-6" + ANSI_RESET);
                }
            } catch (Exception e) {
                kernel.getConsole().println(ANSI_RED + "输入错误: " + e.getMessage() + ANSI_RESET);
            }
        }
    }

    // ==================== 实验1: 进程调度模拟器 ====================

    /**
     * 实验1: 进程调度模拟器
     * 模拟 Linux 0.01 的调度算法
     */
    private static void labProcessScheduling(Kernel kernel) {
        kernel.getConsole().println("\n" + ANSI_BOLD + ANSI_BLUE + "=== 实验1: 进程调度模拟器 ===" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "实验目标: 理解 Linux 0.01 的时间片轮转调度算法" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "核心概念: counter(时间片计数器), priority(优先级), state(状态)" + ANSI_RESET);

        // 创建模拟进程
        List<ProcessSim> processes = new ArrayList<>();
        processes.add(new ProcessSim(1, "init", 15, 15, "READY"));
        processes.add(new ProcessSim(2, "shell", 12, 12, "READY"));
        processes.add(new ProcessSim(3, "editor", 10, 10, "READY"));
        processes.add(new ProcessSim(4, "compiler", 8, 8, "READY"));
        processes.add(new ProcessSim(5, "daemon", 5, 5, "READY"));

        int round = 0;
        int maxRounds = 8;

        while (round < maxRounds) {
            round++;
            kernel.getConsole().println("\n" + ANSI_CYAN + "--- 第 " + round + " 轮调度 ---" + ANSI_RESET);

            // 展示当前进程状态
            printProcessTable(kernel, processes);

            // 找出 counter 最大的 READY 进程
            ProcessSim nextProcess = findNextProcess(processes);

            if (nextProcess == null) {
                kernel.getConsole().println(ANSI_YELLOW + "所有进程都已结束或等待，重新计算 counter..." + ANSI_RESET);
                recalculateCounters(processes);
                continue;
            }

            // 提问学生
            kernel.getConsole().print(ANSI_BOLD + "\n问题: 根据 Linux 0.01 调度算法，下一个应该运行哪个进程？（输入PID）: " + ANSI_RESET);
            try {
                String input = reader.readLine();
                if (input == null) break;
                int studentAnswer = Integer.parseInt(input.trim());

                if (studentAnswer == nextProcess.pid) {
                    kernel.getConsole().println(ANSI_GREEN + "✓ 正确！选择了 PID=" + nextProcess.pid + " (" + nextProcess.name + ")" + ANSI_RESET);
                    kernel.getConsole().println(ANSI_YELLOW + "  原因: 该进程的 counter=" + nextProcess.counter + " 是所有 READY 进程中最大的" + ANSI_RESET);
                } else {
                    kernel.getConsole().println(ANSI_RED + "✗ 错误！正确答案是 PID=" + nextProcess.pid + " (" + nextProcess.name + ")" + ANSI_RESET);
                    kernel.getConsole().println(ANSI_YELLOW + "  原因: 该进程的 counter=" + nextProcess.counter + " 是所有 READY 进程中最大的" + ANSI_RESET);
                    kernel.getConsole().println(ANSI_YELLOW + "  你的选择 PID=" + studentAnswer + " 的 counter 不是最大的" + ANSI_RESET);
                }
            } catch (Exception e) {
                kernel.getConsole().println(ANSI_RED + "输入错误" + ANSI_RESET);
            }

            // 模拟运行一个时间片
            kernel.getConsole().println(ANSI_CYAN + "\n>>> 运行进程 PID=" + nextProcess.pid + " (" + nextProcess.name + ") 一个时间片" + ANSI_RESET);
            nextProcess.state = "RUNNING";
            printProcessTable(kernel, processes);

            nextProcess.counter--;
            kernel.getConsole().println(ANSI_YELLOW + "  counter 从 " + (nextProcess.counter + 1) + " 减为 " + nextProcess.counter + ANSI_RESET);

            if (nextProcess.counter <= 0) {
                nextProcess.state = "READY";
                kernel.getConsole().println(ANSI_YELLOW + "  时间片用完，进程回到 READY 状态" + ANSI_RESET);
            } else {
                nextProcess.state = "READY";
            }

            // 检查是否所有进程 counter 都为 0
            boolean allZero = true;
            for (ProcessSim p : processes) {
                if (p.counter > 0) {
                    allZero = false;
                    break;
                }
            }

            if (allZero) {
                kernel.getConsole().println(ANSI_BOLD + "\n所有进程时间片用完！" + ANSI_RESET);
                kernel.getConsole().print(ANSI_BOLD + "问题: 新的 counter 值应该如何计算？公式是 counter = counter/2 + priority\n" +
                        "请输入进程 PID=1 的新 counter 值 (priority=15): " + ANSI_RESET);
                try {
                    String input = reader.readLine();
                    if (input == null) break;
                    int studentAnswer = Integer.parseInt(input.trim());
                    int correctAnswer = 0 / 2 + 15; // counter=0, priority=15

                    if (studentAnswer == correctAnswer) {
                        kernel.getConsole().println(ANSI_GREEN + "✓ 正确！counter = 0/2 + 15 = 15" + ANSI_RESET);
                    } else {
                        kernel.getConsole().println(ANSI_RED + "✗ 错误！正确答案是 " + correctAnswer + ANSI_RESET);
                        kernel.getConsole().println(ANSI_YELLOW + "  计算: counter = 0/2 + 15 = 15" + ANSI_RESET);
                    }
                } catch (Exception e) {
                    kernel.getConsole().println(ANSI_RED + "输入错误" + ANSI_RESET);
                }
                recalculateCounters(processes);
            }

            printProcessTable(kernel, processes);

            // 暂停让学生观察
            kernel.getConsole().print(ANSI_CYAN + "\n按回车继续下一轮..." + ANSI_RESET);
            try {
                reader.readLine();
            } catch (Exception e) {
                break;
            }
        }

        kernel.getConsole().println(ANSI_GREEN + "\n✓ 进程调度实验完成！" + ANSI_RESET);
    }

    // 内部类：模拟进程
    private static class ProcessSim {
        int pid;
        String name;
        int priority;
        int counter;
        String state;

        ProcessSim(int pid, String name, int priority, int counter, String state) {
            this.pid = pid;
            this.name = name;
            this.priority = priority;
            this.counter = counter;
            this.state = state;
        }
    }

    private static void printProcessTable(Kernel kernel, List<ProcessSim> processes) {
        kernel.getConsole().println(String.format("%-6s %-12s %-10s %-10s %-10s", "PID", "Name", "Priority", "Counter", "State"));
        kernel.getConsole().println("----------------------------------------------------");
        for (ProcessSim p : processes) {
            String stateColor = p.state.equals("RUNNING") ? ANSI_GREEN : (p.state.equals("READY") ? ANSI_CYAN : ANSI_YELLOW);
            kernel.getConsole().println(String.format("%-6d %-12s %-10d %-10d %s%-10s%s",
                    p.pid, p.name, p.priority, p.counter, stateColor, p.state, ANSI_RESET));
        }
    }

    private static ProcessSim findNextProcess(List<ProcessSim> processes) {
        ProcessSim maxProcess = null;
        int maxCounter = -1;
        for (ProcessSim p : processes) {
            if (p.state.equals("READY") && p.counter > maxCounter) {
                maxCounter = p.counter;
                maxProcess = p;
            }
        }
        return maxProcess;
    }

    private static void recalculateCounters(List<ProcessSim> processes) {
        for (ProcessSim p : processes) {
            p.counter = p.counter / 2 + p.priority;
        }
    }

    // ==================== 实验2: 内存分配模拟器 ====================

    /**
     * 实验2: 内存分配模拟器
     * 模拟物理内存页面的分配和释放
     */
    private static void labMemoryAllocation(Kernel kernel) {
        kernel.getConsole().println("\n" + ANSI_BOLD + ANSI_BLUE + "=== 实验2: 内存分配模拟器 ===" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "实验目标: 理解物理内存页面管理和位图表示法" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "核心概念: 页面分配、页面释放、内存碎片" + ANSI_RESET);

        // 用 boolean[] 模拟 20 个物理内存页面
        boolean[] memoryPages = new boolean[20];
        // 初始状态：前5页已分配（内核使用）
        for (int i = 0; i < 5; i++) {
            memoryPages[i] = true;
        }

        while (true) {
            kernel.getConsole().println("\n" + ANSI_CYAN + "--- 当前内存状态 ---" + ANSI_RESET);
            printMemoryBitmap(kernel, memoryPages);
            printFragmentationInfo(kernel, memoryPages);

            kernel.getConsole().println("\n请选择操作：");
            kernel.getConsole().println(ANSI_GREEN + "  1. 分配 N 页" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  2. 释放页 X" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  3. 查看位图" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  4. 计算碎片率" + ANSI_RESET);
            kernel.getConsole().println(ANSI_RED + "  5. 返回" + ANSI_RESET);
            kernel.getConsole().print("\n请输入选项 (1-5): ");

            try {
                String input = reader.readLine();
                if (input == null) break;
                int choice = Integer.parseInt(input.trim());

                switch (choice) {
                    case 1:
                        kernel.getConsole().print("请输入要分配的页数: ");
                        String pagesInput = reader.readLine();
                        if (pagesInput != null) {
                            int n = Integer.parseInt(pagesInput.trim());
                            allocatePages(kernel, memoryPages, n);
                        }
                        break;
                    case 2:
                        kernel.getConsole().print("请输入要释放的页号 (0-19): ");
                        String pageInput = reader.readLine();
                        if (pageInput != null) {
                            int x = Integer.parseInt(pageInput.trim());
                            releasePage(kernel, memoryPages, x);
                        }
                        break;
                    case 3:
                        printMemoryBitmap(kernel, memoryPages);
                        break;
                    case 4:
                        printFragmentationInfo(kernel, memoryPages);
                        break;
                    case 5:
                        kernel.getConsole().println(ANSI_YELLOW + "返回主菜单..." + ANSI_RESET);
                        return;
                    default:
                        kernel.getConsole().println(ANSI_RED + "无效选项" + ANSI_RESET);
                }
            } catch (Exception e) {
                kernel.getConsole().println(ANSI_RED + "输入错误: " + e.getMessage() + ANSI_RESET);
            }
        }
    }

    private static void printMemoryBitmap(Kernel kernel, boolean[] memoryPages) {
        kernel.getConsole().println("\n内存位图 (20页):");
        kernel.getConsole().print("页号: ");
        for (int i = 0; i < 20; i++) {
            kernel.getConsole().print(String.format("%2d ", i));
        }
        kernel.getConsole().println("");

        kernel.getConsole().print("状态: ");
        for (int i = 0; i < 20; i++) {
            if (memoryPages[i]) {
                kernel.getConsole().print(ANSI_RED + "[█] " + ANSI_RESET);
            } else {
                kernel.getConsole().print(ANSI_GREEN + "[░] " + ANSI_RESET);
            }
        }
        kernel.getConsole().println("");
        kernel.getConsole().println(ANSI_RED + "[█] 已分配" + ANSI_RESET + "  " + ANSI_GREEN + "[░] 空闲" + ANSI_RESET);
    }

    private static void printFragmentationInfo(Kernel kernel, boolean[] memoryPages) {
        int freePages = 0;
        int totalFreeBlocks = 0;
        int currentBlock = 0;

        for (int i = 0; i < memoryPages.length; i++) {
            if (!memoryPages[i]) {
                freePages++;
                currentBlock++;
            } else {
                if (currentBlock > 0) {
                    totalFreeBlocks++;
                }
                currentBlock = 0;
            }
        }
        if (currentBlock > 0) {
            totalFreeBlocks++;
        }

        double fragmentationRate = freePages > 0 ? (1.0 - (double)totalFreeBlocks / freePages) * 100 : 0;

        kernel.getConsole().println(ANSI_CYAN + "\n内存统计:" + ANSI_RESET);
        kernel.getConsole().println("  总页数: " + memoryPages.length);
        kernel.getConsole().println("  已分配: " + (memoryPages.length - freePages) + " 页");
        kernel.getConsole().println("  空闲: " + freePages + " 页");
        kernel.getConsole().println("  空闲块数: " + totalFreeBlocks);
        kernel.getConsole().println(String.format("  碎片率: %.2f%%", fragmentationRate));
    }

    private static void allocatePages(Kernel kernel, boolean[] memoryPages, int n) {
        // 找到连续的空闲页面
        int startIdx = -1;
        int consecutiveFree = 0;

        for (int i = 0; i < memoryPages.length; i++) {
            if (!memoryPages[i]) {
                if (consecutiveFree == 0) {
                    startIdx = i;
                }
                consecutiveFree++;
                if (consecutiveFree >= n) {
                    break;
                }
            } else {
                consecutiveFree = 0;
                startIdx = -1;
            }
        }

        if (consecutiveFree >= n) {
            for (int i = startIdx; i < startIdx + n; i++) {
                memoryPages[i] = true;
            }
            kernel.getConsole().println(ANSI_GREEN + "✓ 成功分配 " + n + " 页，起始页号: " + startIdx + ANSI_RESET);
        } else {
            kernel.getConsole().println(ANSI_RED + "✗ 无法找到连续的 " + n + " 个空闲页面" + ANSI_RESET);
        }
    }

    private static void releasePage(Kernel kernel, boolean[] memoryPages, int x) {
        if (x < 0 || x >= memoryPages.length) {
            kernel.getConsole().println(ANSI_RED + "✗ 页号超出范围 (0-19)" + ANSI_RESET);
            return;
        }
        if (!memoryPages[x]) {
            kernel.getConsole().println(ANSI_YELLOW + "⚠ 页 " + x + " 已经是空闲状态" + ANSI_RESET);
        } else {
            memoryPages[x] = false;
            kernel.getConsole().println(ANSI_GREEN + "✓ 成功释放页 " + x + ANSI_RESET);
        }
    }

    // ==================== 实验3: 管道通信模拟器 ====================

    /**
     * 实验3: 管道通信模拟器
     * 使用真实的 Pipe 对象演示进程间通信
     */
    private static void labPipeCommunication(Kernel kernel) {
        kernel.getConsole().println("\n" + ANSI_BOLD + ANSI_BLUE + "=== 实验3: 管道通信模拟器 ===" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "实验目标: 理解管道(Pipe)作为进程间通信(IPC)机制" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "核心概念: 缓冲区、读写指针、EOF、阻塞" + ANSI_RESET);

        // 创建真实的 Pipe 对象
        Pipe pipe = new Pipe();

        kernel.getConsole().println(ANSI_CYAN + "\n已创建管道对象，初始状态:" + ANSI_RESET);
        printPipeStatus(kernel, pipe);

        boolean writeEndClosed = false;
        boolean readEndClosed = false;

        while (true) {
            kernel.getConsole().println("\n请选择操作：");
            kernel.getConsole().println(ANSI_GREEN + "  1. 写入数据" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  2. 读取数据" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  3. 查看缓冲区状态" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  4. 关闭写端" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  5. 关闭读端" + ANSI_RESET);
            kernel.getConsole().println(ANSI_RED + "  6. 返回" + ANSI_RESET);
            kernel.getConsole().print("\n请输入选项 (1-6): ");

            try {
                String input = reader.readLine();
                if (input == null) break;
                int choice = Integer.parseInt(input.trim());

                switch (choice) {
                    case 1:
                        if (writeEndClosed) {
                            kernel.getConsole().println(ANSI_RED + "✗ 写端已关闭，无法写入" + ANSI_RESET);
                        } else {
                            kernel.getConsole().print("请输入要写入的数据: ");
                            String data = reader.readLine();
                            if (data != null) {
                                byte[] dataBytes = data.getBytes();
                                pipe.write(dataBytes, dataBytes.length);
                                kernel.getConsole().println(ANSI_GREEN + "✓ 成功写入 " + dataBytes.length + " 字节" + ANSI_RESET);
                                kernel.getConsole().println(ANSI_YELLOW + "  解释: 数据被写入管道的缓冲区，等待读取" + ANSI_RESET);
                            }
                        }
                        break;
                    case 2:
                        if (readEndClosed) {
                            kernel.getConsole().println(ANSI_RED + "✗ 读端已关闭，无法读取" + ANSI_RESET);
                        } else {
                            byte[] buffer = new byte[1024];
                            int bytesRead = pipe.read(buffer, buffer.length);
                            if (bytesRead > 0) {
                                String readData = new String(buffer, 0, bytesRead);
                                kernel.getConsole().println(ANSI_GREEN + "✓ 成功读取 " + bytesRead + " 字节: \"" + readData + "\"" + ANSI_RESET);
                                kernel.getConsole().println(ANSI_YELLOW + "  解释: 数据从缓冲区读出，遵循 FIFO 顺序" + ANSI_RESET);
                            } else if (bytesRead == 0) {
                                kernel.getConsole().println(ANSI_YELLOW + "⚠ 读取到 EOF (返回0)，说明写端已关闭且缓冲区为空" + ANSI_RESET);
                            } else {
                                kernel.getConsole().println(ANSI_RED + "✗ 读取失败" + ANSI_RESET);
                            }
                        }
                        break;
                    case 3:
                        printPipeStatus(kernel, pipe);
                        break;
                    case 4:
                        if (!writeEndClosed) {
                            writeEndClosed = true;
                            kernel.getConsole().println(ANSI_GREEN + "✓ 写端已关闭" + ANSI_RESET);
                            kernel.getConsole().println(ANSI_YELLOW + "  解释: 关闭写端后，读取端在读完缓冲区数据后将收到 EOF" + ANSI_RESET);
                        } else {
                            kernel.getConsole().println(ANSI_YELLOW + "⚠ 写端已经关闭" + ANSI_RESET);
                        }
                        break;
                    case 5:
                        if (!readEndClosed) {
                            readEndClosed = true;
                            kernel.getConsole().println(ANSI_GREEN + "✓ 读端已关闭" + ANSI_RESET);
                            kernel.getConsole().println(ANSI_YELLOW + "  解释: 关闭读端后，写入端再写入将收到 SIGPIPE 信号" + ANSI_RESET);
                        } else {
                            kernel.getConsole().println(ANSI_YELLOW + "⚠ 读端已经关闭" + ANSI_RESET);
                        }
                        break;
                    case 6:
                        kernel.getConsole().println(ANSI_YELLOW + "返回主菜单..." + ANSI_RESET);
                        return;
                    default:
                        kernel.getConsole().println(ANSI_RED + "无效选项" + ANSI_RESET);
                }
            } catch (Exception e) {
                kernel.getConsole().println(ANSI_RED + "输入错误: " + e.getMessage() + ANSI_RESET);
            }
        }
    }

    private static void printPipeStatus(Kernel kernel, Pipe pipe) {
        kernel.getConsole().println(ANSI_CYAN + "\n管道状态:" + ANSI_RESET);
        // 由于 Pipe 类的具体实现可能不同，这里展示通用信息
        kernel.getConsole().println("  管道对象: " + pipe.toString());
        kernel.getConsole().println(ANSI_YELLOW + "  提示: 管道内部有一个缓冲区，数据遵循 FIFO (先进先出) 顺序" + ANSI_RESET);
    }

    // ==================== 实验4: 信号处理模拟器 ====================

    /**
     * 实验4: 信号处理模拟器
     * 模拟信号的发送、屏蔽和处理
     */
    private static void labSignalHandling(Kernel kernel) {
        kernel.getConsole().println("\n" + ANSI_BOLD + ANSI_BLUE + "=== 实验4: 信号处理模拟器 ===" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "实验目标: 理解信号机制、信号位图、信号处理器" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "核心概念: 信号位图、信号屏蔽、信号处理器、SIGKILL/SIGSTOP特殊性" + ANSI_RESET);

        // 用 int 模拟信号位图（32位）
        int signalBitmap = 0;
        // 用 int 模拟信号屏蔽位图
        int signalMask = 0;
        // 用 String[] 模拟信号处理器
        String[] signalHandlers = new String[32];
        Arrays.fill(signalHandlers, "DEFAULT");

        while (true) {
            kernel.getConsole().println("\n" + ANSI_CYAN + "--- 当前信号状态 ---" + ANSI_RESET);
            printSignalStatus(kernel, signalBitmap, signalMask, signalHandlers);

            kernel.getConsole().println("\n请选择操作：");
            kernel.getConsole().println(ANSI_GREEN + "  1. 发送信号" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  2. 设置处理器" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  3. 屏蔽信号" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  4. 处理待处理信号" + ANSI_RESET);
            kernel.getConsole().println(ANSI_GREEN + "  5. 查看状态" + ANSI_RESET);
            kernel.getConsole().println(ANSI_RED + "  6. 返回" + ANSI_RESET);
            kernel.getConsole().print("\n请输入选项 (1-6): ");

            try {
                String input = reader.readLine();
                if (input == null) break;
                int choice = Integer.parseInt(input.trim());

                switch (choice) {
                    case 1:
                        kernel.getConsole().print("请输入信号编号 (1-31): ");
                        String sigInput = reader.readLine();
                        if (sigInput != null) {
                            int sigNum = Integer.parseInt(sigInput.trim());
                            sendSignal(kernel, signalBitmap, signalMask, sigNum);
                        }
                        break;
                    case 2:
                        kernel.getConsole().print("请输入信号编号 (1-31): ");
                        String sigInput2 = reader.readLine();
                        if (sigInput2 != null) {
                            int sigNum = Integer.parseInt(sigInput2.trim());
                            setSignalHandler(kernel, signalHandlers, sigNum);
                        }
                        break;
                    case 3:
                        kernel.getConsole().print("请输入要屏蔽的信号编号 (1-31): ");
                        String sigInput3 = reader.readLine();
                        if (sigInput3 != null) {
                            int sigNum = Integer.parseInt(sigInput3.trim());
                            maskSignal(kernel, signalMask, sigNum);
                        }
                        break;
                    case 4:
                        processPendingSignals(kernel, signalBitmap, signalMask, signalHandlers);
                        break;
                    case 5:
                        printSignalStatus(kernel, signalBitmap, signalMask, signalHandlers);
                        break;
                    case 6:
                        kernel.getConsole().println(ANSI_YELLOW + "返回主菜单..." + ANSI_RESET);
                        return;
                    default:
                        kernel.getConsole().println(ANSI_RED + "无效选项" + ANSI_RESET);
                }
            } catch (Exception e) {
                kernel.getConsole().println(ANSI_RED + "输入错误: " + e.getMessage() + ANSI_RESET);
            }
        }
    }

    private static void printSignalStatus(Kernel kernel, int signalBitmap, int signalMask, String[] signalHandlers) {
        kernel.getConsole().println("\n信号位图 (32位二进制):");
        kernel.getConsole().println("  " + String.format("%32s", Integer.toBinaryString(signalBitmap)).replace(' ', '0'));
        kernel.getConsole().println("\n信号屏蔽位图:");
        kernel.getConsole().println("  " + String.format("%32s", Integer.toBinaryString(signalMask)).replace(' ', '0'));
        kernel.getConsole().println("\n信号处理器设置:");
        for (int i = 1; i <= 31; i++) {
            if (signalHandlers[i] != null && !signalHandlers[i].equals("DEFAULT")) {
                kernel.getConsole().println("  Signal " + i + ": " + signalHandlers[i]);
            }
        }
    }

    private static void sendSignal(Kernel kernel, int signalBitmap, int signalMask, int sigNum) {
        if (sigNum < 1 || sigNum > 31) {
            kernel.getConsole().println(ANSI_RED + "✗ 信号编号超出范围 (1-31)" + ANSI_RESET);
            return;
        }
        if (sigNum == 9 || sigNum == 19) {
            kernel.getConsole().println(ANSI_YELLOW + "⚠ 注意: SIGKILL(9) 和 SIGSTOP(19) 不可捕获/屏蔽" + ANSI_RESET);
        }
        // 设置对应位
        signalBitmap |= (1 << sigNum);
        kernel.getConsole().println(ANSI_GREEN + "✓ 信号 " + sigNum + " 已发送（位图中对应位置1）" + ANSI_RESET);
    }

    private static void setSignalHandler(Kernel kernel, String[] signalHandlers, int sigNum) {
        if (sigNum < 1 || sigNum > 31) {
            kernel.getConsole().println(ANSI_RED + "✗ 信号编号超出范围 (1-31)" + ANSI_RESET);
            return;
        }
        if (sigNum == 9 || sigNum == 19) {
            kernel.getConsole().println(ANSI_RED + "✗ SIGKILL(9) 和 SIGSTOP(19) 不可捕获" + ANSI_RESET);
            return;
        }
        kernel.getConsole().println("选择处理器类型:");
        kernel.getConsole().println("  1. DEFAULT (默认处理)");
        kernel.getConsole().println("  2. IGNORE (忽略)");
        kernel.getConsole().println("  3. CUSTOM (自定义)");
        kernel.getConsole().print("请输入选项 (1-3): ");
        try {
            String input = reader.readLine();
            if (input != null) {
                int choice = Integer.parseInt(input.trim());
                switch (choice) {
                    case 1:
                        signalHandlers[sigNum] = "DEFAULT";
                        break;
                    case 2:
                        signalHandlers[sigNum] = "IGNORE";
                        break;
                    case 3:
                        signalHandlers[sigNum] = "CUSTOM";
                        break;
                    default:
                        kernel.getConsole().println(ANSI_RED + "无效选项" + ANSI_RESET);
                        return;
                }
                kernel.getConsole().println(ANSI_GREEN + "✓ 信号 " + sigNum + " 的处理器设置为 " + signalHandlers[sigNum] + ANSI_RESET);
            }
        } catch (Exception e) {
            kernel.getConsole().println(ANSI_RED + "输入错误" + ANSI_RESET);
        }
    }

    private static void maskSignal(Kernel kernel, int signalMask, int sigNum) {
        if (sigNum < 1 || sigNum > 31) {
            kernel.getConsole().println(ANSI_RED + "✗ 信号编号超出范围 (1-31)" + ANSI_RESET);
            return;
        }
        if (sigNum == 9 || sigNum == 19) {
            kernel.getConsole().println(ANSI_RED + "✗ SIGKILL(9) 和 SIGSTOP(19) 不可屏蔽" + ANSI_RESET);
            return;
        }
        signalMask |= (1 << sigNum);
        kernel.getConsole().println(ANSI_GREEN + "✓ 信号 " + sigNum + " 已屏蔽（屏蔽位图中对应位置1）" + ANSI_RESET);
    }

    private static void processPendingSignals(Kernel kernel, int signalBitmap, int signalMask, String[] signalHandlers) {
        kernel.getConsole().println(ANSI_CYAN + "\n开始处理待处理信号..." + ANSI_RESET);
        boolean hasPending = false;

        for (int i = 1; i <= 31; i++) {
            // 检查信号是否在位图中
            if ((signalBitmap & (1 << i)) != 0) {
                hasPending = true;
                // 检查是否被屏蔽
                if ((signalMask & (1 << i)) != 0) {
                    kernel.getConsole().println(ANSI_YELLOW + "  信号 " + i + " 被屏蔽，跳过" + ANSI_RESET);
                    continue;
                }
                // 根据处理器执行动作
                String handler = signalHandlers[i];
                if (handler == null) handler = "DEFAULT";

                switch (handler) {
                    case "DEFAULT":
                        kernel.getConsole().println(ANSI_GREEN + "  信号 " + i + ": 执行默认处理" + ANSI_RESET);
                        break;
                    case "IGNORE":
                        kernel.getConsole().println(ANSI_GREEN + "  信号 " + i + ": 忽略" + ANSI_RESET);
                        break;
                    case "CUSTOM":
                        kernel.getConsole().println(ANSI_GREEN + "  信号 " + i + ": 执行自定义处理器" + ANSI_RESET);
                        break;
                }
                // 清除该信号位
                signalBitmap &= ~(1 << i);
            }
        }

        if (!hasPending) {
            kernel.getConsole().println(ANSI_YELLOW + "  没有待处理的信号" + ANSI_RESET);
        }
    }

    // ==================== 实验5: Fork过程模拟器 ====================

    /**
     * 实验5: Fork 过程模拟器
     * 逐步展示 fork 系统调用的每个阶段
     */
    private static void labForkProcess(Kernel kernel) {
        kernel.getConsole().println("\n" + ANSI_BOLD + ANSI_BLUE + "=== 实验5: Fork过程模拟器 ===" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "实验目标: 理解 fork() 系统调用的内部实现过程" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "核心概念: PCB复制、地址空间复制(COW)、返回值设置" + ANSI_RESET);

        // 展示父进程 PCB
        kernel.getConsole().println(ANSI_CYAN + "\n父进程 PCB 信息:" + ANSI_RESET);
        kernel.getConsole().println("  PID: 1");
        kernel.getConsole().println("  PPID: 0");
        kernel.getConsole().println("  State: RUNNING");
        kernel.getConsole().println("  Priority: 15");
        kernel.getConsole().println("  Counter: 15");

        kernel.getConsole().println(ANSI_BOLD + "\n按回车开始 fork 过程..." + ANSI_RESET);
        try {
            reader.readLine();
        } catch (Exception e) {
            return;
        }

        // Step 1: 分配新 PID
        kernel.getConsole().println(ANSI_GREEN + "\nStep 1: 分配新 PID" + ANSI_RESET);
        kernel.getConsole().println("  >>> 从 PID 表中分配一个新的 PID: 2");
        kernel.getConsole().println(ANSI_YELLOW + "  解释: 每个进程必须有唯一的 PID，内核维护一个 PID 表来跟踪已分配的 PID" + ANSI_RESET);

        waitForEnter(kernel);

        // Step 2: 复制 PCB
        kernel.getConsole().println(ANSI_GREEN + "\nStep 2: 复制 PCB (进程控制块)" + ANSI_RESET);
        kernel.getConsole().println("  >>> 为子进程创建新的 PCB，大部分字段与父进程相同");
        kernel.getConsole().println("\n  子进程 PCB 信息:");
        kernel.getConsole().println("    PID: 2 (新分配)");
        kernel.getConsole().println("    PPID: 1 (指向父进程)");
        kernel.getConsole().println("    State: RUNNING (暂时)");
        kernel.getConsole().println("    Priority: 15 (继承自父进程)");
        kernel.getConsole().println("    Counter: 15 (继承自父进程)");
        kernel.getConsole().println(ANSI_YELLOW + "  解释: PCB 包含进程的所有元数据，fork 需要复制这些信息" + ANSI_RESET);

        waitForEnter(kernel);

        // Step 3: 复制地址空间 (COW)
        kernel.getConsole().println(ANSI_GREEN + "\nStep 3: 复制地址空间 (Copy-On-Write)" + ANSI_RESET);
        kernel.getConsole().println("  >>> 子进程共享父进程的物理内存页面，但标记为只读");
        kernel.getConsole().println("  >>> 当任一进程尝试写入时，才真正复制该页面");
        kernel.getConsole().println(ANSI_YELLOW + "  解释: COW 优化了 fork 的性能，避免了不必要的内存复制" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "  只有当父子进程中有一个执行 exec() 时，COW 的优势才体现出来" + ANSI_RESET);

        waitForEnter(kernel);

        // Step 4: 设置返回值
        kernel.getConsole().println(ANSI_GREEN + "\nStep 4: 设置返回值" + ANSI_RESET);
        kernel.getConsole().println("  >>> 父进程中 fork() 返回子进程的 PID: 2");
        kernel.getConsole().println("  >>> 子进程中 fork() 返回: 0");
        kernel.getConsole().println(ANSI_YELLOW + "  解释: 通过返回值区分父子进程" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "  典型用法: if (pid == 0) { /* 子进程代码 */ } else { /* 父进程代码 */ }" + ANSI_RESET);

        waitForEnter(kernel);

        // Step 5: 加入就绪队列
        kernel.getConsole().println(ANSI_GREEN + "\nStep 5: 加入就绪队列" + ANSI_RESET);
        kernel.getConsole().println("  >>> 将子进程的状态设置为 READY");
        kernel.getConsole().println("  >>> 子进程被加入调度器的就绪队列，等待被调度执行");
        kernel.getConsole().println(ANSI_YELLOW + "  解释: 子进程现在可以独立于父进程被调度执行" + ANSI_RESET);

        waitForEnter(kernel);

        // 对比父子进程 PCB
        kernel.getConsole().println(ANSI_CYAN + "\n=== 父子进程 PCB 对比 ===" + ANSI_RESET);
        kernel.getConsole().println(String.format("%-15s %-15s %-15s", "Field", "Parent", "Child"));
        kernel.getConsole().println("------------------------------------------------");
        kernel.getConsole().println(String.format("%-15s %-15s %-15s", "PID", "1", "2"));
        kernel.getConsole().println(String.format("%-15s %-15s %-15s", "PPID", "0", "1"));
        kernel.getConsole().println(String.format("%-15s %-15s %-15s", "State", "RUNNING", "READY"));
        kernel.getConsole().println(String.format("%-15s %-15s %-15s", "Priority", "15", "15"));
        kernel.getConsole().println(String.format("%-15s %-15s %-15s", "Counter", "15", "15"));
        kernel.getConsole().println(String.format("%-15s %-15s %-15s", "Address Space", "Shared (COW)", "Shared (COW)"));

        // 提问学生
        kernel.getConsole().println(ANSI_BOLD + "\n理解性问题:" + ANSI_RESET);
        kernel.getConsole().println(ANSI_GREEN + "1. 为什么 fork() 在父进程中返回子进程 PID，在子进程中返回 0？" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "   提示: 这样设计可以让代码通过判断返回值来区分父子进程" + ANSI_RESET);

        kernel.getConsole().println(ANSI_GREEN + "\n2. COW (Copy-On-Write) 的优势是什么？" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "   提示: 考虑 fork() 后立即 exec() 的场景" + ANSI_RESET);

        kernel.getConsole().println(ANSI_GREEN + "\n3. 如果父进程在 fork() 后修改了一个变量，子进程能看到这个修改吗？" + ANSI_RESET);
        kernel.getConsole().println(ANSI_YELLOW + "   提示: 考虑 COW 机制触发时的行为" + ANSI_RESET);

        kernel.getConsole().println(ANSI_GREEN + "\n✓ Fork过程实验完成！" + ANSI_RESET);
    }

    private static void waitForEnter(Kernel kernel) {
        kernel.getConsole().print(ANSI_CYAN + "\n按回车继续..." + ANSI_RESET);
        try {
            reader.readLine();
        } catch (Exception e) {
            // Ignore
        }
    }
}
