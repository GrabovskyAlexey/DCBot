<#if data??>
*Ресурсы на ${data.id} сервере*
<#if data.main>*👑Основной*<#else>*Обменник:* <#if data.exchange?has_content>${data.exchange}<#else>Отсутствует</#if></#if>
*На руках:* ${data.draadorCount}🪆
<#if data.balance gt 0>*Мне должны:* ${data.balance}🪆
<#elseif data.balance lt 0>*Я должен:* ${data.balance * -1}🪆
</#if>*Пустот:* ${data.voidCount} 🟣
<#if data.cbEnabled>*КБ:* ${data.cbCount}😈
</#if><#if data.main><#include 'notes.ftl'></#if>
<#include 'server_resource_history.ftl'>
<#else>
Отсутствую данные о ресурсах на сервере
</#if>
