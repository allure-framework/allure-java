package io.qameta.allure.spring4.webmvc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author charlie (Dmitry Baev).
 */
@Configuration
public class AllureWebMvcConfigurerAdapter extends WebMvcConfigurerAdapter {

    @Bean
    public AllureSpring4WebMvc allureSpring4WebMvc() {
        return new AllureSpring4WebMvc();
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(allureSpring4WebMvc());
        super.addInterceptors(registry);
    }
}
