Scenario: First

Given a is 2
And b is 2
When I add a to b
Then result is 4

Scenario: Second

Given a is 3
And b is 7
When I add a to b
Then result is 10

Scenario: Third

Given a is 5
And b is -5
When I add a to b
Then result is 0
