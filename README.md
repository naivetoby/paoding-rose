# paoding-rose-jade DAO层使用手册
## jdk 需要 1.8以上版本
## 语法
* 1.0 语法支持
    * 1.1 变量赋值
        * 1.1.1 冒号[:] 表示这是一个变量,例如例子中 :limit
        ```java
         @SQL("SELECT user_id, device_token FROM test_table LIMIT :limit")
         public List<Test> getTests(@SQLParam("limit") int limit);
        ```
      * 1.1.2 :1 :2 ....的语法使用
      ```java
      @SQL("SELECT user_id, device_token FROM test_table LIMIT :1")
      public List<Test> getTests(int limit);
      
      @SQL("SELECT user_id, device_token FROM test_table where user_name = :1.userName")
      public List<Test> getTests(User user);
      ```
    * 1.2 字符串拼接
        * 1.2.1 双# 的用法[##]
        ```java
        @SQL("SELECT user_id, device_token FROM test_##(:tableName) where user_name = :user.userName")
        public List<Test> getTests(@SQLParam("tableName") String tableName, @SQLParam("user")User user);
        ```
    * 1.3 条件表达式 #if(){}  #if(){}#else{}
    ```java
    // #if(){}
    @SQL("SELECT user_id, device_token FROM test_user #if(:user!=null){ where user_name = :user.userName}")
    public List<Test> getTests(@SQLParam("user")User user);
    // #if(){}#else{}
    @SQL("SELECT user_id, device_token FROM test_user #if(:user!=null){ where #if(:user.userName!=null){ user_name = :user.userName }#else{ id=:user.id }}")
    public List<Test> getTests(@SQLParam("user")User user);
    ```
    * 1.4 for循环的使用,请参考下方demo中的用例
    * 1.5 for循环中index的使用,请参考下方demo中的用例
    * 1.6 in 的使用
    ```java
     @SQL("SELECT user_id, device_token FROM test_##(:partition) where user_id in(:ids)")
     public List<Test> getTestsByIds(@SQLParam("partition") int partition, @SQLParam("ids") List<Integer> ids);
    ```
    * 1.7 like 的使用
    ```java
     @SQL("SELECT user_id, device_token FROM test_user where user_name like CONCAT('%',:userName,'%')")
     public List<Test> getTestsByIds(@SQLParam("userName") String userName);
    ```
