<#assign lang = (locale?string?lower_case)!"">
<#assign isEn = lang?starts_with("en")>

<#if data.active>
<#assign targetId = data.request.targetServerId!isEn?string("any server","любой сервер")>
<#assign sourceId = data.request.sourceServerId!isEn?string("any server","любой сервер")>
<#if isEn>
*Request*: <#if data.request.type == "EXCHANGE_MAP">Exchange 🪆 *${sourceId}* ➡️ *${targetId}*
<#elseif data.request.type == "EXCHANGE_VOID">
Exchange 🟣 *${sourceId}* ➡️ *${targetId}*
<#elseif data.request.type == "SELL_MAP">
Sell 🪆 *${data.request.sourcePrice}:${data.request.targetPrice} 🟣*
<#elseif data.request.type == "BUY_MAP">
Buy 🪆 *${data.request.sourcePrice}:${data.request.targetPrice} 🟣*
</#if>
User: *${data.firstName}*
<#else>
*Запрос: *<#if data.request.type == "EXCHANGE_MAP">Обменять 🪆 *${sourceId}* ➡️ *${targetId}*
<#elseif data.request.type == "EXCHANGE_VOID">
Обменять 🟣 *${sourceId}* ➡️ *${targetId}*
<#elseif data.request.type == "SELL_MAP">
Продать 🪆 *${data.request.sourcePrice}:${data.request.targetPrice} 🟣*
<#elseif data.request.type == "BUY_MAP">
Купить 🪆 *${data.request.targetPrice}:${data.request.sourcePrice} 🟣*
</#if>
Пользователь: *${data.firstName}*
</#if>
https://t.me/${data.username}
<#else>
<#if isEn>
User cancel exchange request
<#else>
Пользователь уже отменил заявку
</#if>
</#if>