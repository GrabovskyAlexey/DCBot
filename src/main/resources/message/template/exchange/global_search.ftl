<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "global_search_en.ftl">
<#else>
    <#include "global_search_ru.ftl">
</#if>
