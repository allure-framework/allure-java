Feature: HTTP attachment masking

  Scenario: Configured HTTP mask
    * configure logging =
      """
      {
        mask: {
          headers: ['X-Api-Key'],
          jsonPaths: ['$.customSecret'],
          patterns: [{ regex: 'Soul', replacement: 'MASKED-NAME' }],
          replacement: '[MASKED]',
          enableForUri: function(uri) { return uri.indexOf('/users/login') > -1 }
        }
      }
      """
    * url karate.properties['mock.server.url']
    * path '/users/login'
    * header X-Api-Key = 'private-api-key'
    * request { username: 'Soul', customSecret: 'private-body-value' }
    * method post
    * status 200
