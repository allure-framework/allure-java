Feature: Test One

  Scenario: Add a to b
    Given a is 5
    And b is 10
    When I add a to b
    Then result is 15

  @flaky @tmsLink=OAT219 @severity=blocker @issue=12312 @known @muted @goofy=dog @melted
  Scenario Outline: Outline
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>
    Examples:
    |a|b|result|
    |1|3|4     |
    |2|4|5     |