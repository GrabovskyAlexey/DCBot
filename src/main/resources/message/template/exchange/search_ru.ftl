*Сервер ${data.serverId}*

<#if data.requests?has_content>
*Найденные активные заявки:*
<#list data.requests as request>
<#assign targetId = request.targetServerId!"любой сервер">
<#assign sourceId = request.sourceServerId!"любой сервер">
<#if request.type == "EXCHANGE_MAP">
*${request.pos}.* Обменять 🪆 *${sourceId}* ➡️ *${targetId}*
<#elseif request.type == "EXCHANGE_VOID">
*${request.pos}.* Обменять 🟣 *${sourceId}* ➡️ *${targetId}*
<#elseif request.type == "SELL_MAP">
*${request.pos}.* Продать 🪆 *${request.sourcePrice}:${request.targetPrice} 🟣*
<#elseif request.type == "BUY_MAP">
*${request.pos}.* Купить 🪆 *${request.targetPrice}:${request.sourcePrice} 🟣*
</#if>
</#list>
<#else>
Активных заявок не найдено.
</#if>
