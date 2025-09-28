<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Here you can enable or disable notifications
<#else>
Здесь можно включить или выключить уведомления
</#if>
<#if (data)??>
<#if isEn>
*Current settings:*
<#else>
*Текущие настройки:*
</#if>
<#if isEn>
*Siege:* <#if data.siegeEnabled>5 minutes before<#else>at siege start</#if>
*Mine:* <#if data.mineEnabled>✅<#else>❌</#if>
*CB:* <#if data.cbEnabled>✅<#else>❌</#if>
*Quick tracking:* <#if data.quickResourceEnabled>✅<#else>❌</#if>
<#else>
*Осада:* <#if data.siegeEnabled>за 5 минут<#else>в момент осады</#if>
*КШ:* <#if data.mineEnabled>✅<#else>❌</#if>
*КБ:* <#if data.cbEnabled>✅<#else>❌</#if>
*Быстрый учет:* <#if data.quickResourceEnabled>✅<#else>❌</#if>
</#if>
</#if>