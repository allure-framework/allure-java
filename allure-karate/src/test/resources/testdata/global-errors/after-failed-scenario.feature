Feature: Cleanup after a failed scenario

Background:
  * configure afterFeature = function(){ throw new Error('cleanup after failure') }

Scenario: Failing scenario
  * match 1 == 2