## baseDao 公共方法的抽取
```java
package cn.zhangfusheng.base.server.dao;

import cn.zhangfusheng.base.page.PageRequest;
import cn.zhangfusheng.base.page.SortBy;
import net.paoding.rose.jade.annotation.ReturnGeneratedKeys;
import net.paoding.rose.jade.annotation.SQL;
import net.paoding.rose.jade.annotation.SQLParam;
import net.paoding.rose.jade.annotation.SQLType;

import java.util.*;

/**
 * @ClassName: BaseDao
 * @Author: ZFS
 * @Date: 2018/11/26 16:12
 */
public interface BaseDao<T> {

    public static final String SELECT_SQL = "select $SELECT_COLUMN from `$TABLE_NAME` ";
    public static final String COUNT_SQL = "select count(1) from `$TABLE_NAME` ";
    public static final String DELETE_SQL = "delete from `$TABLE_NAME` ";

    /**
     * 根据id查询
     * @param id 主键id
     * @return T
     */
    @SQL(SELECT_SQL + "where id = :id")
    T queryById(@SQLParam("id") Integer id);

    /**
     * 根据条件查询
     * @param queryMap 查询条件
     * @return
     */
    @SQL(SELECT_SQL + "#if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }}")
    List<T> queryByAll(@SQLParam("m") Map<String, Object> queryMap);

    /**
     * 查询并排序
     * @param queryMap 查询条件
     * @param sortBy   排序方式
     * @return
     */
    @SQL(SELECT_SQL + "#if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }}" +
            " #if(:s != null && :s.columnName!=null && :s.sortOrder!=null){order by ##(:s.columnName) ##(:s.sortOrder)}")
    List<T> queryForSort(@SQLParam("m") Map<String, Object> queryMap, @SQLParam("s") SortBy sortBy);

    /**
     * 分页查询
     * @param queryMap    查询条件
     * @param pageRequest 分页数据
     * @param sortBy      排序方式
     * @return
     */
    @SQL(SELECT_SQL + "#if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }}" +
            " #if(:s != null && :s.columnName!=null && :s.sortOrder!=null){order by ##(:s.columnName) ##(:s.sortOrder)}" +
            " #if(:pageRequest != null){LIMIT :pageRequest.startNum,:pageRequest.endNum}")
    List<T> queryForPage(
            @SQLParam("m") Map<String, Object> queryMap, @SQLParam("pageRequest") PageRequest pageRequest, @SQLParam("s") SortBy sortBy);

    /**
     * 根据条件只能查询出一条结果
     * @param queryMap
     * @return
     */
    @SQL(SELECT_SQL + "#if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }}")
    T queryOne(@SQLParam("m") Map<String, Object> queryMap);

    /**
     * 统计个数
     * @param queryMap
     * @return int
     */
    @SQL(COUNT_SQL + "#if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }}")
    int baseCount(@SQLParam("m") Map<String, Object> queryMap);

    /**
     * 根据id 更新
     * @param keyValue 可以调用cn.slhz.base.bean.BaseBean.beanTOMap(Object object) 方法,将对象转换成map
     * @param id       主键id
     * @return 更新的个数 == 1
     */
    @SQL("UPDATE $TABLE_NAME SET #for(key in :m.keySet()){#if(index!=0){,} `##(:key)` = #(:m[:key]) } WHERE id = :id")
    Integer updateById(@SQLParam("m") Map<String, Object> keyValue, @SQLParam("id") int id);

    /**
     * 插入一条数据 该表必须包含id字段
     * @param keyValue 可以调用cn.slhz.base.bean.BaseBean.beanTOMap(Object object) 方法,将对象转换成map
     * @return 主键id
     */
    @ReturnGeneratedKeys
    @SQL("INSERT INTO $TABLE_NAME" +
            " (#for(key in :keyValue.keySet()){#if(index!=0){,} `##(:key)`})" +
            " VALUES" +
            " (#for(key in :keyValue.keySet()){#if(index!=0){,}'##(:keyValue[:key])'})")
    int insert(@SQLParam("keyValue") Map<String, Object> keyValue);

    /**
     * 批量保存
     * @param keyValue
     * @return
     */
    @SQL("INSERT INTO $TABLE_NAME ( ##(:keyValue.filedName) ) VALUES ##(:keyValue.filedValue) ")
    int insertAll(@SQLParam("keyValue") Map<String, Object> keyValue);

    /**
     * 批量删除
     * @param ids
     * @return
     */
    @SQL("DELETE FROM $TABLE_NAME WHERE id in (:ids)")
    int delete(@SQLParam("ids") List<Integer> ids);

    /**
     * 根据id单个删除
     * @param id
     * @return
     */
    @SQL("DELETE FROM $TABLE_NAME WHERE id = :id")
    int deleteById(@SQLParam("id") int id);

    /**
     * 根据条件删除
     * @param queryMap
     * @return
     */
    @SQL("DELETE FROM $TABLE_NAME #if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }}")
    int delete(@SQLParam("m") Map<String, Object> queryMap);

    /**
     * 先查询 如果不存在 则 插入
     * @param keyValue
     * @return
     */
    @ReturnGeneratedKeys
    @SQL(type = SQLType.WRITE,
            value = "INSERT INTO $TABLE_NAME (#if(index!=0){,}#for(key in :m.keySet()){#if(index!=0){,}`##(:key)`}) select #for(key in :m.keySet()){#if(index!=0){,}'##(:m[:key])'} from dual where not exists (select id from $TABLE_NAME #if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }}})")
    Integer selectInsert(@SQLParam("m") Map<String, Object> keyValue);


}
```
## PageRequest 分页相关参数封装
```java
package cn.zhangfusheng.base.page;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author fusheng.zhang
 * @Description
 * @create 2019-11-05 11:36:00
 */
@Data
@Accessors(chain = true)
public class PageRequest {

    /**
     * 每页的大小
     */
    @ApiModelProperty("分页大小")
    private int pageSize;

    /**
     * 开始页数
     */
    @ApiModelProperty("第几页")
    private int pageNumber;
    /**
     * 开始条数
     */
    @ApiModelProperty(hidden = true)
    private int startNum;
    /**
     * 结束页数
     */
    @ApiModelProperty(hidden = true)
    private int endPage;
    /**
     * 结束条数
     */
    @ApiModelProperty(hidden = true)
    private int endNum;
    /**
     * 总条数
     */
    @ApiModelProperty(hidden = true)
    private int count;
    /**
     * 总条数
     */
    @ApiModelProperty(hidden = true)
    private int total;
    /**
     * 总页数
     */
    @ApiModelProperty(hidden = true)
    private int totalPage;

    public synchronized <T> PageResponse<T> pageResponse() {
        return new PageResponse<>();
    }

    public synchronized <T> PageResponse<T> pageResponse(List<T> data, Integer count) {
        PageResponse<T> tPageResponse = new PageResponse<>();
        tPageResponse.setData(data).setCount(count);
        return tPageResponse;
    }

    public int getPageNumber() {
        return Math.max(pageNumber, 1);
    }

    public int getStartNum() {
        return startNum = pageNumber <= 1 ? 0 : (pageNumber - 1) * getPageSize();
    }

    public int getPageSize() {
        return pageSize = pageSize == 0 ? 10 : pageSize;
    }

    public int getEndPage() {
        return endPage = count % getPageSize() == 0 ? count / getPageSize() : count / getPageSize() + 1;
    }

    public int getEndNum() {
        return endNum = getStartNum() + getPageSize();
    }

    public int getCount() {
        return count;
    }

