<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
<#include "siege_en.ftl">
<#else>
<#include "siege_ru.ftl">
</#if>
