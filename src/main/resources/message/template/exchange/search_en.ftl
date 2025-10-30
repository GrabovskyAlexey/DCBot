*Server ${data.serverId}*

<#if data.requests?has_content>
*Found active requests:*
<#list data.requests as request>
<#assign targetId = request.targetServerId!"any server">
<#assign sourceId = request.sourceServerId!"any server">
<#if request.type == "EXCHANGE_MAP">
*${request.pos}.* Exchange 🪆 *${sourceId}* ➡️ *${targetId}*
<#elseif request.type == "EXCHANGE_VOID">
*${request.pos}.* Exchange 🟣 *${sourceId}* ➡️ *${targetId}*
<#elseif request.type == "SELL_MAP">
*${request.pos}.* Sell 🪆 *${request.sourcePrice}:${request.targetPrice} 🟣*
<#elseif request.type == "BUY_MAP">
*${request.pos}.* Buy 🪆 *${request.targetPrice}:${request.sourcePrice} 🟣*
</#if>
</#list>
<#else>
No active requests found.
</#if>
