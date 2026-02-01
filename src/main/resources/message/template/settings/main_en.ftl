Here you can edit bot settings
<#if (data)??>
*Current settings:*
*Siege:* <#if data.siegeEnabled>5 minutes before<#else>at siege start</#if>
*Mine:* <#if data.mineEnabled>✅<#else>❌</#if>
*DH:* <#if data.cbEnabled>✅<#else>❌</#if>
*Quick tracking:* <#if data.quickResourceEnabled>✅<#else>❌</#if>
*Send resources to main server:* <#if data.enableMainSend>✅<#else>❌</#if>
*Display in summary:* <#if data.showExchangeUsername>@username<#else>Exchange name</#if>
*Language:* 🇺🇸
</#if>