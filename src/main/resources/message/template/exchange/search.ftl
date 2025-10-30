<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "search_en.ftl">
<#else>
    <#include "search_ru.ftl">
</#if>
