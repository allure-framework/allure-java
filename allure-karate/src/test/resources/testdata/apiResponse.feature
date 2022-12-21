Feature: API tests

  Scenario: Get request with response body
    Given path 'https://mysite.example.api.com/users'
    When method get
    Then status 200
    And match response == [{ id: '1', name: 'Soul' }, { id: '2', name: 'Kate' }]

  Scenario: Post request with response body
    Given path 'https://mysite.example.api.com/users/login'
    When method post
    Then status 200
    And match response == { message: 'User logged in', error: null }