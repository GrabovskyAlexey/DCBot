<#assign lang = (locale?string?lower_case)!"">
<#assign isEn = lang?starts_with('en')>
<#assign name = (data?if_exists.username)!"">
<#if name?has_content>
  <#if isEn>
Hello, *${name}*!
  <#else>
Привет, *${name}*!
  </#if>
<#else>
  <#if isEn>
Hello!
  <#else>
Привет!
  </#if>
</#if>

<#if isEn>
*Server ${data.serverId}<#if data.serverName?has_content> (${data.serverName})</#if>*
<#if data.exchange?has_content>
Permanent exchanger: *${data.exchange}*
<#else>
Exchange is not set yet.
</#if>
Reach out in game or via your usual channel to arrange trades. Use the button below to return to the list.
<#else>
*Сервер ${data.serverId}<#if data.serverName?has_content> (${data.serverName})</#if>*
<#if data.exchange?has_content>
Постоянный обменник: *${data.exchange}*
<#else>
Обменник пока не указан.
</#if>
Свяжись с ним в игре или привычным способом, чтобы договориться об обмене. Используй кнопку ниже, чтобы вернуться к списку.
</#if>
