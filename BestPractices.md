# paoding-rose-jade 最佳实践
* baseBean.java
```java
package cn.zhangfusheng.base.model;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 所有的 model 都需要实现 该类,并且需要有 Long 类型的id 以及 LocalDateTime 类型的 createTime updateTime
 * 既 每张表都需要存在 id  create_time update_time 字段
 * 注: id 的类型 可以根据需要修改
 * @ClassName: BeanBase
 * @Author: ZFS
 * @Date: 2018/11/27 11:51
 */
public interface BaseBean extends Serializable {

    BaseBean setId(Long id);

    Long getId();

    BaseBean setUpdateTime(LocalDateTime updateTime);

    LocalDateTime getUpdateTime();

    BaseBean setCreateTime(LocalDateTime updateTime);

    LocalDateTime getCreateTime();

}
```
* baseDao
```java
package cn.zhangfusheng.base.server.dao;

import cn.zhangfusheng.base.model.BaseBean;
import cn.zhangfusheng.base.page.PageRequest;
import cn.zhangfusheng.base.page.SortBy;
import net.paoding.rose.jade.annotation.ReturnGeneratedKeys;
import net.paoding.rose.jade.annotation.SQL;
import net.paoding.rose.jade.annotation.SQLParam;
import net.paoding.rose.jade.annotation.SQLType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * dao 层公共方法的抽取
 * 继承 BaseDao 的 Dao成接口 中 必须定义 final static 修饰的 SELECT_COLUMN 和 TABLE_NAME 两个字段
 * TABLE_NAME 对应的表名称 SELECT_COLUMN 表中的字段
 * 实现简单的CRUD查询
 * @ClassName: BaseDao
 * @Author: ZFS
 * @Date: 2018/11/26 16:12
 */
public interface BaseDao<T extends BaseBean> {

    public static final String SELECT_SQL = "select $SELECT_COLUMN from `$TABLE_NAME` ";
    public static final String COUNT_SQL = "select count(1) from `$TABLE_NAME` ";
    public static final String DELETE_SQL = "delete from `$TABLE_NAME` ";

    /**
     * 根据id查询
     * @param id 主键id
     * @return T
     */
    @SQL(SELECT_SQL + "where id = :id")
    T queryById(@SQLParam("id") Long id);

    /**
     * 根据id查询 返回 optional
     * @param id
     * @return
     */
    @SQL(SELECT_SQL + "where id = :id")
    Optional<T> queryByIdForOptional(@SQLParam("id") Long id);

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
    @SQL(SELECT_SQL +
            "#if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key])}}" +
            " #if(:l != null){#for(key in :l.keySet()){#if(index == 0){#if(:m != null){and}#else{where}}#else{#if(index != 0){and}} `##(:key)` like ##(:l[:key])}}" +
            " #if(:s != null && :s.columnName!=null && :s.sortOrder!=null){order by ##(:s.columnName) ##(:s.sortOrder)}" +
            " #if(:pageRequest != null){LIMIT :pageRequest.startNum,:pageRequest.pageSize}")
    List<T> queryForPage(
            @SQLParam("m") Map<String, Object> queryMap,
            @SQLParam("l") Map<String, Object> likeMap,
            @SQLParam("pageRequest") PageRequest pageRequest,
            @SQLParam("s") SortBy sortBy);

    /**
     * 根据条件只能查询出一条结果
     * @param queryMap
     * @return
     */
    @SQL(SELECT_SQL + "#if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }}")
    T queryOne(@SQLParam("m") Map<String, Object> queryMap);

    /**
     * 根据查询条件只能查询出一条结果
     * 返回值一个Optional
     * @param queryMap
     * @return
     */
    @SQL(SELECT_SQL + "#if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }}")
    Optional<T> queryOneForOptional(@SQLParam("m") Map<String, Object> queryMap);

    /**
     * 统计个数
     * @param queryMap
     * @return int
     */
    @SQL(COUNT_SQL + "#if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key])}}" +
            " #if(:l != null){#for(key in :l.keySet()){#if(index == 0){#if(:m != null){and}#else{where}}#else{#if(index != 0){and}} `##(:key)` like ##(:l[:key])}}")
    int baseCount(@SQLParam("m") Map<String, Object> queryMap, @SQLParam("l") Map<String, Object> likeMap);

    /**
     * 根据id 更新
     * @param keyValue 可以调用cn.slhz.base.bean.BaseBean.beanTOMap(Object object) 方法,将对象转换成map
     * @param id       主键id
     * @return 更新的个数 == 1
     */
    @SQL("UPDATE $TABLE_NAME SET #for(key in :m.keySet()){#if(index!=0){,} `##(:key)` = #(:m[:key]) } WHERE id = :id")
    int updateById(@SQLParam("m") Map<String, Object> keyValue, @SQLParam("id") Long id);

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
    Long insert(@SQLParam("keyValue") Map<String, Object> keyValue);

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
    @SQL(DELETE_SQL + " WHERE id in (:ids)")
    int delete(@SQLParam("ids") List<Long> ids);

    /**
     * 根据id单个删除
     * @param id
     * @return
     */
    @SQL(DELETE_SQL + " WHERE id = :id")
    int deleteById(@SQLParam("id") Long id);

    /**
     * 根据条件删除
     * @param queryMap
     * @return
     */
    @SQL(DELETE_SQL + " #if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }}")
    int delete(@SQLParam("m") Map<String, Object> queryMap);

    /**
     * 先查询 如果不存在 则 插入
     * @param keyValue
     * @return
     */
    @ReturnGeneratedKeys
    @SQL(type = SQLType.WRITE,
            value = "INSERT INTO $TABLE_NAME (#if(index>0){,}#for(key in :m.keySet()){#if(index>0){,}`##(:key)`}) select #for(key in :m.keySet()){#if(index>0){,}'##(:m[:key])'} from dual where not exists (select id from $TABLE_NAME #if(:m != null){#for(key in :m.keySet()){#if(index==0){where}#else{#if(index!=0){and}} `##(:key)`=#(:m[:key]) }})")
    Long selectInsert(@SQLParam("m") Map<String, Object> keyValue);

}
```
* PageRequest
```java
package cn.zhangfusheng.base.page;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 分页查询请求参数
 * @author fusheng.zhang
 * @Description
 * @create 2019-11-05 11:36:00
 */
