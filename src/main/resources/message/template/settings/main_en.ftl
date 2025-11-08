Here you can enable or disable notifications
<#if (data)??>
*Current settings:*
*Siege:* <#if data.siegeEnabled>5 minutes before<#else>at siege start</#if>
*Mine:* <#if data.mineEnabled>✅<#else>❌</#if>
*DH:* <#if data.cbEnabled>✅<#else>❌</#if>
*Quick tracking:* <#if data.quickResourceEnabled>✅<#else>❌</#if>
</#if>