Scenario: Add a to b
GivenStories:stories/precondition-a.story,stories/precondition-b.story

When I add a to b
Then result is 15
