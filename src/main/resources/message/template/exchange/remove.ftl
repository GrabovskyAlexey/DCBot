<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "remove_en.ftl">
<#else>
    <#include "remove_ru.ftl">
</#if>
