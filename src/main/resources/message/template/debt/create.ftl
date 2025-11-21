<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "create_en.ftl">
<#else>
    <#include "create_ru.ftl">
</#if>
