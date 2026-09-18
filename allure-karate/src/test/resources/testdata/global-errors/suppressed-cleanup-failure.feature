@report=false
Feature: Suppressed feature cleanup

Background:
  * configure afterFeature = function(){ throw new Error('private-cleanup-value') }

Scenario: Suppressed cleanup failure
  * match 1 == 1
