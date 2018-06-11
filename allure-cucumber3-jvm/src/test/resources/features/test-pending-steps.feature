Feature: Test Scenarios with Pending Steps

  @skipped
  Scenario: Scenario with unimplementd steps
    Given первое число 1
    When call unimplemented step
    And второе число 2
    Then test case market as unimplemented

  @skipped
  Scenario: Scenario with pending steps
    Given первое число 1
    When call step with PendingException
    And второе число 2
    Then test case market as unimplemented