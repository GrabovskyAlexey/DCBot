<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Send a suggestion or bug report
<#else>
Отправьте предложение или сообщение об ошибке
</#if>
