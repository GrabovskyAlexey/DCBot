*Global search results*
<#if data.hasMatches>
Matches found: *${data.totalMatchesCount}*
<#assign globalIndex = 1>
<#list data.matchGroups as group>
<#assign userReq = group.userRequest>
<#assign targetId = userReq.targetServerId!"any">
<#assign sourceId = userReq.sourceServerId>

*Your request:*
<#if userReq.type == "EXCHANGE_MAP">
🪆 Exchange maps: server ${sourceId} ➡️ ${targetId}
<#elseif userReq.type == "EXCHANGE_VOID">
🟣 Exchange voids: server ${sourceId} ➡️ ${targetId}
<#elseif userReq.type == "SELL_MAP">
Sell 🪆 *${userReq.sourcePrice}:${userReq.targetPrice} 🟣* on server ${sourceId}
<#elseif userReq.type == "BUY_MAP">
Buy 🪆 *${userReq.targetPrice}:${userReq.sourcePrice} 🟣* on server ${sourceId}
</#if>
*Matches found: ${group.matches?size}*
<#list group.matches as match>
<#assign matchReq = match.request>
<#assign matchTargetId = matchReq.targetServerId!"any">
<#assign matchSourceId = matchReq.sourceServerId>
<#if matchReq.type == "EXCHANGE_MAP">
*${matchReq.pos}.* 🪆 ${matchSourceId} ➡️ ${matchTargetId} (${match.ownerFirstName})
<#elseif matchReq.type == "EXCHANGE_VOID">
*${matchReq.pos}.* 🟣 ${matchSourceId} ➡️ ${matchTargetId} (${match.ownerFirstName})
<#elseif matchReq.type == "SELL_MAP">
*${matchReq.pos}.* Sell 🪆 *${matchReq.sourcePrice}:${matchReq.targetPrice} 🟣* (${match.ownerFirstName})
<#elseif matchReq.type == "BUY_MAP">
*${matchReq.pos}.* Buy 🪆 *${matchReq.targetPrice}:${matchReq.sourcePrice} 🟣* (${match.ownerFirstName})
</#if>
</#list>
</#list>

Click on the request number to get contact info.
<#else>
No matches found.
Try creating new requests or changing exchange conditions.
</#if>
