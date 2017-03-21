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

  @tmsLink=OAT-219 @severity=blocker @issue=BUG-12312 @known @muted @goofy=dog @melted
  Scenario Outline: Outline
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>
    Examples:
      | a | b | result |
      | 1 | 3 | 4      |
      | 2 | 4 | 5      |