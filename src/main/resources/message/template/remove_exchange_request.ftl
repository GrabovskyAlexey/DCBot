<#assign lang = (locale?string?lower_case)!"">
<#assign isEn = lang?starts_with("en")>
<#include 'exchange_detail.ftl'>
*
<#if isEn>
Select the request number to delete
<#else>
Выберете номер заявки для удаления
</#if>
*