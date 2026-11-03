Feature: Test Scenarios with backgrounds

  Background:
    Given cat is sad
    And cat is murmur

  Scenario: Scenario with background
    When Pet the cat
    Then Cat is happy
