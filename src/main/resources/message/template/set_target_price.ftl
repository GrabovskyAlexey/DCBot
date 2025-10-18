<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Specify how many ${data.resource} you want to receive
<#else>
Укажи, сколько ${data.resource} хочешь получить.
</#if>
