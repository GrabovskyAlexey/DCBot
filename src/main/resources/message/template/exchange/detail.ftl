<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "detail_en.ftl">
<#else>
    <#include "detail_ru.ftl">
</#if>
