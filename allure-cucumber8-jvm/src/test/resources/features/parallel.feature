Feature: Parallel execution of pickles

  Scenario Outline: Simple scenario outline with examples
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>
    Examples:
      | a | b | result |
      | 1 | 3 | 4      |
      | 2 | 4 | 6      |

  Scenario: Simple scenario
    Given a is 7
    And b is 8
    When I add a to b
    Then result is 15
