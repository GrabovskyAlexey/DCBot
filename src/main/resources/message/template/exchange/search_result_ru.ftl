<#if data.active>
<#assign targetId = data.request.targetServerId!"любой сервер">
<#assign sourceId = data.request.sourceServerId!"любой сервер">
*Запрос:* <#if data.request.type == "EXCHANGE_MAP">Обменять 🪆 *${sourceId}* ➡️ *${targetId}*
<#elseif data.request.type == "EXCHANGE_VOID">
Обменять 🟣 *${sourceId}* ➡️ *${targetId}*
<#elseif data.request.type == "SELL_MAP">
Продать 🪆 *${data.request.sourcePrice}:${data.request.targetPrice} 🟣*
<#elseif data.request.type == "BUY_MAP">
Купить 🪆 *${data.request.targetPrice}:${data.request.sourcePrice} 🟣*
</#if>
Пользователь: *${data.firstName}*
https://t.me/${data.username}
<#else>
Пользователь уже отменил заявку.
</#if>
