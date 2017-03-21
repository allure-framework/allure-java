#language: en
@third @good
Feature: Text allure 3

  Scenario: Scenario
    Given Anything in given with dots. This is an example
    When whe run the scenario 
    Then scenario name shuld be complete

  Scenario Outline: Scenario Structure
    Given An URL <URL_First>
    And another URL <URL_Second>
    When whe concatenate it
    Then Result should be <URL_Result>

    Examples: 
      | URL_First              | URL_Second             | URL_Result                                 |
      | http://www.google.es   | http://www.leda-mc.com | http://www.google.eshttp://www.leda-mc.com |
      | http://www.leda-mc.com | http://www.google.es   | http://www.leda-mc.comhttp://www.google.es |
