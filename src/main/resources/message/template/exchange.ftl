<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#assign name = (data?if_exists.username)!''>
<#if isEn>
Hello<#if name?has_content>, *${name}*</#if>!
Exchange search will appear here soon.
<#else>
Привет<#if name?has_content>, *${name}*</#if>!
Здесь скоро появится поиск обменников.
</#if>
