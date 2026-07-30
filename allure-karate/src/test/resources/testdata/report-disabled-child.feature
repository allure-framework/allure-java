Feature: inherited reporting suppression

  Scenario: Confidential inherited child
    * eval karate.embed('private child attachment', 'text/plain', 'private-child.txt')
    * match 'private-child-failure-secret' == 'different'
