<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if (data.history)??>
<#if isEn>
*Last 20 operations*
<#else>
*Последние 20 операций*
</#if>
<#list data.history as item>
*${item?counter}.* ${item}
<#else>
<#if isEn>
No operation data
<#else>
Нет данных об операциях
</#if>
</#list>
</#if>
