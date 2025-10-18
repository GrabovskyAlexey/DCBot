<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Specify the server you need the exchange for.
<#else>
Укажи, сервер на который нужен обмен.
</#if>
