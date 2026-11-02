Feature: Cleanup after a suppressed called failure

Background:
  * configure afterFeature = function(){ throw new Error('private-caller-cleanup-value') }

Scenario: Caller with suppressed failure and cleanup
  * call read('classpath:testdata/called-report-disabled.feature')
