<#assign lang = (locale.language!"")?lower_case>
<#if lang?starts_with("en")>
<#include "mine_en.ftl">
<#else>
<#include "mine_ru.ftl">
</#if>