@Data
@Accessors(chain = true)
@ApiModel("分页查询参数")
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
        return new PageResponse<>(this);
    }

    public synchronized <T> PageResponse<T> pageResponse(List<T> data, Integer count) {
        PageResponse<T> tPageResponse = new PageResponse<>(this);
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
* PageResponse
```java
package cn.zhangfusheng.base.page;

import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 分页数据响应
 * @author fusheng.zhang
 * @Description
 * @create 2019-11-05 11:37:00
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Accessors(chain = true)
@ApiModel("分页响应")
public class PageResponse<T> extends PageRequest {

    public PageResponse(int pageSize, int pageNumber, int total) {
        super.setPageSize(pageSize).setPageNumber(pageNumber).setTotal(total);
    }

    public PageResponse(PageRequest pageRequest) {
        super.setPageSize(pageRequest.getPageSize())
                .setPageNumber(pageRequest.getPageNumber())
                .setStartNum(pageRequest.getStartNum())
                .setEndPage(pageRequest.getEndPage())
                .setCount(pageRequest.getCount())
                .setTotal(pageRequest.getTotal())
                .setTotalPage(pageRequest.getTotalPage());

    }

    /**
     * 分页数据
     */
    private List<T> data;


    public PageResponse<T> setData(List<T> data) {
        if (CollectionUtils.isEmpty(data)) {
            this.data = new ArrayList<T>(0);
        } else {
            this.data = data;
        }
        return this;
    }

    public List<T> getData() {
        if (CollectionUtils.isEmpty(data)) {
            return new ArrayList<T>(0);
        }
        return data;
    }
}
```
* SortBy
```java
package cn.zhangfusheng.base.page;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 排序
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
> ======================================================================================================================
> ================================经过上述的公共方法的抽取已经可以实现部分crud            ======================================
> ================================下边给出spring boot 整合 paoding-rose-jade 的最佳实践=====================================
> ======================================================================================================================
## 自定义spring boot 的部分注解
### 自定义 autowired 方便bean的注入__SimpleBaseService.getJadeDAO(),BaseController.getBaseService()
* Autowired
```java
package cn.zhangfusheng.base.server.core.annotation.bean;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fusheng.zhang
 * @Description
 * @create 2020-03-02 14:49:00
 */
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.PARAMETER, ElementType.FIELD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@org.springframework.beans.factory.annotation.Autowired
@Documented
@Inherited
public @interface Autowired {

    @AliasFor(annotation = org.springframework.beans.factory.annotation.Autowired.class, attribute = "required")
    boolean required() default true;

    /**
     * 基础的service 实体注入
     * 一个类中只能有一个
     * 并且建议是第一个注入
     * @return
     */
    boolean baseBean() default false;

}

```
* Like 实现字段的模糊查询
```java
package cn.zhangfusheng.base.annotation.sql;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 模糊查询
 * 注: 使用该注解的字段需要是string 类型
 * @author fusheng.zhang
 * @date 2020-10-19 15:27:26
 */
@Documented
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Like {

    /**
     * 前缀
     * @return
     */
    String prefix() default "%";

    /**
     * 后缀
     * @return
     */
    String suffix() default "%";
}
```
### 简单的CRUD 抽取
* BaseDao __ 使用 上方的 baseDao 即可
* SimpleBaseService
```java
package cn.zhangfusheng.base.server.service;

import cn.zhangfusheng.base.annotation.sql.Like;
import cn.zhangfusheng.base.server.core.constant.BaseConstant;
import cn.zhangfusheng.base.model.BaseBean;
import cn.zhangfusheng.base.server.core.annotation.bean.Autowired;
import cn.zhangfusheng.base.server.dao.BaseDao;
import cn.zhangfusheng.util.base.exception.GlobalSystemException;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Field;

/**
 * @author fusheng.zhang
 * @date 2020-10-19 15:22:36
 */
public interface SimpleBaseService<T extends BaseBean> {

    /**
     * 这个方法是必须实现的,需要返回 对应的dao对象
     * 需要 使用自定义的 cn.zhangfusheng.base.server.annotation.bean.Autowired 注解,并且设置 baseBean=true
     * 建议: 放在类的第一个注入,方便快速查找
     * @return BaseDao
     */
    default BaseDao<T> getJadeDAO() {
        try {
            Class<?> aClass = this.getClass();
            Field[] declaredFields = aClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                Autowired annotation = declaredField.getAnnotation(Autowired.class);
                if (annotation != null && annotation.baseBean()) {
                    //noinspection unchecked
                    return (BaseDao<T>) declaredField.get(this);
                }
            }
            throw new GlobalSystemException(aClass.getName() + "：获取jade dao失败");
        } catch (Exception e) {
            throw new GlobalSystemException(e);
        }
    }

    /**
     * 将单个对象转换成Map,并且对象中的属性使用下划线进行拼接
     * @return {"city_first_char":"123","id":1}
     */
    default JSONObject beanToMap(T t) {
        JSONObject beanMap = JSONObject.parseObject(JSONObject.toJSONString(t, BaseConstant.SerializeConstant.SNAKE_CASE_CONFIG, SerializerFeature.WriteDateUseDateFormat));
        if (ObjectUtils.isEmpty(beanMap)) {
            beanMap = null;
        }
        return beanMap;
    }

    /**
     * 带有 Like 注解的字段是否进行模糊查询
     * @param t
     * @return 返回模糊查询的 key value
     * @throws IllegalAccessException
     */
    default JSONObject checkLike(T t) {
        try {
            Class<? extends BaseBean> aClass = t.getClass();
            BaseBean baseBean = aClass.newInstance();
            for (Field field : aClass.getDeclaredFields()) {
                field.setAccessible(true);
                Like like = field.getAnnotation(Like.class);
                if (like != null) {
                    Object tValue = field.get(t);
                    if (!ObjectUtils.isEmpty(tValue)) {
                        if (tValue instanceof String) {
                            field.set(t, null);
                            field.set(baseBean, String.format("CONCAT('%s','%s','%s')", like.prefix(), tValue, like.suffix()));
                        } else {
                            throw new GlobalSystemException("请将带有like注解的字段设置为string 类型");
                        }
                    }
                }
            }
            if (ObjectUtils.isEmpty(baseBean)) {
                return null;
            }
            return JSONObject.parseObject(JSONObject.toJSONString(baseBean, BaseConstant.SerializeConstant.SNAKE_CASE_CONFIG, SerializerFeature.WriteDateUseDateFormat));
        } catch (IllegalAccessException | InstantiationException e) {
            throw new GlobalSystemException(e);
        }
    }
}

