package jinux.shell.commands;

import jinux.demo.QuizSystem;
import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * quiz 命令 - 交互式知识问答
 */
public class QuizCommand implements Command {
    
    @Override
    public String getName() {
        return "quiz";
    }
    
    @Override
    public String getDescription() {
        return "Interactive knowledge quiz (125 questions)";
    }
    
    @Override
    public String getUsage() {
        return "quiz";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        QuizSystem.runQuiz(context.getKernel());
    }
}
