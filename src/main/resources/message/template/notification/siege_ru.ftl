<#if (data.servers)??>
<#if data.before>
<#assign singleLine = "Через 5 минут начнется осада на *${data.servers[0]} сервере*">
<#assign multiHeader = "Через 5 минут начнется осада на следующих серверах:">
<#else>
<#assign singleLine = "Началась осада на *${data.servers[0]} сервере*">
<#assign multiHeader = "Началась осада на следующих серверах:">
</#if>
<#if data.servers?size == 1>
${singleLine}
<#elseif data.servers?size gt 1>
${multiHeader}
<#list data.servers as server>
*${server} сервер*
</#list>
</#if>
</#if>
