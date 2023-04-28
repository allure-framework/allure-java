Feature: API tests for AT

  Scenario: Some api* request # comment 1
  Request '//user' & get 20* code, ...
  # comment 2
    * print 'First step'

  Scenario:
    # comment 3
    * print 'Second step'
