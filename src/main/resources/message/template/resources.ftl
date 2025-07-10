<#if data.hasServers()>
*Список ресурсов по серверам*
<#list data.servers as server>
*${server.id}s:* Обменник: *<#if server.exchange?has_content>${server.exchange}<#else>Отсутствует</#if> ${server.draadorCount}🪆, ${server.voidCount} 🟣*,
</#list>
<#else>
Отсутствую данные о ресурсах на каком либо сервере
</#if>
