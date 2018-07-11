@FeatureTag @tmsLink=OAT-4444 @flaky @issue=BUG-22400
Feature: Test Scenarios with backgrounds

  Background:
    Given a is 5
    And b is 10

  @good
  Scenario: Scenario with background
    When I add a to b
    Then result is 15

  @bad
  Scenario: Bad scenario with background
    When I add a to b
    Then result is 16
