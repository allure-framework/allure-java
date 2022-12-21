Feature: Simple API tests

#  Background:
#    * url 'https://qameta.testops.cloud/api/login/props'

  Scenario: Simple get request
    * url 'http://localhost:8081'
    * path '/login'
    When method get
    Then status 200

  Scenario: Simple post request
    * url 'http://localhost:8081'
    * path '/login'
    When method post
    Then status 200