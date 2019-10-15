<#ftl output_format="HTML">
<#-- @ftlvariable name="data" type="io.qameta.allure.attachment.http.HttpRequestAttachment" -->
<div><#if data.method??>${data.method}<#else>GET</#if> to <#if data.url??>${data.url}<#else>Unknown</#if></div>

<#if data.time??>
    <h4>Request time</h4>
    <div>
        ${data.time}
    </div>
</#if>

<#if data.body??>
<h4>Body</h4>
<div>
    <pre class="preformated-text">
    ${data.body}
    </pre>
</div>
</#if>

<#if (data.headers)?has_content>
<h4>Headers</h4>
<div>
    <#list data.headers as name, value>
        <div>${name}: ${value}</div>
    </#list>
</div>
</#if>


<#if (data.cookies)?has_content>
<h4>Cookies</h4>
<div>
    <#list data.cookies as name, value>
        <div>${name}: ${value}</div>
    </#list>
</div>
</#if>

<#if data.curl??>
<h4>Curl</h4>
<div>
${data.curl}
</div>
</#if>
