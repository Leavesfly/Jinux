package jinux.ipc;

import jinux.include.Const;

/**
 * 管道（Pipe）
 * 对应 Linux 0.01 中的管道机制
 * 
 * 用于进程间通信的单向数据流
 * 
 * @author Jinux Project
 */
public class Pipe {
    
    /** 管道缓冲区大小 */
    public static final int PIPE_BUF = 4096;
    
    /** 管道缓冲区 */
    private final byte[] buffer;
    
    /** 读位置 */
    private int readPos;
    
    /** 写位置 */
    private int writePos;
    
    /** 数据大小 */
    private int dataSize;
    
    /** 读端引用计数 */
    private int readers;
    
    /** 写端引用计数 */
    private int writers;
    
    /** 管道是否已关闭 */
    private boolean closed;
    
    /** 等待读的进程数 */
    private int waitingReaders;
    
    /** 等待写的进程数 */
    private int waitingWriters;
    
    /**
     * 构造管道
     */
    public Pipe() {
        this.buffer = new byte[PIPE_BUF];
        this.readPos = 0;
        this.writePos = 0;
        this.dataSize = 0;
        this.readers = 1;
        this.writers = 1;
        this.closed = false;
        this.waitingReaders = 0;
        this.waitingWriters = 0;
    }
    
    /**
     * 从管道读取数据
     * 
     * @param buf 目标缓冲区
     * @param count 要读取的字节数
     * @return 实际读取的字节数，-1 表示管道已关闭
     */
    public synchronized int read(byte[] buf, int count) {
        if (count <= 0 || buf == null) {
            return -Const.EINVAL;
        }
        
        // 等待数据可用
        while (dataSize == 0) {
            // 如果没有写端，返回 EOF
            if (writers == 0) {
                return 0;
            }
            
            // 等待数据
            waitingReaders++;
            try {
                wait();
            } catch (InterruptedException e) {
                waitingReaders--;
                return -Const.EINTR;
            }
            waitingReaders--;
        }
        
        // 计算实际读取大小
        int toRead = Math.min(count, dataSize);
        int bytesRead = 0;
        
        // 从循环缓冲区读取
        while (bytesRead < toRead) {
            int available = Math.min(toRead - bytesRead, PIPE_BUF - readPos);
            System.arraycopy(buffer, readPos, buf, bytesRead, available);
            bytesRead += available;
            readPos = (readPos + available) % PIPE_BUF;
        }
        
        dataSize -= bytesRead;
        
        // 唤醒等待写的进程
        if (waitingWriters > 0) {
            notifyAll();
        }
        
        return bytesRead;
    }
    
    /**
     * 向管道写入数据
     * 
     * @param buf 源缓冲区
     * @param count 要写入的字节数
     * @return 实际写入的字节数，-1 表示错误
     */
    public synchronized int write(byte[] buf, int count) {
        if (count <= 0 || buf == null) {
            return -Const.EINVAL;
        }
        
        // 检查是否有读端
        if (readers == 0) {
            // 没有读端，发送 SIGPIPE 信号
            return -Const.EPIPE;
        }
        
        int bytesWritten = 0;
        
        while (bytesWritten < count) {
            // 等待空间可用
            while (dataSize >= PIPE_BUF) {
                if (readers == 0) {
                    return -Const.EPIPE;
                }
                
                waitingWriters++;
                try {
                    wait();
                } catch (InterruptedException e) {
                    waitingWriters--;
                    return bytesWritten > 0 ? bytesWritten : -Const.EINTR;
                }
                waitingWriters--;
            }
            
            // 写入数据
            int toWrite = Math.min(count - bytesWritten, PIPE_BUF - dataSize);
            int written = 0;
            
            while (written < toWrite) {
                int available = Math.min(toWrite - written, PIPE_BUF - writePos);
                System.arraycopy(buf, bytesWritten + written, buffer, writePos, available);
                written += available;
                writePos = (writePos + available) % PIPE_BUF;
            }
            
            bytesWritten += written;
            dataSize += written;
            
            // 唤醒等待读的进程
            if (waitingReaders > 0) {
                notifyAll();
            }
        }
        
        return bytesWritten;
    }
    
    /**
     * 关闭读端
     */
    public synchronized void closeRead() {
        if (readers > 0) {
            readers--;
        }
        
        if (readers == 0 && writers == 0) {
            closed = true;
        }
        
        // 唤醒所有等待的写进程
        notifyAll();
    }
    
    /**
     * 关闭写端
     */
    public synchronized void closeWrite() {
        if (writers > 0) {
            writers--;
        }
        
        if (readers == 0 && writers == 0) {
            closed = true;
        }
        
        // 唤醒所有等待的读进程
        notifyAll();
    }
    
    /**
     * 增加读端引用
     */
    public synchronized void incrementReaders() {
        readers++;
    }
    
    /**
     * 增加写端引用
     */
    public synchronized void incrementWriters() {
        writers++;
    }
    
    /**
     * 检查管道是否为空
     */
    public synchronized boolean isEmpty() {
        return dataSize == 0;
    }
    
    /**
     * 检查管道是否已满
     */
    public synchronized boolean isFull() {
        return dataSize >= PIPE_BUF;
    }
    
    /**
     * 获取可读字节数
     */
    public synchronized int available() {
        return dataSize;
    }
    
    @Override
    public String toString() {
        return String.format("Pipe[size=%d/%d, readers=%d, writers=%d, closed=%b]",
            dataSize, PIPE_BUF, readers, writers, closed);
    }
    
    // ==================== Getters ====================
    
    public int getReaders() {
        return readers;
    }
    
    public int getWriters() {
        return writers;
    }
    
    public boolean isClosed() {
        return closed;
    }
}
