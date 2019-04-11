Feature: Test Scenarios with multiple examples

  Scenario Outline: Scenario with Positive Examples
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>

    Examples:
      | a | b | result |
      | 1 | 3 | 4      |

    Examples:
      | a | b | result |
      | 2 | 8 | 10     |