<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Your message was successfully sent to the developers
<#else>
Ваше сообщение успешно отправлено разработчикам
</#if>
