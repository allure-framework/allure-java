@allure.label.parentSuite:Cucumber
@allure.label.suite:Petstore
@allure.label.subSuite:CRUD
Feature: Arithmetic_operations

  Scenario Outline: Addition
    Given a is <a>
    And b is <b>
    When I add a to b
    Then result is <result>

    Examples:
      | a | b | result |
      | 1 | 1 | 2      |
      | 2 | 1 | 3      |
      | 2 | 7 | 9      |
