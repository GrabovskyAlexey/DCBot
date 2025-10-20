<#assign detail = data.detail>
<#assign server = detail.dto>
*Ресурсы на сервере ${server.id}*
<#if server.main>
*👑 Основной сервер*
<#else>
*Обменник:* <#if server.exchange?has_content>${server.exchange}<#else>Отсутствует</#if>
</#if>
*На руках:* ${server.draadorCount} 🪆
<#if server.balance gt 0>
*Мне должны:* +${server.balance} 🪆
<#elseif server.balance lt 0>
*Я должен:* ${server.balance} 🪆
</#if>
*Пустоты:* ${server.voidCount} 🟣
<#if server.cbEnabled>
*КБ:* ${server.cbCount} 😈
</#if>
<#if server.main && server.notes?size gt 0>
*Заметки:*
<#list server.notes as note>
*${note_index + 1}.* ${note}
</#list>
</#if>
<#if detail.history?size gt 0>
*История:*
<#list detail.history as record>
${record}
</#list>
</#if>