<#if (data.servers)??>
<#if data.before>
<#assign prefix ="Через 5 минут начнеться">
<#else>
<#assign prefix ="Началась">
</#if>
<#if data.servers?size == 1>
${prefix} осада на *${data.servers[0]} сервере*
<#elseif data.servers?size gt 1>
${prefix} осада на следующих серверах:
<#list data.servers as server>
*${server} сервер*
</#list>
</#if>
</#if>
