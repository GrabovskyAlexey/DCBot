<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "flow/subscribe/main_en.ftl">
<#else>
    <#include "flow/subscribe/main_ru.ftl">
</#if>
