@FeatureTag @tmsLink=OAT-4444 @flaky @issue=BUG-22400
Feature: Test Scenarios with Examples

  @good @tmsLink=OAT-219 @severity=blocker @issue=BUG-12312 @known @muted @goofy=dog @melted @link=http://yandex.ru
  Scenario Outline: Scenario with Positive Examples
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>
    Examples:
      | a | b | result |
      | 1 | 3 | 4      |
      | 2 | 4 | 6      |

  @good @tmsLink=OAT-219 @severity=blocker @issue=BUG-12312 @known @muted @goofy=dog @melted @link=http://yandex.ru
  Scenario Outline: Scenario with Arguments in description: <a> and <b> == <result>
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>
    Examples:
      | a | b | result |
      | 1 | 3 | 4      |
      | 2 | 4 | 6      |

  @bad @tmsLink=OAT-219 @severity=blocker @issue=BUG-12312 @known @muted @goofy=dog @melted @link=http://yandex.ru
  Scenario Outline: Scenario with Negative Examples
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>
    Examples:
      | a | b | result |
      | 1 | 3 | 6      |
      | 2 | 4 | 1      |
