@report=false
Feature: Suppressed example preparation

Scenario Outline: Suppressed prepared example
  * match value == 1

Examples:
  | (function(){ throw new Error('private-preparation-value') })() |
