package io.qameta.allure.spring4;

import io.qameta.allure.attachment.DefaultAttachmentProcessor;
import io.qameta.allure.attachment.FreemarkerAttachmentRenderer;
import io.qameta.allure.attachment.http.HttpRequestAttachment;
import io.qameta.allure.attachment.http.HttpResponseAttachment;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static io.qameta.allure.servletapi.HttpServletAttachmentBuilder.buildRequest;
import static io.qameta.allure.servletapi.HttpServletAttachmentBuilder.buildResponse;

/**
 * @author charlie (Dmitry Baev).
 */
public class AllureSpring4WebMvc implements HandlerInterceptor {

    @Override
    public boolean preHandle(final HttpServletRequest request,
                             final HttpServletResponse response,
                             final Object handler) throws Exception {
        return true;
    }

    @Override
    public void postHandle(final HttpServletRequest request,
                           final HttpServletResponse response,
                           final Object handler, final ModelAndView modelAndView) throws Exception {
        //do nothing
    }

    @Override
    public void afterCompletion(final HttpServletRequest request,
                                final HttpServletResponse response,
                                final Object handler,
                                final Exception ex) throws Exception {
        final HttpRequestAttachment requestAttachment = buildRequest(request);
        final HttpResponseAttachment responseAttachment = buildResponse(response);
        final DefaultAttachmentProcessor processor = new DefaultAttachmentProcessor();

        processor.addAttachment(
                requestAttachment,
                new FreemarkerAttachmentRenderer("http-request.ftl")
        );

        processor.addAttachment(
                responseAttachment,
                new FreemarkerAttachmentRenderer("http-response.ftl")
        );
    }
}
