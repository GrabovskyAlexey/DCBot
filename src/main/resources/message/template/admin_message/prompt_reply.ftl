<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
    <#include "prompt_reply_en.ftl">
<#else>
    <#include "prompt_reply_ru.ftl">
</#if>
