*Server ${data.serverId}*

<#if data.requests?has_content>
*Your active requests:*
<#list data.requests as request>
<#assign targetId = request.targetServerId!"any server">
<#if request.type == "EXCHANGE_MAP">
*${request.pos}.* Exchange 🪆 ➡️ *${targetId}*
<#elseif request.type == "EXCHANGE_VOID">
*${request.pos}.* Exchange 🟣 ➡️ *${targetId}*
<#elseif request.type == "SELL_MAP">
*${request.pos}.* Sell 🪆 *${request.sourcePrice}:${request.targetPrice} 🟣*
<#elseif request.type == "BUY_MAP">
*${request.pos}.* Buy 🪆 *${request.targetPrice}:${request.sourcePrice} 🟣*
</#if>
</#list>
<#else>
You have no active requests yet.
</#if>
