package net.paoding.rose.jade.config;

import net.paoding.rose.jade.context.spring.JadeBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * @author fusheng.zhang
 * @Description
 * @create 2020-03-24 15:57:00
 */
// @Configuration
// @ConditionalOnProperty(prefix = "paoding.rose.jade", name = "enable", havingValue = "true")
public class JadeBeanFactoryConfig extends JadeBeanFactoryPostProcessor {

}