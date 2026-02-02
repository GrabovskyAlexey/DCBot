<#assign hasUsername = data.username?? && data.username?has_content>
<#if !hasUsername>
*Important: Your Telegram account doesn't have a username set. You can create and search for exchanges, but other players won't see your offers until you set a @username in your Telegram settings.*

</#if>
<#if data.allActiveRequests?has_content>
*Your active requests (${data.allActiveRequests?size}):*
<#list data.allActiveRequests as request>
<#assign targetId = request.targetServerId!"any">
<#assign sourceId = request.sourceServerId>
<#if request.type == "EXCHANGE_MAP">
*${request.pos}.* 🪆 Server ${sourceId} ➡️ ${targetId}
<#elseif request.type == "EXCHANGE_VOID">
*${request.pos}.* 🟣 Server ${sourceId} ➡️ ${targetId}
<#elseif request.type == "SELL_MAP">
*${request.pos}.* Sell 🪆 *${request.sourcePrice}:${request.targetPrice} 🟣* (server ${sourceId})
<#elseif request.type == "BUY_MAP">
*${request.pos}.* Buy 🪆 *${request.targetPrice}:${request.sourcePrice} 🟣* (server ${sourceId})
</#if>
</#list>

</#if>
<#if data.hasServers>
Select a server to manage requests or use global search.
Servers with active requests are marked with ✅.
<#else>
No exchange requests saved yet.
</#if>
