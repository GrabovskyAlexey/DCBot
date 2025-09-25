<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Specify the number of steps
<#else>
Укажите количество шагов
</#if>