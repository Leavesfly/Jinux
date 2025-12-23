package jinux.mm;

import jinux.include.Const;
import java.util.HashMap;
import java.util.Map;

/**
 * 页表
 * 对应 Linux 0.01 中的页表机制
 * 
 * 实现虚拟地址到物理地址的映射
 * 简化实现：使用 HashMap 存储映射关系，而非真实的页表结构
 * 
 * @author Jinux Project
 */
public class PageTable {
    
    /** 虚拟页号 -> 物理页号的映射 */
    private final Map<Integer, Integer> mappings;
    
    /** 页面权限标志（简化：每个虚拟页一个标志） */
    private final Map<Integer, Integer> flags;
    
    // 页面标志位
    public static final int PAGE_PRESENT = 0x001;  // 页面存在
    public static final int PAGE_RW = 0x002;        // 可读写
    public static final int PAGE_USER = 0x004;      // 用户态可访问
    public static final int PAGE_COW = 0x008;        // 写时复制标记
    
    /**
     * 构造空页表
     */
    public PageTable() {
        this.mappings = new HashMap<>();
        this.flags = new HashMap<>();
    }
    
    /**
     * 映射一个虚拟页到物理页
     * 
     * @param vpage 虚拟页号
     * @param ppage 物理页号
     * @param pageFlags 页面标志
     */
    public void map(int vpage, int ppage, int pageFlags) {
        mappings.put(vpage, ppage);
        flags.put(vpage, pageFlags | PAGE_PRESENT);
    }
    
    /**
     * 取消映射
     * 
     * @param vpage 虚拟页号
     */
    public void unmap(int vpage) {
        mappings.remove(vpage);
        flags.remove(vpage);
    }
    
    /**
     * 获取虚拟页对应的物理页
     * 
     * @param vpage 虚拟页号
     * @return 物理页号，如果不存在返回 -1
     */
    public int getPhysicalPage(int vpage) {
        Integer ppage = mappings.get(vpage);
        return ppage != null ? ppage : -1;
    }
    
    /**
     * 检查虚拟页是否已映射
     * 
     * @param vpage 虚拟页号
     * @return 是否已映射
     */
    public boolean isMapped(int vpage) {
        return mappings.containsKey(vpage);
    }
    
    /**
     * 虚拟地址转物理地址
     * 
     * @param vaddr 虚拟地址
     * @return 物理地址，如果未映射返回 -1
     */
    public long translate(long vaddr) {
        int vpage = (int) (vaddr >> Const.PAGE_SHIFT);
        int offset = (int) (vaddr & (Const.PAGE_SIZE - 1));
        
        int ppage = getPhysicalPage(vpage);
        if (ppage < 0) {
            return -1; // 页面未映射
        }
        
        return (((long) ppage) << Const.PAGE_SHIFT) | offset;
    }
    
    /**
     * 检查页面权限
     * 
     * @param vpage 虚拟页号
     * @param requiredFlags 需要的权限
     * @return 是否有权限
     */
    public boolean checkPermission(int vpage, int requiredFlags) {
        Integer pageFlags = flags.get(vpage);
        if (pageFlags == null) {
            return false;
        }
        return (pageFlags & requiredFlags) == requiredFlags;
    }
    
    /**
     * 复制页表（用于 fork）
     * 
     * @return 新的页表副本
     */
    public PageTable copy() {
        PageTable newTable = new PageTable();
        newTable.mappings.putAll(this.mappings);
        newTable.flags.putAll(this.flags);
        return newTable;
    }
    
    /**
     * 清空页表
     */
    public void clear() {
        mappings.clear();
        flags.clear();
    }
    
    /**
     * 获取已映射的虚拟页数量
     */
    public int getMappedPageCount() {
        return mappings.size();
    }
    
    /**
     * 获取页面标志
     * 
     * @param vpage 虚拟页号
     * @return 页面标志，如果不存在返回null
     */
    public Integer getFlags(int vpage) {
        return flags.get(vpage);
    }
    
    /**
     * 设置页面标志
     * 
     * @param vpage 虚拟页号
     * @param pageFlags 页面标志
     */
    public void setFlags(int vpage, int pageFlags) {
        flags.put(vpage, pageFlags);
    }
}
