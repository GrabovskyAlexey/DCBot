<#assign summaries = data.summaries![]>
<#if summaries?has_content>
*Resource overview by server*
<#list summaries as summary>
${summary.statusIcon} *${summary.id}s:* <#if summary.main>*👑 Main*<#else>Exchange:* <#if summary.exchange?has_content>${summary.exchange}*<#else>Not set*</#if></#if>, *${summary.draadorCount}${summary.balanceLabel} 🪆, ${summary.voidCount} 🟣*<#if summary.cbEnabled>, *${summary.cbCount} 😈*</#if>
</#list>
<#else>
You don't have resource data yet. Choose a server below to start tracking.
</#if>
