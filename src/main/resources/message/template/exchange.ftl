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

<#if data.hasServers>
  <#if isEn>
*Saved exchangers by server:*
  <#else>
*Сохранённые обменники по серверам:*
  </#if>
  <#list data.servers as item>
    <#if isEn>
${item.id}<#if item.name?has_content> (${item.name})</#if><#if item.hasExchange>: *${item.exchange}*</#if>
    <#else>
${item.id}<#if item.name?has_content> (${item.name})</#if><#if item.hasExchange>: *${item.exchange}*</#if>
    </#if>
  </#list>
  <#if isEn>
Tap a server to view exchanger details.
  <#else>
Нажми на сервер, чтобы посмотреть данные обменника.
  </#if>
<#else>
  <#if isEn>
No exchangers saved yet.
  <#else>
Сохранённых обменников пока нет.
  </#if>
</#if>
