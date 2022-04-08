import net.paoding.rose.jade.jpa.JpaDetail;
import net.paoding.rose.jade.jpa.JpaUtil;
import org.junit.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author fusheng.zhang
 * @date 2022-02-17 19:39:18
 */
public class TestJpa {

    class User {
        private int id;
        private int age;
        private int status;
        private int nameLength;
    }

    @Test
    public void test() {
        String field = "id,age", tableName = "tb_user", methodName = "findByIdInAndIdAndNameLengthOrderByAgeDescIdAscStatus";
        Class<?> returnType = User.class;
        Pageable pageable = PageRequest.of(0, 10, Sort.by(new Sort.Order(Sort.Direction.ASC, "nameLength")));
        JpaUtil jpaUtil = new JpaUtil(field, tableName, methodName, returnType, null);
        JpaDetail jpaDetail = jpaUtil.methodToSql(new Object[]{Arrays.asList(3, 4, 5), 1, 2});
        System.out.println(jpaDetail.sql(Optional.of(pageable)));
        System.out.println(jpaDetail.getArgs());
    }
}
