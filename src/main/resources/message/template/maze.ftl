<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if (data.location)??>
<#if data.isComplete()>
<#if isEn>
*Congratulations, you have completed the maze*
<#else>
*Поздравляем, Вы прошли игру*
</#if>
<#else>
<#if isEn>
Current location:
<#else>
Текущее местонахождение:
</#if>
Этаж: ${data.location.level}
<#if isEn>
Direction: <#if data.location.direction == "LEFT">L${data.location.offset}<#elseif data.location.direction == "RIGHT">R${data.location.offset}<#else>0</#if>
<#else>
Направление: <#if data.location.direction == "LEFT">L${data.location.offset}<#elseif data.location.direction == "RIGHT">R${data.location.offset}<#else>0</#if>
</#if>
<#include 'history.ftl'>
</#if>
<#else>
<#if isEn>
No data about maze progress
<#else>
Нет данных по прохождению лабиринта
</#if>
</#if>