```
* BaseService
```java
package cn.zhangfusheng.base.server.service;

import cn.zhangfusheng.base.model.BaseBean;
import cn.zhangfusheng.base.page.PageRequest;
import cn.zhangfusheng.base.page.PageResponse;
import cn.zhangfusheng.base.page.SortBy;
import cn.zhangfusheng.base.server.core.config.ServerAssemblyConfig;
import cn.zhangfusheng.base.server.core.redis.RedisHelp;
import cn.zhangfusheng.base.server.dao.BaseDao;
import cn.zhangfusheng.util.base.assertion.AssertUtil;
import cn.zhangfusheng.util.base.exception.GlobalSystemException;
import com.alibaba.fastjson.JSONObject;
import org.springframework.util.ObjectUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * @ClassName: BaseService
 * @Author: ZFS
 * @Date: 2018/12/7 11:48
 */
public interface BaseService<T extends BaseBean> extends SimpleBaseService<T> {

    /**
     * 根据id查询
     * @param id 主键id
     * @return T
     */
    default T queryById(Long id) {
        return getJadeDAO().queryById(id);
    }

    /**
     * 根据id查询 返回 optional
     * @param id
     * @return
     */
    default Optional<T> queryByIdForOptional(Long id) {
        return getJadeDAO().queryByIdForOptional(id);
    }

