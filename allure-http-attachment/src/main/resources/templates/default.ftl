# Request

`${requestMethod}: ${requestUrl}`

## Query params
<#if queryParams??>
<#list queryParams?keys as name>
* ${name}=${queryParams[name]}
<#else>
No parameters
</#list>
</#if>

## Headers
<#if requestHeaders??>
<#list requestHeaders?keys as name>
* ${name}: ${requestHeaders[name]}
<#else>
No headers
</#list>
</#if>

## Request body
<#if requestBody??>
<#if requestBody?length == 0>
No body
<#else>
`${requestBody}`
</#if>
<#else>
No body
</#if>

# Response

## Status
<#if responseStatus??>
`${responseStatus}`
<#else>
No status
</#if>

## Response headers
<#if responseHeaders??>
<#list responseHeaders?keys as name>
* ${name}: ${responseHeaders[name]}
<#else>
No headers
</#list>
</#if>

## Response body
<#if responseBody??>
<#if responseBody?length == 0>
No body
<#else>
${responseBody}
</#if>
<#else>
No body
</#if>

# Curl
${toCurl()}
