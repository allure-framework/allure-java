<html>
<head>
    <script src="https://yastatic.net/jquery/2.2.3/jquery.min.js" crossorigin="anonymous"></script>

    <link href="https://yastatic.net/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet" crossorigin="anonymous">
    <script src="https://yastatic.net/bootstrap/3.3.6/js/bootstrap.min.js" crossorigin="anonymous"></script>

    <link type="text/css" href="https://yandex.st/highlightjs/8.0/styles/github.min.css" rel="stylesheet"/>
    <script type="text/javascript" src="https://yandex.st/highlightjs/8.0/highlight.min.js"></script>
    <script type="text/javascript" src="https://yandex.st/highlightjs/8.0/languages/bash.min.js"></script>
    <script type="text/javascript" src="https://yandex.st/highlightjs/8.0/languages/json.min.js"></script>
    <script type="text/javascript" src="https://yandex.st/highlightjs/8.0/languages/xml.min.js"></script>
    <script type="text/javascript">hljs.initHighlightingOnLoad();</script>

    <style>
        .nav {
            margin-bottom: 15px;
        }
        pre {
            white-space: pre-wrap;
        }
    </style>
</head>
<body>
<div class="container">
    <div class="row">
        <div class="col-md-12">
            <ul id="tabs" class="nav nav-pills" role="tablist">
                <li role="presentation" class="active">
                    <a aria-expanded="true" aria-controls=request" data-toggle="tab" role="tab" href="#request">
                        Request
                    </a>
                </li>
                <li role="presentation">
                    <a aria-expanded="true" aria-controls="response" data-toggle="tab" role="tab" href="#response">
                        Response
                    </a>
                </li>
                <li role="presentation">
                    <a aria-expanded="true" aria-controls="curl" data-toggle="tab" role="tab" href="#curl">
                        Curl
                    </a>
                </li>
            </ul>
            <div class="tab-content">
                <div id="request" class="tab-pane fade active in">
                    <pre><code>${requestMethod}: ${requestUrl}</code></pre>
                    <h4>Query parameters</h4>
                <#list queryParams?keys as name>
                    <pre>${name}=${queryParams[name]}</pre>
                <#else>
                    <p>No parameters</p>
                </#list>
                    <h4>Headers</h4>
                <#list requestHeaders?keys as name>
                    <pre><code><b>${name}</b>: ${requestHeaders[name]}</code></pre>
                <#else>
                    <p>No headers</p>
                </#list>
                    <h4>Request body</h4>
                <#if requestBody??>
                    <#if requestBody?length == 0>
                        <p>No body</p>
                    <#else>
                        <pre><code>${requestBody}</code></pre>
                    </#if>
                <#else>
                    <p>No body</p>
                </#if>
                </div>
                <div id="response" class="tab-pane fade">
                    <h4>Status</h4>
                    <pre><code><b>${responseStatus}</b></code></pre>
                    <h4>Response headers</h4>
                <#list responseHeaders?keys as name>
                    <pre><code><b>${name}</b>: ${responseHeaders[name]}</code></pre>
                <#else>
                    <p>No headers</p>
                </#list>
                    <h4>Response body</h4>
                <#if responseBody??>
                    <#if responseBody?length == 0>
                        <p>No body</p>
                    <#else>
                        <pre><code>${responseBody}</code></pre>
                    </#if>
                <#else>
                    <p>No body</p>
                </#if>
                </div>
                <div id="curl" class="tab-pane fade">
                <#if curl??>
                    <#if curl?length == 0>
                        <p>No curl</p>
                    <#else>
                        <pre>${curl}</pre>
                    </#if>
                </#if>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>