    /**
     * 根据条件只能查询出一条结果
     * @param t
     * @return
     */
    default T queryOne(T t) {
        return getJadeDAO().queryOne(beanToMap(t).getInnerMap());
    }

    /**
     * 根据条件只能查询出一条结果
     * 返回Optional
     * @param t
     * @return
     */
    default Optional<T> queryOneForOptional(T t) {
        return getJadeDAO().queryOneForOptional(beanToMap(t).getInnerMap());
    }

    /**
     * 根据where 条件 查询
     * @param t 继承了 BaseBean 的实体
     * @return List<T>
     */
    default List<T> queryByAll(T t) {
        return getJadeDAO().queryByAll(beanToMap(t));
    }

    /**
     * 根据条件查询并排序
     * @param t      查询条件
     * @param sortBy 排序方式
     * @return
     */
    default List<T> queryForSort(T t, SortBy sortBy) {
        return getJadeDAO().queryForSort(beanToMap(t), sortBy);
    }

    /**
     * 分页查询
     * @param t           查询条件
     * @param pageRequest 分页数据
     * @param sortBy      排序方式
     * @return
     */
    default PageResponse<T> queryForPage(T t, PageRequest pageRequest, SortBy sortBy) {
        JSONObject likeMap = this.checkLike(t);
        JSONObject beanMap = this.beanToMap(t);
        BaseDao<T> jadeDAO = this.getJadeDAO();
        int count = jadeDAO.baseCount(beanMap, likeMap);
        if (count > 0) {
            List<T> ts = jadeDAO.queryForPage(beanMap, likeMap, pageRequest, sortBy);
            return pageRequest.pageResponse(ts, count);
        } else {
            return pageRequest.pageResponse();
        }
    }

    /**
     * 数据统计
     * @param t 查询条件
     * @return int
     */
    default int baseCount(T t) {
        JSONObject likeMap = checkLike(t);
        JSONObject beanMap = beanToMap(t);
        return getJadeDAO().baseCount(beanMap, likeMap);
    }

    /**
     * 根据id更新
     * @param t 更新字段和值 不想更新的字段赋值为空或原值
     * @return 更新个数
     */
    default T updateById(T t) {
        AssertUtil.checkEmpty(t, new GlobalSystemException("update model is null"));
        AssertUtil.checkEmpty(t.getId(), new GlobalSystemException("update model id is null"));
        t.setUpdateTime(LocalDateTime.now());
        int update = getJadeDAO().updateById(beanToMap(t), t.getId());
        AssertUtil.checkExpression(update < 0, "更新失败");
        return t;
    }

    /**
     * 插入
     * @param t 对象
     * @return id
     */
    default T insert(T t) {
        AssertUtil.checkExpression(ObjectUtils.isEmpty(t), new GlobalSystemException("save model is null"));
        LocalDateTime nowTime = LocalDateTime.now();
        if (!ServerAssemblyConfig.projectDataSourceProperties.isAutoIncrement()) {
            t.setId(RedisHelp.id(this.getClass()));
        }
        t.setCreateTime(nowTime).setUpdateTime(nowTime);
        Long id = getJadeDAO().insert(beanToMap(t));
        AssertUtil.checkExpression(id < 0, "添加失败");
        t.setId(id);
        return t;
    }

