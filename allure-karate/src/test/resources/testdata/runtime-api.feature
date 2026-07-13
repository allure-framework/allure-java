Feature: Runtime API parameters

  Scenario Outline: Runtime parameters for <native>
    * def Allure = Java.type('io.qameta.allure.Allure')
    * def runtime = Allure.parameter('runtime', 'value')
    * def excluded = Allure.parameter('excluded', 'ignored', true)

    Examples:
      | native  |
      | example |
