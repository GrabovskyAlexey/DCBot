<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "server_en.ftl">
<#else>
    <#include "server_ru.ftl">
</#if>