    /**
     * 保存或者更新
     * 存在id是更新
     * @param t
     * @return
     */
    default T insertOrUpdate(T t) {
        AssertUtil.checkEmpty(t, "更新或者插入的参数为空");
        if (t.getId() != null) {
            return this.updateById(t);
        } else {
            return this.insert(t);
        }
    }

    /**
     * 查询结果不存在则插入
     * @param t
     * @return
     */
    default T selectInsert(T t) {
        Long id = getJadeDAO().selectInsert(beanToMap(t));
        t.setId(id);
        return t;
    }

    /**
     * 删除
     * @param ids ids
     * @return 删除个数
     */
    default Integer delete(List<Long> ids) {
        AssertUtil.checkEmpty(ids, new GlobalSystemException("delete ids is null"));
        int delete = getJadeDAO().delete(ids);
        AssertUtil.checkExpression(delete < 0, "删除失败");
        return delete;
    }

    /**
     * 根据id删除
     * @param id
     * @return
     */
    default int deleteById(Long id) {
        AssertUtil.checkEmpty(id, new GlobalSystemException("delete id is null"));
        int delNum = getJadeDAO().deleteById(id);
        AssertUtil.checkExpression(delNum < 0, "删除失败");
        return delNum;
    }
}
```
* BaseController
```java
package cn.zhangfusheng.base.server.controller;

import cn.zhangfusheng.base.annotation.valid.Valid;
import cn.zhangfusheng.base.model.BaseBean;
import cn.zhangfusheng.base.page.PageRequest;
import cn.zhangfusheng.base.page.PageResponse;
import cn.zhangfusheng.base.page.SortBy;
import cn.zhangfusheng.base.response.BaseResponse;
import cn.zhangfusheng.base.server.core.annotation.bean.Autowired;
import cn.zhangfusheng.base.server.service.BaseService;
import cn.zhangfusheng.util.base.exception.GlobalSystemException;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author fusheng.zhang
 * @Description
 * @create 2020-01-13 16:48:00
 */
public interface BaseController<T extends BaseBean> {

    @GetMapping("{id}")
    @ApiOperation(value = "根据id查询", notes = "根据id查询", produces = "application/json")
    default BaseResponse<T> queryById(@PathVariable Long id) {
        return BaseResponse.success(getBaseService().queryById(id));
    }

    @GetMapping
    @ApiOperation(value = "根据条件查询全部[不支持复杂查询]", notes = "根据条件查询全部[不支持复杂查询]", produces = "application/json")
    default BaseResponse<List<T>> queryByAll(T t) {
        return BaseResponse.success(getBaseService().queryByAll(t));
    }

    @GetMapping("queryForSort")
    @ApiOperation(value = "根据条件查询全部[排序]", notes = "根据条件查询全部[排序]", produces = "application/json")
    default BaseResponse<List<T>> queryForSort(T t, SortBy sortBy) {
        return BaseResponse.success(getBaseService().queryForSort(t, sortBy));
    }

    @GetMapping("queryForPage")
    @ApiOperation(value = "根据条件[分页]查询全部[不支持复杂查询]", notes = "根据条件[分页]查询全部[不支持复杂查询]", produces = "application/json")
    default BaseResponse<PageResponse<T>> queryForPage(T t, PageRequest pageRequest, SortBy sortBy) {
        return BaseResponse.success(getBaseService().queryForPage(t, pageRequest, sortBy));
    }

    @GetMapping("queryOne")
    @ApiOperation(value = "根据条件查询[该条件的对应的数据只能存在一条]", notes = "根据条件查询[该条件的对应的数据只能存在一条]", produces = "application/json")
    default BaseResponse<T> queryOne(T t) {
        return BaseResponse.success(getBaseService().queryOne(t));
    }

    @PutMapping
    @ApiOperation(value = "更新[id必须存在]", notes = "更新[id必须存在]", produces = "application/json")
    default BaseResponse<T> updateById(@RequestBody T t) {
        return BaseResponse.success("更新成功", getBaseService().updateById(t));
    }

