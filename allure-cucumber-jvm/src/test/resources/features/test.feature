Feature: Test One

  Scenario: Add a to b
    Given a is 5
    And b is 10
    When I add a to b
    Then result is 15