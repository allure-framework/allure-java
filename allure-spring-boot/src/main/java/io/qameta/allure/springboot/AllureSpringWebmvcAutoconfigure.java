package io.qameta.allure.springboot;

import io.qameta.allure.spring4.webmvc.AllureSpring4WebMvc;
import io.qameta.allure.spring4.webmvc.AllureWebMvcConfigurerAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

/**
 * @author charlie (Dmitry Baev).
 */
@Configuration
@ConditionalOnMissingBean(value = AllureSpring4WebMvc.class)
public class AllureSpringWebmvcAutoconfigure extends AllureWebMvcConfigurerAdapter {
}
