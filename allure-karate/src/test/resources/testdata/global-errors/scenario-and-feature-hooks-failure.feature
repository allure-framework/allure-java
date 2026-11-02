Feature: Scenario and feature cleanup failures

Background:
  * configure afterScenario = function(){ throw new Error('scenario cleanup failed') }
  * configure afterFeature = function(){ throw new Error('feature cleanup failed') }

Scenario: Scenario with both cleanup hooks
  * match 1 == 1
