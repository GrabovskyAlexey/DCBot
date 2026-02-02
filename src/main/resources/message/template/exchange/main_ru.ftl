<#assign hasUsername = data.username?? && data.username?has_content>
<#if !hasUsername>
*Важно: У твоего аккаунта Telegram не указан username. Ты можешь создавать и искать обмены, но другие игроки не увидят твои предложения, пока не задашь @username в настройках Telegram.*

</#if>
<#if data.allActiveRequests?has_content>
*Твои активные заявки (${data.allActiveRequests?size}):*
<#list data.allActiveRequests as request>
<#assign targetId = request.targetServerId!"любой">
<#assign sourceId = request.sourceServerId>
<#if request.type == "EXCHANGE_MAP">
*${request.pos}.* 🪆 Сервер ${sourceId} ➡️ ${targetId}
<#elseif request.type == "EXCHANGE_VOID">
*${request.pos}.* 🟣 Сервер ${sourceId} ➡️ ${targetId}
<#elseif request.type == "SELL_MAP">
*${request.pos}.* Продать 🪆 *${request.sourcePrice}:${request.targetPrice} 🟣* (сервер ${sourceId})
<#elseif request.type == "BUY_MAP">
*${request.pos}.* Купить 🪆 *${request.targetPrice}:${request.sourcePrice} 🟣* (сервер ${sourceId})
</#if>
</#list>

</#if>
<#if data.hasServers>
Выбери сервер для управления заявками или используй глобальный поиск.
Символом ✅ отмечены сервера с активными заявками.
<#else>
Сохранённых заявок пока нет.
</#if>
