@smoke
Feature: tags demo - first
    run the following example from the command line:
    mvn test -Dcucumber.options="--tags @smoke" -Dtest=TagsRunner

Scenario: f1 - s1
    * print 'first feature:@smoke, first scenario'
    * url 'http://localhost:8081'
    * path '/login'
    When method get
    # comment
    Then status 200

@fire
Scenario: f1 - s2
    #* print 'first feature:@smoke, second scenario:@fire'