    @PostMapping
    @ApiOperation(value = "添加", notes = "添加", produces = "application/json")
    default BaseResponse<T> insert(@RequestBody T t) {
        return BaseResponse.success("添加成功", getBaseService().insert(t));
    }

    @PostMapping("insertOrUpdate")
    @ApiOperation(value = "保存或者更新", notes = "保存或者更新", produces = "application/json")
    default BaseResponse<T> insertOrUpdate(@RequestBody T t) {
        return BaseResponse.success("修改成功", getBaseService().insertOrUpdate(t));
    }

    @DeleteMapping("{id}")
    @ApiOperation(value = "根据id删除", notes = "根据id删除", produces = "application/json")
    default BaseResponse<Integer> delete(@PathVariable Long id) {
        return BaseResponse.success("删除成功", getBaseService().deleteById(id));
    }

    /**
     * 获取service
     * 需要 使用自定义的 cn.zhangfusheng.base.server.annotation.bean.Autowired 注解,并且设置 baseBean=true
     * 建议: 放在类的第一个注入,方便快速查找
     * @return
     */
    default BaseService<T> getBaseService() {
        try {
            Class<?> aClass = this.getClass();
            Field[] declaredFields = aClass.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                declaredField.setAccessible(true);
                Autowired annotation = declaredField.getAnnotation(Autowired.class);
                if (annotation != null && annotation.baseBean()) {
                    //noinspection unchecked
                    return (BaseService<T>) declaredField.get(this);
                }
            }
            throw new GlobalSystemException(aClass.getName() + "：获取对应的baseService失败");
        } catch (Exception e) {
            throw new GlobalSystemException(e);
        }
    }

}
```
* BaseResponse
```java
package cn.zhangfusheng.base.response;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 响应数据对象
 *
 * @ClassName: BaseResponse
 * @Author: ZFS
 * @Date: 2018/11/27 15:03
 */
@Data
@Accessors(chain = true)
public class BaseResponse<T> {

    public static final Integer STATUS_200 = 200;
    public static final Integer STATUS_400 = 400;
    public static final Integer STATUS_404 = 404;
    public static final Integer STATUS_412 = 412;
    public static final Integer STATUS_500 = 500;
    public static final Integer STATUS_401 = 401;

    private Integer result;
    private String msg;
    private T data;

    public synchronized static BaseResponse<Object> login(String msg) {
        return BaseResponse.error(msg);
    }

    public synchronized static <V> BaseResponse<V> loginSuccess(String msg, V data) {
        BaseResponse<V> baseResponse = new BaseResponse<>();
        baseResponse.setMsg(msg).setData(data).setResult(STATUS_200);
        return baseResponse;
    }

    public synchronized static <V> BaseResponse<V> success(String msg) {
        BaseResponse<V> baseResponse = new BaseResponse<>();
        baseResponse.setMsg(msg).setResult(STATUS_200);
        return baseResponse;
    }

    public synchronized static <V> BaseResponse<V> success(String msg, V data) {
        BaseResponse<V> tBaseResponse = new BaseResponse<>();
        tBaseResponse.setResult(STATUS_200).setData(data).setMsg(msg);
        return tBaseResponse;
    }

    public synchronized static <V> BaseResponse<V> success(V data) {
        BaseResponse<V> tBaseResponse = new BaseResponse<>();
        tBaseResponse.setResult(STATUS_200).setData(data);
        return tBaseResponse;
    }

    @SafeVarargs
    public synchronized static <V> BaseResponse<V> error(String msg, V... data) {
        BaseResponse<V> baseResponse = new BaseResponse<>();
        baseResponse.setMsg(msg).setResult(STATUS_400);
        return baseResponse;
    }

    /**
     * 是否成功
     *
     * @return
     */
    public boolean success() {
        return STATUS_200.equals(this.result);
    }
}
```
> ======================================================================================================================
> ================================来个demo展示一下========================================================================
> ======================================================================================================================
### demo
* TbAddress (需要implements BaseBean)
```java
package cn.zhangfusheng.user.model;

import cn.zhangfusheng.base.model.BaseBean;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;


import cn.zhangfusheng.base.annotation.valid.NotNull;
import java.time.LocalDateTime;

/**
 * @description :
 * @author: fusheng.zhang
 * @date: 2020-06-02 15:17:46
 */
