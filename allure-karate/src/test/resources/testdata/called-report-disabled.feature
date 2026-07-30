Feature: explicitly suppressed called feature

  @report=false
  Scenario: Confidential called failure
    * eval karate.embed('called private attachment', 'text/plain', 'called-private.txt')
    * match 'called-private-failure-secret' == 'different'
