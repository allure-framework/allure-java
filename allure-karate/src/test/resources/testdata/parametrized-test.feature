Feature: Parameterized tests

  Scenario Outline: /<path> should return <status>
    * url 'http://localhost:8081'
    Given path '/<path>'
    When method get
    Then status <status>

    Examples:
      | path   | status  |
      | login  | 200     |
      | user   | 301     |
      | pages  | 404     |