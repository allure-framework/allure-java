Feature: Called feature

  Scenario: Called scenario
    * eval
      """
      karate.embed('called evidence', 'text/plain', 'called.txt')
      """
    * url karate.properties['mock.server.url']
    * path '/called'
    * method get
    * status 200
    * call read('classpath:testdata/nested-call-target.feature')
