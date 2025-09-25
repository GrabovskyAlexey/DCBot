<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Enter the exchanger nickname
<#else>
Укажите ник обменника
</#if>
