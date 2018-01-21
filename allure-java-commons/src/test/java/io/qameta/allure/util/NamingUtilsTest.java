package io.qameta.allure.util;

import io.qameta.allure.testdata.DummyCard;
import io.qameta.allure.testdata.DummyUser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static io.qameta.allure.util.NamingUtils.processNameTemplate;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author charlie (Dmitry Baev).
 */
@RunWith(Parameterized.class)
public class NamingUtilsTest {

    @Parameterized.Parameter
    public String template;

    @Parameterized.Parameter(1)
    public Map<String, Object> parameters;

    @Parameterized.Parameter(2)
    public String expected;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(
                new Object[]{"Hello word", Collections.emptyMap(), "Hello word"},
                new Object[]{"{missing}", Collections.emptyMap(), "{missing}"},
                new Object[]{"", Collections.singletonMap("a", "b"), ""},
                new Object[]{"Hello {user}!", Collections.singletonMap("user", "Ivan"), "Hello Ivan!"},
                new Object[]{"Hello {user}", Collections.singletonMap("user", null), "Hello null"},
                new Object[]{"Hello {users}", Collections.singletonMap("users", Arrays.asList("Ivan", "Petr")), "Hello [Ivan, Petr]"},
                new Object[]{"Hello {users}", Collections.singletonMap("users", new String[]{"Ivan", "Petr"}), "Hello [Ivan, Petr]"},
                new Object[]{"Hello {users}", Collections.singletonMap("users", Collections.singletonMap("a", "b")), "Hello {a=b}"},
                new Object[]{"Password: {user.password}", Collections.singletonMap("user", new DummyUser(null, "123", null)), "Password: 123"},
                new Object[]{"Passwords: {users.password}", Collections.singletonMap("users", new DummyUser[]{new DummyUser(null, "123", null)}), "Passwords: [123]"},
                new Object[]{"Passwords: {users.password}", Collections.singletonMap("users", new DummyUser[]{null, new DummyUser(null, "123", null)}), "Passwords: [null, 123]"},
                new Object[]{"Passwords: {users.password}", Collections.singletonMap("users", new DummyUser[][]{null, {null, new DummyUser(null, "123", null)}}), "Passwords: [null, [null, 123]]"},
                new Object[]{"Card: {user.card.number}", Collections.singletonMap("user", new DummyUser(null, null, new DummyCard("123"))), "Card: 123"},
                new Object[]{"Password: {user.getPassword()}", Collections.singletonMap("user", new DummyUser(null, "123", null)), "Password: 123"},
                new Object[]{"Card: {user.getCard().number}", Collections.singletonMap("user", new DummyUser(null, null, new DummyCard("123"))), "Card: 123"},
                new Object[]{"Card: {user.getCard().getNumber()}", Collections.singletonMap("user", new DummyUser(null, null, new DummyCard("123"))), "Card: 123"}
        );
    }

    @Test
    public void shouldProcessTemplate() throws Exception {
        final String actual = processNameTemplate(template, parameters);

        assertThat(actual)
                .describedAs("Should process template \"%s\" as \"%s\"", template, expected)
                .isEqualTo(expected);
    }
}