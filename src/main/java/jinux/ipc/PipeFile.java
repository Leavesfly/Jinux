package jinux.ipc;

/**
 * 管道文件描述符包装
 * 将管道包装成可以被文件描述符表使用的对象
 * 
 * @author Jinux Project
 */
public class PipeFile {
    
    /** 管道对象 */
    private final Pipe pipe;
    
    /** 是否为读端 */
    private final boolean isReadEnd;
    
    /**
     * 构造管道文件描述符
     * 
     * @param pipe 管道对象
     * @param isReadEnd 是否为读端
     */
    public PipeFile(Pipe pipe, boolean isReadEnd) {
        this.pipe = pipe;
        this.isReadEnd = isReadEnd;
    }
    
    /**
     * 读取数据
     */
    public int read(byte[] buf, int count) {
        if (!isReadEnd) {
            return -9; // EBADF
        }
        return pipe.read(buf, count);
    }
    
    /**
     * 写入数据
     */
    public int write(byte[] buf, int count) {
        if (isReadEnd) {
            return -9; // EBADF
        }
        return pipe.write(buf, count);
    }
    
    /**
     * 关闭
     */
    public void close() {
        if (isReadEnd) {
            pipe.closeRead();
        } else {
            pipe.closeWrite();
        }
    }
    
    public Pipe getPipe() {
        return pipe;
    }
    
    public boolean isReadEnd() {
        return isReadEnd;
    }
    
    @Override
    public String toString() {
        return String.format("PipeFile[%s, %s]", 
            isReadEnd ? "READ" : "WRITE", pipe);
    }
}
