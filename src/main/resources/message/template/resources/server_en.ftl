<#assign detail = data.detail>
<#assign server = detail.dto>
*Resources on server ${server.id}*
<#if server.main>
*👑 Main server*
<#else>
*Exchange:* <#if server.exchange?has_content>${server.exchange}<#else>Not set</#if><#if server.exchangeUsername?has_content> (*@${server.exchangeUsername}*)</#if>
</#if>
*On hand:* ${server.draadorCount} 🪆
<#if server.balance gt 0>
*They owe me:* ${server.balance} 🪆
<#elseif server.balance lt 0>
*I owe:* ${server.balance * -1} 🪆
</#if>
*Voids:* ${server.voidCount} 🟣
<#if server.cbEnabled>
*CB:* ${server.cbCount} 😈
</#if>

<#if server.partners?has_content>
*Exchangers hands:*
<#list server.partners as partner>
<#if partner.username?has_content>*@${partner.username}*<#if partner.mainServerId?? && partner.mainServerId?has_content> *(${partner.mainServerId}s*)</#if>: ${partner.draadorCount} 🪆</#if>
</#list>
</#if>

<#if server.main && server.notes?size gt 0>
*Notes:*
<#list server.notes as note>
*${note_index + 1}.* ${note}
</#list>
</#if>
<#if detail.history?size gt 0>
*History:*
<#list detail.history as record>
${record}
</#list>
</#if>
