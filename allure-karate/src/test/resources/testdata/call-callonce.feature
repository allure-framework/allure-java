Feature: Call & Call once Feature
  This feature calls other passing features and keeps their evidence under one result.

  @smoke
  Scenario: Main Scenario with a call
    * match 1 == 1
    * call read('classpath:testdata/call-target.feature')
    * callonce read('classpath:testdata/callonce-target.feature')
    * print 'Main scenario completed.'
