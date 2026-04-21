package jinux.fs;

import jinux.include.FileSystemConstants;

/**
 * 目录项数据类
 * 封装目录项的解析和序列化操作
 */
public class DirectoryEntry {
    private final int inodeNumber;
    private final String name;
    
    public DirectoryEntry(int inodeNumber, String name) {
        this.inodeNumber = inodeNumber;
        this.name = name;
    }
    
    public int getInodeNumber() {
        return inodeNumber;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * 从目录数据中解析指定索引位置的目录项
     * 
     * @param dirData 目录数据字节数组
     * @param entryIndex 目录项索引
     * @return 解析后的 DirectoryEntry，如果该位置为空则返回 null
     */
    public static DirectoryEntry parse(byte[] dirData, int entryIndex) {
        int offset = entryIndex * FileSystemConstants.DIR_ENTRY_SIZE;
        int ino = ByteUtils.readLittleEndianShort(dirData, offset);
        if (ino == 0) {
            return null;
        }
        
        int nameLen = 0;
        while (nameLen < FileSystemConstants.MAX_FILE_NAME_LENGTH && dirData[offset + 2 + nameLen] != 0) {
            nameLen++;
        }
        String name = new String(dirData, offset + 2, nameLen);
        return new DirectoryEntry(ino, name);
    }
    
    /**
     * 将当前目录项写入到指定的目录数据中
     * 
     * @param dirData 目录数据字节数组
     * @param entryIndex 目录项索引
     */
    public void writeTo(byte[] dirData, int entryIndex) {
        int offset = entryIndex * FileSystemConstants.DIR_ENTRY_SIZE;
        ByteUtils.writeLittleEndianShort(dirData, offset, inodeNumber);
        byte[] nameBytes = name.getBytes();
        int nameLen = Math.min(nameBytes.length, FileSystemConstants.MAX_FILE_NAME_LENGTH);
        System.arraycopy(nameBytes, 0, dirData, offset + 2, nameLen);
        for (int i = nameLen; i < FileSystemConstants.MAX_FILE_NAME_LENGTH; i++) {
            dirData[offset + 2 + i] = 0;
        }
    }
    
    /**
     * 清空指定索引位置的目录项
     * 
     * @param dirData 目录数据字节数组
     * @param entryIndex 目录项索引
     */
    public static void clear(byte[] dirData, int entryIndex) {
        int offset = entryIndex * FileSystemConstants.DIR_ENTRY_SIZE;
        for (int i = 0; i < FileSystemConstants.DIR_ENTRY_SIZE; i++) {
            dirData[offset + i] = 0;
        }
    }
}
