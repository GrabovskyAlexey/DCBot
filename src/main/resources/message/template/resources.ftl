<#if data.hasServers()>
*Список ресурсов по серверам*
<#list data.servers as server>
*${server.id}s:* Обменник: *<#if server.exchange?has_content>${server.exchange}<#else>Отсутствует</#if>, ${server.draadorCount} <#if server.balance gt 0>(+${server.balance})<#elseif server.balance lt 0>(${server.balance})<#else></#if>🪆, ${server.voidCount} 🟣*,
</#list>
<#else>
Отсутствую данные о ресурсах на каком либо сервере
</#if>
