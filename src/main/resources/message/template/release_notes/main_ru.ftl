<#assign hasVersion = (data.version)?? && data.version?has_content>
📢 ОБНОВЛЕНИЕ
<#if hasVersion>
🆕 Новое обновление (${data.version})
</#if>
*Что изменилось*
${data.text}
