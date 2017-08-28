Feature: Test Allure Results generation

  @good
  Scenario: scenario with safe name
    Given scenario name is scenario with safe name
    Then scenario should pass == true

  @good
  Scenario: scenario with unsafe <>:"/\|?* name and /
    Given scenario name is scenario with unsafe <>\:"/\|?* name and /
    Then scenario should pass == true

  @good
  Scenario: сценарий с кириллическим именем
    Given scenario name is сценарий с кириллическим именем
    Then scenario should pass == true

  @good
  Scenario Outline: If previous scenarios were executed then result file should have been saved
    When looking for results in build/allure-results/
    Then result file with name should be present: <FILENAME>

    Examples:
    | FILENAME                                                                                            |
    | test-allure-results-generation_scenario-with-safe-name-([a-zA-Z0-9]+)-result.json                   |
    | test-allure-results-generation_scenario-with-unsafe-_________-name-and-_-([a-zA-Z0-9]+)-result.json |
    | test-allure-results-generation_сценарий-с-кириллическим-именем-([a-zA-Z0-9]+)-result.json           |