Feature: Call & Call once Feature
  This feature calls another feature and demonstrates Allure reporting issue.

  @smoke
  Scenario: Main Scenario with a call
    Given url 'https://jsonplaceholder.typicode.com'
    When method GET
    Then status 200

    * call read('classpath:testdata/apiResponse.feature')
    * callonce read('classpath:testdata/api.feature')

    Then print 'Main scenario completed.'
