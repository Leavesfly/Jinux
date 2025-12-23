package jinux.ipc;

import jinux.include.Const;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Pipe类的单元测试
 */
public class PipeTest {
    
    private Pipe pipe;
    
    @BeforeEach
    void setUp() {
        pipe = new Pipe();
    }
    
    @Test
    void testPipeCreation() {
        assertNotNull(pipe);
        assertEquals(1, pipe.getReaders());
        assertEquals(1, pipe.getWriters());
        assertFalse(pipe.isClosed());
        assertTrue(pipe.isEmpty());
        assertFalse(pipe.isFull());
        assertEquals(0, pipe.available());
    }
    
    @Test
    void testWriteAndRead() {
        byte[] writeBuf = "Hello, Pipe!".getBytes();
        int written = pipe.write(writeBuf, writeBuf.length);
        
        assertEquals(writeBuf.length, written);
        assertFalse(pipe.isEmpty());
        assertEquals(writeBuf.length, pipe.available());
        
        byte[] readBuf = new byte[writeBuf.length];
        int read = pipe.read(readBuf, readBuf.length);
        
        assertEquals(writeBuf.length, read);
        assertArrayEquals(writeBuf, readBuf);
        assertTrue(pipe.isEmpty());
    }
    
    @Test
    void testPartialRead() {
        byte[] writeBuf = "Hello, World!".getBytes();
        pipe.write(writeBuf, writeBuf.length);
        
        byte[] readBuf = new byte[5];
        int read = pipe.read(readBuf, readBuf.length);
        
        assertEquals(5, read);
        assertEquals("Hello", new String(readBuf));
        assertEquals(writeBuf.length - 5, pipe.available());
    }
    
    @Test
    void testPartialWrite() {
        byte[] writeBuf = new byte[Pipe.PIPE_BUF + 100];
        for (int i = 0; i < writeBuf.length; i++) {
            writeBuf[i] = (byte) i;
        }
        
        // 写入应该被限制在PIPE_BUF大小
        int written = pipe.write(writeBuf, writeBuf.length);
        assertTrue(written > 0);
        assertTrue(written <= Pipe.PIPE_BUF);
    }
    
    @Test
    void testReadEmptyPipe() {
        byte[] readBuf = new byte[10];
        
        // 在没有写端关闭的情况下，应该等待
        // 但为了测试，我们先关闭写端
        pipe.closeWrite();
        
        int read = pipe.read(readBuf, readBuf.length);
        assertEquals(0, read); // EOF
    }
    
    @Test
    void testWriteToPipeWithoutReader() {
        pipe.closeRead();
        
        byte[] writeBuf = "test".getBytes();
        int written = pipe.write(writeBuf, writeBuf.length);
        
        assertEquals(-Const.EPIPE, written);
    }
    
    @Test
    void testCloseRead() {
        pipe.closeRead();
        assertEquals(0, pipe.getReaders());
        assertTrue(pipe.isClosed());
    }
    
    @Test
    void testCloseWrite() {
        pipe.closeWrite();
        assertEquals(0, pipe.getWriters());
        assertTrue(pipe.isClosed());
    }
    
    @Test
    void testCloseBothEnds() {
        pipe.closeRead();
        assertFalse(pipe.isClosed()); // 还有写端
        
        pipe.closeWrite();
        assertTrue(pipe.isClosed()); // 两端都关闭了
    }
    
    @Test
    void testIncrementReaders() {
        int initialReaders = pipe.getReaders();
        pipe.incrementReaders();
        
        assertEquals(initialReaders + 1, pipe.getReaders());
    }
    
    @Test
    void testIncrementWriters() {
        int initialWriters = pipe.getWriters();
        pipe.incrementWriters();
        
        assertEquals(initialWriters + 1, pipe.getWriters());
    }
    
    @Test
    void testCircularBuffer() {
        // 测试循环缓冲区：写入超过缓冲区大小的数据
        byte[] data1 = new byte[Pipe.PIPE_BUF / 2];
        byte[] data2 = new byte[Pipe.PIPE_BUF / 2];
        
        for (int i = 0; i < data1.length; i++) {
            data1[i] = (byte) i;
        }
        for (int i = 0; i < data2.length; i++) {
            data2[i] = (byte) (i + 100);
        }
        
        pipe.write(data1, data1.length);
        pipe.write(data2, data2.length);
        
        byte[] readBuf1 = new byte[data1.length];
        int read1 = pipe.read(readBuf1, readBuf1.length);
        assertEquals(data1.length, read1);
        assertArrayEquals(data1, readBuf1);
        
        byte[] readBuf2 = new byte[data2.length];
        int read2 = pipe.read(readBuf2, readBuf2.length);
        assertEquals(data2.length, read2);
        assertArrayEquals(data2, readBuf2);
    }
    
    @Test
    void testReadInvalidParameters() {
        byte[] buf = new byte[10];
        
        // 无效的count
        int result = pipe.read(buf, -1);
        assertEquals(-Const.EINVAL, result);
        
        // null缓冲区
        result = pipe.read(null, 10);
        assertEquals(-Const.EINVAL, result);
    }
    
    @Test
    void testWriteInvalidParameters() {
        byte[] buf = new byte[10];
        
        // 无效的count
        int result = pipe.write(buf, -1);
        assertEquals(-Const.EINVAL, result);
        
        // null缓冲区
        result = pipe.write(null, 10);
        assertEquals(-Const.EINVAL, result);
    }
    
    @Test
    void testIsFull() {
        // 填满管道
        byte[] data = new byte[Pipe.PIPE_BUF];
        pipe.write(data, data.length);
        
        assertTrue(pipe.isFull());
        assertEquals(Pipe.PIPE_BUF, pipe.available());
    }
    
    @Test
    void testToString() {
        String str = pipe.toString();
        assertNotNull(str);
        assertTrue(str.contains("Pipe"));
    }
    
    @Test
    void testMultipleWritesAndReads() {
        // 多次写入和读取
        for (int i = 0; i < 10; i++) {
            byte[] writeBuf = ("Message " + i).getBytes();
            pipe.write(writeBuf, writeBuf.length);
            
            byte[] readBuf = new byte[writeBuf.length];
            int read = pipe.read(readBuf, readBuf.length);
            
            assertEquals(writeBuf.length, read);
            assertArrayEquals(writeBuf, readBuf);
        }
    }
}

