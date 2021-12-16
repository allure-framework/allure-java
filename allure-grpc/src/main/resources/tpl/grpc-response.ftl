<html>
<#-- @ftlvariable name="data" type="io.qameta.allure.grpc.GrpcResponseAttachment" -->
<head>
    <meta http-equiv="content-type" content="text/html; charset = UTF-8">
    <script src="https://yastatic.net/jquery/2.2.3/jquery.min.js" crossorigin="anonymous"></script>

    <link href="https://yastatic.net/bootstrap/3.3.6/css/bootstrap.min.css" rel="stylesheet" crossorigin="anonymous">
    <script src="https://yastatic.net/bootstrap/3.3.6/js/bootstrap.min.js" crossorigin="anonymous"></script>

    <link type="text/css" href="https://yandex.st/highlightjs/8.0/styles/github.min.css" rel="stylesheet"/>
    <script type="text/javascript" src="https://yandex.st/highlightjs/8.0/highlight.min.js"></script>
    <script type="text/javascript" src="https://yandex.st/highlightjs/8.0/languages/json.min.js"></script>
    <script type="text/javascript">hljs.initHighlightingOnLoad();</script>

    <style>
        pre {
            white-space: pre-wrap;
        }
    </style>
</head>
<body>

<div><h4>Status</h4>
    <#if data.status??>
        <pre><code><b>${data.status}</b></code></pre>
    <#else>Unknown</#if>
</div>

<#if data.body??>
    <h4>Response body</h4>
    <div>
        <pre><code>${data.body}</code></pre>
    </div>
</#if>

<#if (data.metadata)?has_content>
    <h4>Metadata</h4>
    <div>
        <#list data.metadata as key, value>
            <div>${key}: ${value}</div>
        </#list>
    </div>
</#if>

</body>
</html>
