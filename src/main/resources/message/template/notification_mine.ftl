<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
*Time to capture the mine*
<#else>
*Пора захватывать КШ*
</#if>