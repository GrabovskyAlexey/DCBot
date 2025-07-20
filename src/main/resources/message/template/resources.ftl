<#if data.hasServers()>
*Список ресурсов по серверамㅤㅤㅤㅤㅤ*
<#list data.servers as server>
<#if server.notifyDisable>✅<#else>❌</#if>*${server.id}s:* Обменник: *<#if server.exchange?has_content>${server.exchange}<#else>Отсутствует</#if>, ${server.draadorCount} <#if server.balance gt 0>(+${server.balance})<#elseif server.balance lt 0>(${server.balance})<#else></#if>🪆, ${server.voidCount} 🟣*<#if server.cbEnabled>, ${server.cbCount}😈</#if>
</#list>
<#else>
Отсутствую данные о ресурсах на каком либо сервере
</#if>
