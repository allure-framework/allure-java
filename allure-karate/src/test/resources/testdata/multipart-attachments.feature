Feature: multipart attachments

  Scenario: Rich multipart attachments
    * eval
      """
      karate.embed({
        name: 'visual comparison',
        parts: [
          { role: 'baseline', mime: 'image/png', data: 'baseline-bytes' },
          { role: 'current', mime: 'image/png', data: 'current-bytes' },
          { role: 'diff', mime: 'image/png', data: 'diff-bytes' }
        ],
        meta: { threshold: 0.1 }
      });
      karate.embed({
        name: 'multi evidence',
        parts: [
          { role: 'request', mime: 'text/plain', data: 'request evidence' },
          { role: 'reference', mime: 'image/png', url: 'ext/image/reference.png' }
        ],
        meta: { source: 'karate extension' }
      });
      """
