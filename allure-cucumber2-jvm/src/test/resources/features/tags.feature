@FeatureTag
@tmsLink=OAT-4444
@flaky
@issue=BUG-22400
Feature: Test Simple Scenarios

  @good
  Scenario: Add a to b
    Given a is 5
    And b is 10
    When I add a to b
    Then result is 15

