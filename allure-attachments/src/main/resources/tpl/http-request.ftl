<#-- @ftlvariable name="data" type="io.qameta.allure.attachment.http.HttpRequestAttachment" -->
<h2>${data.name}</h2>

<h3>Request</h3>
<div>${data.method} to ${data.url}</div>

<#if data.body??>
<h3>Body</h3>
<div>
    <pre>
    ${data.body}
    </pre>
</div>
</#if>

<#if data.headers??>
<h3>Headers</h3>
<div>
    <#list data.headers as name, value>
        <div>${name}: ${value}</div>
    </#list>
</div>
</#if>


<#if data.cookies??>
<h3>Cookies</h3>
<div>
    <#list data.cookies as name, value>
        <div>${name}: ${value}</div>
    </#list>
</div>
</#if>

<h3>Curl</h3>
<div>
${data.curl}
</div>
