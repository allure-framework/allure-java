@FeatureTag @tmsLink=OAT-4444 @flaky @issue=BUG-22400
Feature: Test Scenarios with Attachments and Steps

  @good
  Scenario: Simple Image attachment using Allure.addAttachmnet
    When I attach picture to step
    Then it is displayed in report

  @good
  Scenario: Step with sub step @Step and @Attachment
    When I attach file in sub-step
    Then it is displayed in report

  @good
  Scenario: Step with sub step @Step
    When I execute steps with @Step
    Then it is displayed in report