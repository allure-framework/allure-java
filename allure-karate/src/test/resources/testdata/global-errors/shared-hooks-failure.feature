Feature: Shared cleanup hooks

Background:
  * configure afterScenarioOutline = function(){ throw new Error('outline cleanup failed') }
  * configure afterFeature = function(){ throw new Error('feature cleanup failed') }

Scenario Outline: Example <value>
  * match value == '#number'

Examples:
  | value |
  | 1     |
  | 2     |
