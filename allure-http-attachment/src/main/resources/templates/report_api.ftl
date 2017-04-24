<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en" lang="en">
<head>
    <meta http-equiv="content-type" content="text/html; charset=utf-8">
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
        html, body, div, span, applet, object, iframe,
        a, abbr, acronym, address, big, cite,
        del, dfn, em, font, img, ins, kbd, q, s, samp,
        small, strike, strong, sub, sup, tt, var,
        b, u, i, center, a,
        dl, dt, dd, ol, ul, li,
        fieldset, form, label, legend,
        table, caption, tbody, tfoot, thead, tr, th, td {
            margin: 0;
            padding: 0;
            border: 0;
            outline: 0;
            font-size: 100%;
            vertical-align: baseline;
            background: transparent;
        }

        body {
            font: 14px/20px Arial, Tahoma, Verdana, sans-serif;
        }

        pre {
            white-space: pre-wrap;
        }

        ol, ul {
            list-style: none;
        }

        :focus {
            outline: 0;
        }

        ins {
            text-decoration: none;
        }

        del {
            text-decoration: line-through;
        }

        table {
            border-collapse: collapse;
            border-spacing: 0;
        }

        header, nav, section, article, aside, footer {
            display: block;
        }

        body {
            font: 14px/20px Arial, Tahoma, Verdana, sans-serif;
        }

        a {
            color: #555;
            outline: none;
            text-decoration: none;
        }

        a:hover {
            text-decoration: none;
        }

        p {
            margin: 0 0 18px
        }


        input {
            vertical-align: middle;
        }

        /* TABS */
        .tabs {
            position: relative;
            margin: 0 auto;
        }

        .tabs label {
            display: block;
            float: left;
            background: #ffffff;
            background: url(data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiA/Pgo8c3ZnIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyIgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgdmlld0JveD0iMCAwIDEgMSIgcHJlc2VydmVBc3BlY3RSYXRpbz0ibm9uZSI+CiAgPGxpbmVhckdyYWRpZW50IGlkPSJncmFkLXVjZ2ctZ2VuZXJhdGVkIiBncmFkaWVudFVuaXRzPSJ1c2VyU3BhY2VPblVzZSIgeDE9IjAlIiB5MT0iMCUiIHgyPSIwJSIgeTI9IjEwMCUiPgogICAgPHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2ZmZmZmZiIgc3RvcC1vcGFjaXR5PSIxIi8+CiAgICA8c3RvcCBvZmZzZXQ9IjQlIiBzdG9wLWNvbG9yPSIjZWZmMGY0IiBzdG9wLW9wYWNpdHk9IjEiLz4KICAgIDxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2RkZGVlMCIgc3RvcC1vcGFjaXR5PSIxIi8+CiAgPC9saW5lYXJHcmFkaWVudD4KICA8cmVjdCB4PSIwIiB5PSIwIiB3aWR0aD0iMSIgaGVpZ2h0PSIxIiBmaWxsPSJ1cmwoI2dyYWQtdWNnZy1nZW5lcmF0ZWQpIiAvPgo8L3N2Zz4=);
            background: -moz-linear-gradient(top, #ffffff 0%, #eff0f4 4%, #dddee0 100%);
            background: -webkit-gradient(linear, left top, left bottom, color-stop(0%, #ffffff), color-stop(4%, #eff0f4), color-stop(100%, #dddee0));
            background: -webkit-linear-gradient(top, #ffffff 0%, #eff0f4 4%, #dddee0 100%);
            background: -o-linear-gradient(top, #ffffff 0%, #eff0f4 4%, #dddee0 100%);
            background: -ms-linear-gradient(top, #ffffff 0%, #eff0f4 4%, #dddee0 100%);
            background: linear-gradient(to bottom, #ffffff 0%, #eff0f4 4%, #dddee0 100%);
            filter: progid:DXImageTransform.Microsoft.gradient(startColorstr='#ffffff', endColorstr='#dddee0', GradientType=0);
            -moz-border-radius: 6px 6px 0 0;
            -webkit-border-radius: 6px 6px 0 0;
            border-radius: 6px 6px 0 0;
            border-right: 1px solid #f3f3f3;
            border-left: 1px solid #ccc;
            color: #555;
            cursor: pointer;
            font-weight: bold;
            font-size: 15px;
            position: relative;
            top: 2px;
            width: 150px;
            height: 45px;
            line-height: 45px;
            text-align: center;
            text-transform: uppercase;
            text-shadow: #fff 0 1px 0;
            z-index: 1;
        }

        .tabs input {
            position: absolute;
            left: -9999px;
        }

        #request:checked ~ #request_l,
        #response:checked ~ #response_l,
        #curl:checked ~ #curl_l {
            background: #fff;
            border-color: #fff;
            top: 0;
            z-index: 3;
        }

        .tabs_cont {
            background: #fff;
            border-radius: 0 6px 6px 6px;
            padding: 20px 25px;
            position: relative;
            z-index: 2;
        }

        .tabs_cont > div {
            position: absolute;
            left: -9999px;
            top: 0;
            opacity: 0;
            -moz-transition: opacity .5s ease-in-out;
            -webkit-transition: opacity .5s ease-in-out;
            transition: opacity .5s ease-in-out;
        }

        #request:checked ~ .tabs_cont #request_c,
        #response:checked ~ .tabs_cont #response_c,
        #curl:checked ~ .tabs_cont #curl_c {
            position: static;
            left: 0;
            opacity: 1;
        }
    </style>
</head>
<body>
<section class="tabs">
    <input id="request" type="radio" name="tab" checked="checked"/>
    <input id="response" type="radio" name="tab"/>
    <input id="curl" type="radio" name="tab"/>
    <label for="request" id="request_l">Request</label>
    <label for="response" id="response_l">Response</label>
    <label for="curl" id="curl_l">Curl</label>
    <div style="clear:both"></div>

    <div class="tabs_cont">
        <div id="request_c">
            <pre><code>${requestMethod}: ${requestUrl}</code></pre>
            <h4>Query parameters</h4>
        <#if queryParams??>
            <#list queryParams?keys as name>
                <pre>${name}=${queryParams[name]}</pre>
            <#else>
                <p>No parameters</p>
            </#list>
            <h4>Headers</h4>
        </#if>
        <#if requestHeaders??>
            <#list requestHeaders?keys as name>
                <pre><code><b>${name}</b>: ${requestHeaders[name]}</code></pre>
            <#else>
                <p>No headers</p>
            </#list>
            <h4>Request body</h4>
        </#if>
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
        <div id="response_c">
            <h4>Status</h4>
        <#if responseStatus??>
            <pre><code><b>${responseStatus}</b></code></pre>
        <#else>
            <p>No status</p>
        </#if>
            <h4>Response headers</h4>
        <#if responseHeaders??>
            <#list responseHeaders?keys as name>
                <pre><code><b>${name}</b>: ${responseHeaders[name]}</code></pre>
            <#else>
                <p>No headers</p>
            </#list>
            <h4>Response body</h4>
        </#if>
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
        <div id="curl_c">
        <#if curl??>
            <#if curl?length == 0>
                <p>No curl</p>
            <#else>
                <pre>${curl}</pre>
            </#if>
        </#if>
        </div>
    </div>
</section>
</body>
</html>