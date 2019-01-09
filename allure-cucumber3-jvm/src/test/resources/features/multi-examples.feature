Feature: Test Scenarios with multiple Examples

  Scenario Outline: Scenario with Multiple Examples
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>

    @ExamplesTag1
    Examples:
      | a | b | result |
      | 1 | 3 | 4      |

    @ExamplesTag2
    Examples:
      | a | b | result |
      | 2 | 4 | 6      |