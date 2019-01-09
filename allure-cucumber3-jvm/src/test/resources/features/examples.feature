Feature: Test Scenarios with Examples

  Scenario Outline: Scenario with Positive Examples
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>

    Examples:
      | a | b | result |
      | 1 | 3 | 4      |
      | 2 | 4 | 6      |
