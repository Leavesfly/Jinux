package jinux.mm;

/**
 * 页表接口
 * 
 * 定义虚拟页到物理页的映射操作。
 * 遵循依赖倒置原则（DIP），使页表实现可替换和可测试。
 *
 * @author Jinux Project
 */
public interface IPageTable {

    /**
     * 映射一个虚拟页到物理页
     *
     * @param virtualPage 虚拟页号
     * @param physicalPage 物理页号
     * @param pageFlags 页面标志
     */
    void map(int virtualPage, int physicalPage, int pageFlags);

    /**
     * 取消映射
     *
     * @param virtualPage 虚拟页号
     */
    void unmap(int virtualPage);

    /**
     * 获取虚拟页对应的物理页
     *
     * @param virtualPage 虚拟页号
     * @return 物理页号，如果不存在返回 -1
     */
    int getPhysicalPage(int virtualPage);

    /**
     * 检查虚拟页是否已映射
     *
     * @param virtualPage 虚拟页号
     * @return 是否已映射
     */
    boolean isMapped(int virtualPage);

    /**
     * 虚拟地址转物理地址
     *
     * @param virtualAddress 虚拟地址
     * @return 物理地址，如果未映射返回 -1
     */
    long translate(long virtualAddress);

    /**
     * 检查页面权限
     *
     * @param virtualPage 虚拟页号
     * @param requiredFlags 需要的权限
     * @return 是否有权限
     */
    boolean checkPermission(int virtualPage, int requiredFlags);

    /**
     * 复制页表（用于 fork）
     *
     * @return 新的页表副本
     */
    IPageTable copy();

    /**
     * 清空页表
     */
    void clear();

    /**
     * 获取已映射的虚拟页数量
     *
     * @return 已映射页数
     */
    int getMappedPageCount();

    /**
     * 获取页面标志
     *
     * @param virtualPage 虚拟页号
     * @return 页面标志，如果不存在返回 null
     */
    Integer getFlags(int virtualPage);

    /**
     * 设置页面标志
     *
     * @param virtualPage 虚拟页号
     * @param pageFlags 页面标志
     */
    void setFlags(int virtualPage, int pageFlags);
}
