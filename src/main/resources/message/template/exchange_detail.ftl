<#assign lang = (locale?string?lower_case)!"">
<#assign isEn = lang?starts_with("en")>

<#if isEn>
*Server ${data.serverId}*
<#else>
*Сервер ${data.serverId}*
</#if>
<#if data.requests?has_content>
  <#if isEn>
*Your active requests:*
  <#else>
*Твои активные заявки:*
  </#if>
  <#list data.requests as request>
    <#assign targetId = request.targetServerId!isEn?string("any server","любой сервер")>
    <#if isEn>
    <#if request.type == "EXCHANGE_MAP">
*${request.pos}.* Exchange 🪆 ➡️ *${targetId}*
    <#elseif request.type == "EXCHANGE_VOID">
*${request.pos}.* Exchange 🟣 ➡️ *${targetId}*
    <#elseif request.type == "SELL_MAP">
*${request.pos}.* Sell 🪆 *${request.sourcePrice}:${request.targetPrice} 🟣*
    <#elseif request.type == "BUY_MAP">
*${request.pos}.* Buy 🪆 *${request.sourcePrice}:${request.targetPrice} 🟣*
</#if>
    <#else>
        <#if request.type == "EXCHANGE_MAP">
*${request.pos}.* Обменять 🪆 ➡️ *${targetId}*
        <#elseif request.type == "EXCHANGE_VOID">
*${request.pos}.* Обменять 🟣 ➡️ *${targetId}*
        <#elseif request.type == "SELL_MAP">
*${request.pos}.* Продать 🪆 *${request.sourcePrice}:${request.targetPrice} 🟣*
        <#elseif request.type == "BUY_MAP">
*${request.pos}.* Купить 🪆 *${request.targetPrice}:${request.sourcePrice} 🟣*
        </#if>
    </#if>
  </#list>
<#else>
  <#if isEn>
You have no active requests yet.
  <#else>
У тебя пока нет активных заявок.
  </#if>
</#if>
