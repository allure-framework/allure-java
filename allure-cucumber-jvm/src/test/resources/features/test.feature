@FeatureTag @tmsLink=OAT-4444 @flaky @issue=BUG-22400
Feature: Test One

  This is description for current feature.
  It should appear on each scenario in report

  @good
  Scenario: Add a to b
    Given a is 5
    And b is 10
    When I add a to b
    Then result is 15

  @good @tmsLink= @tmsLink=ISSUE=12345
  Scenario: Scenario with empty tag and tag with more than one =
    Given a is 5
    And b is 10
    When I add a to b
    Then result is 15

  @tmsLink=OAT-219 @severity=blocker @issue=BUG-12312 @known @muted @goofy=dog @melted @link=http://yandex.ru
  Scenario Outline: Outline
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>
    Examples:
      | a | b | result |
      | 1 | 3 | 4      |
      | 2 | 4 | 5      |

  @good @link=http://yandex.ru @link.mylink-112-qwe=mylinkname-12  @link.mylink-112-qwe=12_12-12
  Scenario: data table
    Given users are:
      | name    | login    | email          |
      | Viktor  | clicman  | clicman@ya.ru  |
      | Viktor2 | clicman2 | clicman2@ya.ru |
    And users are:
      | name1   | login1   | email1         |
      | Viktor  | clicman  | clicman@ya.ru  |
      | Viktor2 | clicman2 | clicman2@ya.ru |
