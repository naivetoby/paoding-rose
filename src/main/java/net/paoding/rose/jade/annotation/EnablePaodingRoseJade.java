package net.paoding.rose.jade.annotation;

import net.paoding.rose.jade.config.JadeConfig;
import net.paoding.rose.jade.context.spring.JadeBeanFactoryRegister;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author fusheng.zhang
 * @date 2021-08-29 11:05:16
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import({JadeBeanFactoryRegister.class, JadeConfig.class})
public @interface EnablePaodingRoseJade {

    @AliasFor("basePackage")
    String[] value() default {};

    String[] basePackage() default {};

}
