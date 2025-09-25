<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if data??>
<#if isEn>
*Resources on ${data.id} server*
<#else>
*Ресурсы на ${data.id} сервере*
</#if>
<#if data.main>
<#if isEn>*👑Main*<#else>*👑Основной*</#if>
<#else>
<#if isEn>*Exchange:* <#if data.exchange?has_content>${data.exchange}<#else>Not set</#if><#else>*Обменник:* <#if data.exchange?has_content>${data.exchange}<#else>Отсутствует</#if></#if>
</#if>
<#if isEn>*On hand:* ${data.draadorCount}🪆<#else>*На руках:* ${data.draadorCount}🪆</#if>
<#if data.balance gt 0>
<#if isEn>*They owe me:* ${data.balance}🪆<#else>*Мне должны:* ${data.balance}🪆</#if>
<#elseif data.balance lt 0>
<#if isEn>*I owe:* ${data.balance * -1}🪆<#else>*Я должен:* ${data.balance * -1}🪆</#if>
</#if>
<#if isEn>*Voids:* ${data.voidCount} 🟣<#else>*Пустот:* ${data.voidCount} 🟣</#if>
<#if data.cbEnabled>
<#if isEn>*CB:* ${data.cbCount}😈<#else>*КБ:* ${data.cbCount}😈</#if>
</#if>
<#if data.main><#include 'notes.ftl'></#if>
<#include 'server_resource_history.ftl'>
<#else>
<#if isEn>
No resource data for this server
<#else>
Отсутствую данные о ресурсах на сервере
</#if>
</#if>
