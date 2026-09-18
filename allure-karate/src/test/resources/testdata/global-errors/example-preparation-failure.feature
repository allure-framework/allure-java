Feature: Example preparation failure

Scenario Outline: Prepared example
  * match value == 1

Examples:
  | (function(){ throw new Error('example preparation failed') })() |
