<#if data.active>
<#assign targetId = data.request.targetServerId!"any server">
<#assign sourceId = data.request.sourceServerId!"any server">
*Request*: <#if data.request.type == "EXCHANGE_MAP">Exchange 🪆 *${sourceId}* ➡️ *${targetId}*
<#elseif data.request.type == "EXCHANGE_VOID">
Exchange 🟣 *${sourceId}* ➡️ *${targetId}*
<#elseif data.request.type == "SELL_MAP">
Sell 🪆 *${data.request.sourcePrice}:${data.request.targetPrice} 🟣*
<#elseif data.request.type == "BUY_MAP">
Buy 🪆 *${data.request.targetPrice}:${data.request.sourcePrice} 🟣*
</#if>
User: *${data.firstName}*
https://t.me/${data.username}
<#else>
User cancelled the exchange request.
</#if>
