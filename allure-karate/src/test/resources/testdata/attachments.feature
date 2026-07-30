Feature: embedded attachments

  Scenario: Named attachments
    * eval
      """
      karate.embed('plain attachment', 'text/plain', 'notes.txt');
      karate.embed('{"status":"ok"}', 'application/json', 'payload.json');
      """
