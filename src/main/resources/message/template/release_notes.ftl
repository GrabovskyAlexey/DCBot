<#assign lang = locale?string?lower_case>
<#assign hasVersion = (data.version)?? && data.version?has_content>
<#if lang?starts_with("en")>
  <#assign updateTitle = "📢 UPDATE">
  <#assign newUpdateLine = hasVersion?then("🆕 New release (" + data.version + ")", "")>
  <#assign changesTitle = "*What's new*">
<#else>
  <#assign updateTitle = "📢 ОБНОВЛЕНИЕ">
  <#assign newUpdateLine = hasVersion?then("🆕 Новое обновление (" + data.version + ")", "")>
  <#assign changesTitle = "*Что изменилось*">
</#if>
<#assign messageText = data.text>
<#if lang?starts_with("en") && (data.textEn)?? && data.textEn?has_content>
  <#assign messageText = data.textEn>
</#if>
${updateTitle}
<#if newUpdateLine?has_content>
${newUpdateLine}
</#if>
${changesTitle}
${messageText}