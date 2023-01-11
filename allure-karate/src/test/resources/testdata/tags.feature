Feature: labels

  @allure.label.epic:epic1
  @allure.label.story:story1
  @allure.label.tag:some_tag
  Scenario: Test with labels
    * print 'First step'
    * print 'Second step'

  @allure.id:141413
  @allure.label.owner:npolly
  @allure.label.layer:unit_tests
  @allure.severity:blocker
  Scenario: Test with owner, id and layer
    * print 'First step'
    * print 'Second step'

  @Karate_tag:1
  Scenario: Test without allure labels
    * print 'First step'
    * print 'Second step'
