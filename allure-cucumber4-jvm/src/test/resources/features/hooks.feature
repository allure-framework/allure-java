Feature: Scenarios with hooks

  @WithHooks
  Scenario: Simple scenario with Before and After hooks
    Given a is 7
    And b is 8
    When I add a to b
    Then result is 15

  @BeforeHookWithException
  Scenario: Simple scenario with Before hook with Exception
    Given a is 7
    And b is 8
    When I add a to b
    Then result is 15

  @AfterHookWithException
  Scenario: Simple scenario with After hook with Exception
    Given a is 7
    And b is 8
    When I add a to b
    Then result is 15