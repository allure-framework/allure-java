Feature: Descriptions feature

This is description for current feature.
It should appear on each scenario in report

  Scenario: Add a to b (1)
    Given a is 5
    And b is 10
    When I add a to b
    Then result is 15

  Scenario: Add a to b (2)
    Given a is 1
    And b is 2
    When I add a to b
    Then result is 3

