<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "prompt_amount_en.ftl">
<#else>
    <#include "prompt_amount_ru.ftl">
</#if>