@Data
@ToString(callSuper = true)
@Accessors(chain = true)
@ApiModel(description = "全国省市县")
public class TbAddress implements BaseBean {

    @ApiModelProperty("")
    private Long id;

    @ApiModelProperty("父级id")
    @NotNull("[父级id]parentId is null or blank")
    private Integer parentId;

    @ApiModelProperty("全名")
    @NotNull("[全名]fullName is null or blank")
    private String fullName;

    @ApiModelProperty("地址名称")
    @NotNull("[地址名称]name is null or blank")
    private String name;

    @ApiModelProperty("地址编码")
    private String addressCode;

    @ApiModelProperty("城市首字母")
    @NotNull("[城市首字母]cityFirstChar is null or blank")
    private String cityFirstChar;

    @ApiModelProperty("汉语拼音")
    @NotNull("[汉语拼音]cityPinYin is null or blank")
    private String cityPinYin;

    @ApiModelProperty("等级")
    @NotNull("[等级]level is null or blank")
    private Integer level;

    @ApiModelProperty("首字")
    @NotNull("[首字]provinceChar is null or blank")
    private String provinceChar;

    @ApiModelProperty("排序")
    @NotNull("[排序]sort is null or blank")
    private Integer sort;

    @ApiModelProperty("0启用,1禁用")
    private Integer status;

    @ApiModelProperty("创建时间")
    private LocalDateTime createTime;

    @ApiModelProperty("更新时间")
    private LocalDateTime updateTime;

}
```
* TbAddressService (extends BaseService)
```java
package cn.zhangfusheng.user.server.service.address;

import cn.zhangfusheng.base.server.service.BaseService;
import cn.zhangfusheng.user.model.TbAddress;

import java.util.List;

/**
 * @description : 全国省市县
 * @author: fusheng.zhang
 * @date: 2020-03-05 16:34:27
 */
public interface TbAddressService extends BaseService<TbAddress> {
    /**
     * 根据父id查询
     * @param pid
     * @return
     */
    List<TbAddress> findByParendId(Integer pid);

}
```
* TbAddressServiceImpl (implements TbAddressService)
```java
package cn.zhangfusheng.user.server.service.address.impl;

import cn.zhangfusheng.base.server.core.annotation.bean.Autowired;
import cn.zhangfusheng.user.model.TbAddress;
import cn.zhangfusheng.user.server.dao.TbAddressDAO;
import cn.zhangfusheng.user.server.service.address.TbAddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description : 全国省市县
 * @author: fusheng.zhang
 * @date: 2020-03-05 16:34:27
 */
@Slf4j
@Service
public class TbAddressServiceImpl implements TbAddressService {

    @Autowired(baseBean = true)
    private TbAddressDAO tbAddressDao;

    @Override
    public List<TbAddress> findByParendId(Integer pid) {
        return this.queryByAll(new TbAddress().setParentId(pid));
    }
}
```
* TbAddressController (implements BaseController)
```java
package cn.zhangfusheng.user.server.controller.address;

import cn.zhangfusheng.base.response.BaseResponse;
import cn.zhangfusheng.base.server.controller.BaseController;
import cn.zhangfusheng.base.server.core.annotation.bean.Autowired;
import cn.zhangfusheng.base.server.core.annotation.controller.RestController;
import cn.zhangfusheng.base.server.core.annotation.project.SkipAuth;
import cn.zhangfusheng.user.model.TbAddress;
import cn.zhangfusheng.user.server.service.address.TbAddressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * @description :
 * @author: fusheng.zhang
 * @date: 2020-03-05 16:34:27
 */
@RestController("tb/address")
@Api(tags = "全国省市县")
public class TbAddressController implements BaseController<TbAddress> {

    @Autowired(baseBean = true)
    private TbAddressService tbAddressService;

    @SkipAuth
    @GetMapping("findByPid")
    @ApiOperation(value = "根据条件查询全部", notes = "根据条件查询全部", produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pid", value = "pid", dataType = "int", paramType = "query"),
    })
    public BaseResponse<List<TbAddress>> findByParendId(Integer pid) {
        return BaseResponse.success(tbAddressService.findByParendId(pid));
    }
}
```
> demo结束,实现了基本的CRUD,以及部分字段的模糊搜索等等,其他复杂的接口还需要自主编写