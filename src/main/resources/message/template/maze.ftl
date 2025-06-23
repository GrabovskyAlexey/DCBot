<#if (data.location)??>
<#if data.isComplete()>
*Поздравляем, Вы прошли игру*
<#else>
Текущее местонахождение:
Этаж: ${data.location.level}
Направление: <#if data.location.direction == "LEFT">L${data.location.offset}<#elseif data.location.direction == "RIGHT">R${data.location.offset}<#else>0</#if>
<#include 'history.ftl'>
</#if>
<#else>
Нет данных по прохождению лабиринта
</#if>