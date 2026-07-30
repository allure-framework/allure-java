Feature: failed-step attachment

  Scenario: Failed step attachment
    * eval
      """
      karate.embed('failure context', 'text/plain', 'failure-context.txt');
      throw new Error('expected failure');
      """
