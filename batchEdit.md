```java
import net.paoding.rose.jade.annotation.SQL;

import java.util.Collection;
import java.util.List;

/**
 * 批量操作示例
 * 1. 如果想产生一条sql 参数就使用 Collection
 * 2. 如果需要产生多条sql 参数就使用 List
 */
public interface BatchDemo {
    /**
     * ❌ 注意参数类型
     * 假设ids = [1,2] 
     * 这个批量删除是错误的,会产生两条sql语句如下:
     * delete from tb_user where id in (1)
     * delete from tb_user where id in (2)
     * @param ids
     * @return
     */
    @SQL("delete from tb_user where id in (:1)")
    int[] delByIdIn(List<Integer> ids);

    /**
     * ✅ 注意参数类型
     * 假设ids = [1,2]
     * 该方法产生的sql如下
     * delete from tb_user where id in (1,2)
     * @param ids
     * @return
     */
    @SQL("delete from tb_user where id in (:1)")
    int[] delByIdIn(Collection<Integer> ids);

    /**
     * 批量插入 同理
     * 批量更新.参数存在几条数据就会产生几条sql
     * update tb_user set user_name='li',password='12' where id=1
     * update tb_user set user_name='li2',password='123' where id=2
     * @param tbUsers
     * @return
     */
    @SQL("update tb_user set user_name=:1.userName,password=:1.pwd where id=:1.id")
    int[] batchUpdate(List<TbUser> tbUsers);
}
```