Feature: reporting suppression

  @report=false
  Scenario: Suppressed failure
    This confidential description must not be reported.
    * eval karate.embed('private attachment', 'text/plain', 'private.txt')
    * url karate.properties['mock.server.url']
    * path '/users/login'
    * header X-Private = 'private-header-secret'
    * request { privateValue: 'private-body-secret' }
    * method post
    * call read('classpath:testdata/report-disabled-child.feature')