    public int getTotalPage() {
        return count % pageSize == 0 ? count / pageSize : count / pageSize + 1;
    }

    public int getTotal() {
        total = count == 0 ? 1 : count;
        return total;
    }
}
```
## sortBy 字段排序配置
```java
package cn.zhangfusheng.base.page;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author fusheng.zhang
 * @Description
 * @create 2020-04-13 16:29:00
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ApiModel("排序")
public class SortBy {

    public static final String DESC = "desc";
    public static final String ASC = "asc";

    @ApiModelProperty("排序字段,多个字段采用逗号拼接")
    private String columnName;

    @ApiModelProperty(value = "排序方式 asc desc", allowableValues = "desc,asc")
    private String sortOrder = DESC;
}
```
* 2.0 调整@SQL注解,添加SQLCondition注解,具体用法如下
```java
        // SELECT_SQL 为BaseDAO中提取的公共的sql模板
        final static String TABLE_NAME = "tb_user";
        final static String SELECT_COLUMN = "`id`,`user_name`,`phone`,`password`,`head_img`,`province_id`,`province_name`,`city_id`,`city_name`,`area_id`,`area_name`,`address`,`status`,`create_time`,`update_time`";
    
        /**
         * 产生sql:
         * select `id`,`user_name`,`phone`,`password`,`head_img`,`province_id`,`province_name`,`city_id`,`city_name`,`area_id`,`area_name`,`address`,`status`,`create_time`,`update_time`
         * from `tb_user`
         * where user_name = '123' and phone = '456' and id in (1001) or province_id=1 or (province_id=1 or id in (1001))
         * @param tbUser
         * @return
         */
        @SQL(
                value = SELECT_SQL,
                condition = {
                        @SQLCondition("#if(:vo.userName!=null){user_name = :vo.userName}"),
                        @SQLCondition("#if(:vo.phone!=null){phone = :vo.phone}"),
                        @SQLCondition("#if(:vo.id!=null){id in (:vo.id)}"),
                        @SQLCondition(value = "#if(:vo.provinceId!=null){province_id=:vo.provinceId}", condition = "or"),
                        @SQLCondition(value = "#if(:vo.provinceId!=null){(province_id=:vo.provinceId or id in (:vo.id))}", condition = "or"),
                }
        )
        List<TbUser> find(@SQLParam("vo") TbUser tbUser);
    
    
        /**
         * 产生sql
         * select
         * `id`,`user_name`,`phone`,`password`,`head_img`,`province_id`,`province_name`,`city_id`,`city_name`,`area_id`,`area_name`,`address`,`status`,`create_time`,`update_time`
         * from `tb_user`
         * where id in (1,2,3,4,5)
         * @param ids
         * @return
         */
        @SQL(
                value = SELECT_SQL, condition = {
                @SQLCondition("#if(:1!=null){id in (:1)}"),
        })
        List<TbUser> findByIds(List<Integer> ids);
    
        /**
         * 产生sql:
         * select
         * `id`,`user_name`,`phone`,`password`,`head_img`,`province_id`,`province_name`,`city_id`,`city_name`,`area_id`,`area_name`,`address`,`status`,`create_time`,`update_time`
         * from `tb_user`
         * where
         * user_name in ('1','2','3','4','5')
         * @param names
         * @return
         */
        @SQL(
                value = SELECT_SQL, condition = {
                @SQLCondition("#if(:1!=null){user_name in (:1)}"),
        })
        List<TbUser> findByNames(List<String> names);
    
        /**
         * 产生sql:
         * select
         * `id`,`user_name`,`phone`,`password`,`head_img`,`province_id`,`province_name`,`city_id`,`city_name`,`area_id`,`area_name`,`address`,`status`,`create_time`,`update_time`
         * from `tb_user`
         * where
         * id in (1,2,3,4,5) and id in (1,2,3,4,5) and user_name in ('1','2','3','4','5')
         * @param names
         * @param ids
         * @return
         */
        @SQL(
                value = SELECT_SQL,
                condition = {
                        @SQLCondition(value = "#if(:2!=null){id in (:2)}", values = {"#if(:2!=null){id in (:2)}", "#if(:1!=null){user_name in (:1)}"})
                }
        )
        List<TbUser> findByNamesAndId(List<String> names, List<Integer> ids);
        
        /**
         * 分页 或者排序
         * @param departmentVo
         * @param pageRequest
         * @return
         */
        @SQL(value = SELECT_SQL,
                condition = {
                        @SQLCondition("#if(:vo.companyId!=null){company_id=:vo.companyId}"),
                        @SQLCondition("#if(:vo.name!=null){department_name like CONCAT('%',:vo.name,'%')}"),
                        @SQLCondition(value = "order by id", condition = ""),
                        @SQLCondition(value = "#if(:pageRequest != null){LIMIT :pageRequest.startNum,:pageRequest.endNum}", condition = ""),
                }
        )
        List<TbDepartment> find(@SQLParam("vo") QueryDepartmentVo departmentVo, @SQLParam("pageRequest") PageRequest pageRequest);

```