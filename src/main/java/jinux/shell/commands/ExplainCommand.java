package jinux.shell.commands;

import jinux.demo.ConceptExplainer;
import jinux.shell.Command;
import jinux.shell.ShellContext;

/**
 * explain 命令 - 概念解释器
 */
public class ExplainCommand implements Command {
    
    @Override
    public String getName() {
        return "explain";
    }
    
    @Override
    public String getDescription() {
        return "Explain OS concepts with analogies";
    }
    
    @Override
    public String getUsage() {
        return "explain [topic]";
    }
    
    @Override
    public void execute(String[] args, ShellContext context) {
        if (args.length == 0) {
            ConceptExplainer.showTopicList(context.getKernel());
        } else {
            ConceptExplainer.explain(context.getKernel(), args[0]);
        }
    }
}
