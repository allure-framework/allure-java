package io.qameta.allure.testdata;

/**
 * @author sskorol (Sergey Korol)
 */
public class DummyCard {

    private final String number;

    public DummyCard(final String number) {
        this.number = number;
    }

    public String getNumber() {
        return number;
    }

    @Override
    public String toString() {
        return "DummyCard{" +
                "number='" + number + '\'' +
                '}';
    }
}
