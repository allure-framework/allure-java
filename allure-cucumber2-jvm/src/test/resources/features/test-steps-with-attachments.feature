@FeatureTag @tmsLink=OAT-4444 @flaky @issue=BUG-22400
Feature: Test Scenarios with Attachments and Steps

  @good
  Scenario: Scenario which has steps with attachments
    When I attach picture to step
    Then it is displayed in report

  @good
  Scenario: Scenario which has steps with attachments
    When I execute steps with @Step
    Then it is displayed in report