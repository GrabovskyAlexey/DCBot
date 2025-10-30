<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "target_server_en.ftl">
<#else>
    <#include "target_server_ru.ftl">
</#if>
