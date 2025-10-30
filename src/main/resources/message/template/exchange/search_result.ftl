<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "search_result_en.ftl">
<#else>
    <#include "search_result_ru.ftl">
</#if>
