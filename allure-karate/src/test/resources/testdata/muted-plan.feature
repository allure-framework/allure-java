Feature: muted execution

@allure.id:123
Scenario Outline: muted row
  * assert false

Examples:
  | value |
  | one   |
  | two   |

Scenario: control
  * match 1 == 1
