<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "prompt_name_en.ftl">
<#else>
    <#include "prompt_name_ru.ftl">
</#if>
