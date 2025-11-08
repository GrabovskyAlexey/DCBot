<#assign summaries = data.summaries![]>
<#if summaries?has_content>
*Сводка ресурсов по серверам*
<#list summaries as summary>
${summary.statusIcon} *${summary.id}s:* <#if summary.main>*👑 Основной*<#else>Обменник:* <#if summary.exchange?has_content>${summary.exchange}*<#else>Отсутствует*</#if></#if>, *${summary.draadorCount}${summary.balanceLabel} 🪆, ${summary.voidCount} 🟣*<#if summary.cbEnabled>, *${summary.cbCount} 😈*</#if>
</#list>
<#else>
Данные о ресурсах ещё не собирались. Выбери сервер ниже, чтобы начать учёт.
</#if>
