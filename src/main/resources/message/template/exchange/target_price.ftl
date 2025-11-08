<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "target_price_en.ftl">
<#else>
    <#include "target_price_ru.ftl">
</#if>
