Здесь можно включить или выключить уведомления
<#if (data)??>
*Текущие настройки:*
*Осада:* <#if data.siegeEnabled>за 5 минут<#else>в момент осады</#if>
*КШ:* <#if data.mineEnabled>✅<#else>❌</#if>
*КБ:* <#if data.cbEnabled>✅<#else>❌</#if>
</#if>