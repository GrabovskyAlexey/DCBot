Здесь можно изменить настройки бота
<#if (data)??>
*Текущие настройки:*
*Осада:* <#if data.siegeEnabled>за 5 минут<#else>в момент осады</#if>
*КШ:* <#if data.mineEnabled>✅<#else>❌</#if>
*КБ:* <#if data.cbEnabled>✅<#else>❌</#if>
*Быстрый учет:* <#if data.quickResourceEnabled>✅<#else>❌</#if>
*Отправлять ресурсы на основной сервер:* <#if data.enableMainSend>✅<#else>❌</#if>
*Отображение в сводке:* <#if data.showExchangeUsername>@username<#else>Имя обменника</#if>
*Язык:* 🇷🇺
</#if>