<#assign hasVersion = (data.version)?? && data.version?has_content>
<#assign messageText = data.textEn?has_content?then(data.textEn, data.text)>
📢 UPDATE
<#if hasVersion>
🆕 New release (${data.version})
</#if>
*What's new*
${messageText}
