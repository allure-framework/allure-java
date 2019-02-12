Feature: Hooks variations

  @bp @af @bp_sf_af
  Scenario: Scenario with good before and bad after but failed
    Given a is 1
    And b is 1
    When I add a to b
    Then result is 3


  @bf @ap @bf_ap
  Scenario: Scenario with bad before and good after
    Given a is 1
    And b is 1
    When I add a to b
    Then result is 2


  @bf @af @bf_af
  Scenario: Scenario with bad before and bad after
    Given a is 1
    And b is 1
    When I add a to b
    Then result is 2


  @bp @af @bp_af
  Scenario: Scenario with bad after
    Given a is 1
    And b is 1
    When I add a to b
    Then result is 2


  @bp @ap @bp_sf_ap
  Scenario: Scenario with good before and good after but failed
    Given a is 1
    And b is 1
    When I add a to b
    Then result is 3


  @bp @af @bp_su_af
  Scenario: Step is not implemented with bad after
    Given hello my friend


  @bp @ap @bp_su_ap
  Scenario: Step is not implemented
    Given hello my friend


  @bp @ap @bp_suf_ap
  Scenario: Step is not implemented
    Given hello my friend
    Given a is 1
    And b is 1
    When I add a to b
    Then result is 3


  @bp @ap @bp_spu_ap
  Scenario: Step is not implemented
    Given a is 1
    And b is 1
    When I add a to b
    Then result is 2
    Given hello my friend


  @bp @ap @bp_sfu_ap
  Scenario: Step is not implemented
    Given a is 1
    And b is 1
    When I add a to b
    Then result is 3
    Given hello my friend
