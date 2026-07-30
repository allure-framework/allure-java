Feature: Nested called feature

  Scenario: Nested called scenario
    * eval
      """
      karate.embed('nested evidence', 'text/plain', 'nested.txt')
      """
    * match 2 == 2
