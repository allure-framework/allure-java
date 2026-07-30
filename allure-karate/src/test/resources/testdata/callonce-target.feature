Feature: Callonce feature

  Scenario: Callonce scenario
    * eval
      """
      karate.embed('callonce evidence', 'text/plain', 'callonce.txt')
      """
    * match 3 == 3
