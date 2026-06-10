Feature: API tests

  Scenario: Get request with response body
    Given url karate.properties['mock.server.url']
    And path '/users'
    When method get
    Then status 200
    And match response == [{ id: '1', name: 'Soul' }, { id: '2', name: 'Kate' }]

  Scenario: Post request with response body
    Given url karate.properties['mock.server.url']
    And path '/users/login'
    When method post
    Then status 200
    And match response == { message: 'User logged in', error: null }
