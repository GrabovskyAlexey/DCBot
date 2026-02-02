*Результаты глобального поиска*
<#if data.hasMatches>
Найдено совпадений: *${data.totalMatchesCount}*
<#assign globalIndex = 1>
<#list data.matchGroups as group>
<#assign userReq = group.userRequest>
<#assign targetId = userReq.targetServerId!"любой">
<#assign sourceId = userReq.sourceServerId>

*Твоя заявка:*
<#if userReq.type == "EXCHANGE_MAP">
🪆 Обмен карт: сервер ${sourceId} ➡️ ${targetId}
<#elseif userReq.type == "EXCHANGE_VOID">
🟣 Обмен пустот: сервер ${sourceId} ➡️ ${targetId}
<#elseif userReq.type == "SELL_MAP">
Продать 🪆 *${userReq.sourcePrice}:${userReq.targetPrice} 🟣* на сервере ${sourceId}
<#elseif userReq.type == "BUY_MAP">
Купить 🪆 *${userReq.targetPrice}:${userReq.sourcePrice} 🟣* на сервере ${sourceId}
</#if>
*Найдено совпадений: ${group.matches?size}*
<#list group.matches as match>
<#assign matchReq = match.request>
<#assign matchTargetId = matchReq.targetServerId!"любой">
<#assign matchSourceId = matchReq.sourceServerId>
<#if matchReq.type == "EXCHANGE_MAP">
*${globalIndex}.* 🪆 ${matchSourceId} ➡️ ${matchTargetId} (${match.ownerFirstName})
<#elseif matchReq.type == "EXCHANGE_VOID">
*${globalIndex}.* 🟣 ${matchSourceId} ➡️ ${matchTargetId} (${match.ownerFirstName})
<#elseif matchReq.type == "SELL_MAP">
*${globalIndex}.* Продать 🪆 *${matchReq.sourcePrice}:${matchReq.targetPrice} 🟣* (${match.ownerFirstName})
<#elseif matchReq.type == "BUY_MAP">
*${globalIndex}.* Купить 🪆 *${matchReq.targetPrice}:${matchReq.sourcePrice} 🟣* (${match.ownerFirstName})
</#if>
<#assign globalIndex = globalIndex + 1>
</#list>
</#list>

Нажми на номер заявки, чтобы получить контакт.
<#else>
Совпадений не найдено.
Попробуй создать новые заявки или изменить условия обмена.
</#if>
