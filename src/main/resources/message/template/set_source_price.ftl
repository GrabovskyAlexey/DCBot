<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Specify how many ${data.resource} you want to give.
<#else>
Укажи, сколько ${data.resource} хочешь отдать.
</#if>
