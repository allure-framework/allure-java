Feature: attachments

  Background:
    * configure driver = { type: 'chrome', timeout: 5000, screenshotOnFailure: true, showDriverLog: true }

  Scenario: Screenshot attachment
    Given driver 'https://docs.qameta.io/allure-testops/'
    Then match true == false