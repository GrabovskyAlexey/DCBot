<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "source_price_en.ftl">
<#else>
    <#include "source_price_ru.ftl">
</#if>
