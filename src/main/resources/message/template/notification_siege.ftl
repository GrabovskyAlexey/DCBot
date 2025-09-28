<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if (data.servers)??>
<#if data.before>
<#if isEn>
<#assign singleLine = "A siege will start in 5 minutes on *${data.servers[0]} server*">
<#assign multiHeader = "Sieges will start in 5 minutes on the following servers:">
<#else>
<#assign singleLine = "Через 5 минут начнется осада на *${data.servers[0]} сервере*">
<#assign multiHeader = "Через 5 минут начнется осада на следующих серверах:">
</#if>
<#else>
<#if isEn>
<#assign singleLine = "A siege has started on *${data.servers[0]} server*">
<#assign multiHeader = "Sieges have started on the following servers:">
<#else>
<#assign singleLine = "Началась осада на *${data.servers[0]} сервере*">
<#assign multiHeader = "Началась осада на следующих серверах:">
</#if>
</#if>
<#if data.servers?size == 1>
${singleLine}
<#elseif data.servers?size gt 1>
${multiHeader}
<#list data.servers as server>
<#if isEn>
*${server} server*
<#else>
*${server} сервер*
</#if>
</#list>
</#if>
</#if>