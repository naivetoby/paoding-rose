import net.paoding.rose.jade.jpa.JpaDetail;
import net.paoding.rose.jade.jpa.JpaUtil;
import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

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
        Pageable pageable = PageRequest.of(0, 10, Sort.by(new Sort.Order(Sort.Direction.ASC, "nameLength")));
        JpaDetail jpaDetail = new JpaUtil("findByIdAndNameLengthOrderByAgeDescIdAscStatus", User.class, null)
                .methodToSql("id,age", "tb_user");
        System.out.println(jpaDetail.sql(Optional.of(pageable)));
    }
}
