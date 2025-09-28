<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if data.hasServers()>
<#if isEn>
*Resource overview by server*
<#else>
*Список ресурсов по серверамㅤㅤㅤㅤㅤ*
</#if>
<#list data.servers as server>
<#assign statusIcon = server.notifyDisable?then('✅','❌')>
<#if isEn>
${statusIcon}*${server.id} server:* <#if server.main>*👑Main<#else>*Exchange:* <#if server.exchange?has_content>${server.exchange}<#else>Not set</#if></#if>, ${server.draadorCount} <#if server.balance gt 0>(+${server.balance})<#elseif server.balance lt 0>(${server.balance})<#else></#if>🪆, ${server.voidCount} 🟣*<#if server.cbEnabled>, ${server.cbCount}😈</#if>
<#else>
${statusIcon}*${server.id}s: <#if server.main>👑Основной<#else>* Обменник: *<#if server.exchange?has_content>${server.exchange}<#else>Отсутствует</#if></#if>, ${server.draadorCount} <#if server.balance gt 0>(+${server.balance})<#elseif server.balance lt 0>(${server.balance})<#else></#if>🪆, ${server.voidCount} 🟣*<#if server.cbEnabled>, ${server.cbCount}😈</#if>
</#if>
</#list>
<#else>
<#if isEn>
No resource data for any server
<#else>
Отсутствую данные о ресурсах на каком либо сервере
</#if>
</#if>
