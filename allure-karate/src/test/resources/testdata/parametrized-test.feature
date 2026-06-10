Feature: Parameterized tests

  Scenario Outline: /<path> should return <status>
    * url karate.properties['mock.server.url']
    Given path '/<path>'
    When method get
    Then status <status>

    Examples:
      | path   | status  |
      | login  | 200     |
      | user   | 301     |
      | pages  | 404     |
