Feature: parallel evidence

  Scenario: Parallel one
    * eval karate.embed('payload-one', 'text/plain', 'parallel-one.txt')
    * url karate.properties['mock.server.url']
    * path '/parallel/one'
    * method get
    * status 200

  Scenario: Parallel two
    * eval karate.embed('payload-two', 'text/plain', 'parallel-two.txt')
    * url karate.properties['mock.server.url']
    * path '/parallel/two'
    * method get
    * status 200

  Scenario: Parallel three
    * eval karate.embed('payload-three', 'text/plain', 'parallel-three.txt')
    * url karate.properties['mock.server.url']
    * path '/parallel/three'
    * method get
    * status 200

  Scenario: Parallel four
    * eval karate.embed('payload-four', 'text/plain', 'parallel-four.txt')
    * url karate.properties['mock.server.url']
    * path '/parallel/four'
    * method get
    * status 200
