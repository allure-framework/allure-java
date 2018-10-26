package io.qameta.allure.testdata;

import java.util.Arrays;

/**
 * @author sskorol (Sergey Korol)
 */
public class DummyUser {

    private final DummyEmail[] emails;
    private final String password;
    private final DummyCard card;

    public DummyUser(final DummyEmail[] emails, final String password, DummyCard card) {
        this.emails = emails;
        this.password = password;
        this.card = card;
    }

    public DummyEmail[] getEmail() {
        return emails;
    }

    public String getPassword() {
        return password;
    }

    public DummyCard getCard() {
        return card;
    }

    @Override
    public String toString() {
        return "DummyUser{" +
                "emails='" + Arrays.toString(emails) + '\'' +
                ", password='" + password + '\'' +
                ", card=" + card +
                '}';
    }
}
