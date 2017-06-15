package io.qameta.allure.spring4;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;

/**
 * @author charlie (Dmitry Baev).
 */
@Configuration
@ConditionalOnMissingBean(value = AllureSpring4WebMvc.class)
public class AllureSpringWebmvcAutoconfigure extends AllureWebMvcConfigurerAdapter {
}
