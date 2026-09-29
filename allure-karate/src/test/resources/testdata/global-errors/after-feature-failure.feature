Feature: Feature cleanup failure

Background:
  * configure afterFeature = function(){ throw new Error('feature cleanup failed') }

Scenario: First scenario
  * match 1 == 1

Scenario: Second scenario
  * match 2 == 2
