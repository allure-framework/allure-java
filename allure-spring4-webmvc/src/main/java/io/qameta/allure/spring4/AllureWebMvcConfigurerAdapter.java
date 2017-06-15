package io.qameta.allure.spring4;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author charlie (Dmitry Baev).
 */
@Configuration
public class AllureWebMvcConfigurerAdapter extends WebMvcConfigurerAdapter {

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(new AllureSpring4WebMvc());
        super.addInterceptors(registry);
    }
}
