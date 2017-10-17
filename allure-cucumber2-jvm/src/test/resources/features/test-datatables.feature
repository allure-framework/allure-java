@FeatureTag @tmsLink=OAT-4444 @flaky @issue=BUG-22400
Feature: Test Scenarios with Data Tables

  @good @link=http://yandex.ru @link.mylink-112-qwe=mylinkname-12  @link.mylink-112-qwe=12_12-12
  Scenario: Simple data table
    Given users are:
      | name    | login    | email          |
      | Viktor  | clicman  | clicman@ya.ru  |
      | Viktor2 | clicman2 | clicman2@ya.ru |
    And users are:
      | name1   | login1   | email1         |
      | Viktor  | clicman  | clicman@ya.ru  |
      | Viktor2 | clicman2 | clicman2@ya.ru |

  Scenario: Steps with argumets and data tables
    Given users are:
      | name    | login    | email          |
      | Viktor  | clicman  | clicman@ya.ru  |
      | Viktor2 | clicman2 | clicman2@ya.ru |
    And step with argument 5 and data table:
      | name1   | login1   | email1         |
      | Viktor  | clicman  | clicman@ya.ru  |
      | Viktor2 | clicman2 | clicman2@ya.ru |
