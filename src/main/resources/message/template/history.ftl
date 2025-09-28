<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if (data.steps)??>
<#if isEn>
*Last 20 steps*
<#else>
*Последние 20 шагов*
</#if>
<#list data.steps as step>
*${step?counter}.* ${step}
<#else>
<#if isEn>
No step data
<#else>
Нет данных о шагах
</#if>
</#list>
</#if>