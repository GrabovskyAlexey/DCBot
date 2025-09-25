<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#include 'step_count.ftl'> <#if isEn>right<#else>вправо</#if>
