Scenario: Add elements to empty stack

Given an empty stack
When I add 4 elements
Then the stack should have 2 elements

Scenario: Clear stack

Given a stack with elements
When I clear stack
Then the stack should have 0 elements