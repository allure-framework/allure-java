Feature: Shared setup failures

@setup
Scenario: Prepare shared data
  * karate.fail('shared setup failed')

Scenario: Callonce setup failure
  * callonce read('classpath:testdata/global-errors/setup-failure-target.feature')

Scenario: CallSingle setup failure
  * def result = karate.callSingle('classpath:testdata/global-errors/setup-failure-target.feature')

Scenario: Setup scenario failure
  * def result = karate.setup()
