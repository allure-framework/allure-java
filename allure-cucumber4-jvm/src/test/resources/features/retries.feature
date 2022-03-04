Feature: Retries feature

  Scenario: A flaky test that is retried
    Given a flaky test
    When the test is executed
    Then the test only passes on the third run.

  Scenario: A test with flaky given
    Given a flaky given
    When the test is executed
    Then the test only passes on the third run.

  @RetrySkipped
  Scenario: A test with a flaky broken before
    Given a flaky test
    When the test is executed
    Then the test only passes on the third run.