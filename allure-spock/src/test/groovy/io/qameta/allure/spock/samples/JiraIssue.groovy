package io.qameta.allure.spock.samples

import io.qameta.allure.LabelAnnotation

import java.lang.annotation.Documented
import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

/**
 * @author vbragin
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@LabelAnnotation(name = "jira")
@interface JiraIssue {
    String value();
}
