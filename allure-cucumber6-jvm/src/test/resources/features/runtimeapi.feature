@beforeFeature
Feature: Should support runtime API in all steps

  @beforeScenario
  Scenario: Scenario with Runtime API usage
    When step 1
    When step 2
    And step 3
    Then step 4
    And step 5
