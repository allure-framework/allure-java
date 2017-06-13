package io.qameta.allure.spock

import io.qameta.allure.*
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Title
import spock.lang.Unroll

/**
 * Created on 12.06.2017
 *
 * @author Yuri Kudryavtsev
 *         skype: yuri.kudryavtsev.indeed
 *         email: yuri.kudryavtsev@indeed-id.com
 */

@Feature("Feature annotation")
@Title("Title spec annotation")
@Link("https://www.yandex.ru/")
@Issue("AKiOS-280")
@TmsLink("TEST1")
@Flaky
@Muted
@Epic("EPIC LABEL!")
@Owner("yuri.kudryavtsev")
class FeatureCombinationsTest extends Specification {

    @Shared
    String specLogin
    @Shared
    String specPass
    @Shared
    Integer specSum


    @Unroll
    @Story("Story annotation")
    @Severity(SeverityLevel.CRITICAL)
    @Link("https://www.ya.ru/")
    @Issue("AKiOS-1")
    @TmsLink("TEST2")
    @Description("feature description")
    def "Some test: #testName"() {
        when: "когда проверяем логин"
        specLogin = login
        and: "и пароль"
        specPass = pass
        then: "логин и пароль не должны быть пусты"
        !specLogin.isEmpty() && !specPass.isEmpty()
        and: "длина логина не должна быть меньше 3 символов"
        specLogin.length() >= 3
        and: "длина пароля не должна быть меньше 8 символов"
        specPass.length() >= 8

        when: "когда складываем первое и второе число"
        specSum = firstNum + secondNum
        then: "их сумма должна быть = третьему"
        specSum == thirdNum

        expect: "в A должна быть указана истина"
        A
        and: "в B должна быть указана ложь"
        !B
        and:
        testCall("test") == "test"

        where:
        testName      | login  | pass       | firstNum | secondNum | thirdNum | A    | B
        "first test"  | "test" | "QWERqwer" | 1        | 2         | 3        | true | false
    }

    @Step("Test call: {call}")
    @Attachment
    private static String testCall(String call) {
        return call
    }
}