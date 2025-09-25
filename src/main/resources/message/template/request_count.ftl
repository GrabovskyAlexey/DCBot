<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Specify the quantity
<#else>
Укажите количество
</#if>
