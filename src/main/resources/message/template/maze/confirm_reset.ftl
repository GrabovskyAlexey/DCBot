<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "confirm_reset_en.ftl">
#else>
    <#include "confirm_reset_ru.ftl">
</#if>
