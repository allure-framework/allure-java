Feature: HTTP attachments

  Scenario: HTTP request and response attachment
    * url karate.properties['mock.server.url']
    * path '/users/login'
    * header X-Request-Id = 'karate-http-attachment'
    * header Authorization = 'Bearer secret'
    * request { username: 'Soul' }
    When method post
    Then status 200
    And match response == { message: 'User logged in', error: null }
