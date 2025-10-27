<#assign location = data.location?if_exists>
<#if location??>
    <#if data.isComplete()>
*Поздравляем, вы прошли игру*
    <#else>
Текущее местонахождение:
Этаж: ${location.level}
Направление: <#if location.direction == "LEFT">L${location.offset}<#elseif location.direction == "RIGHT">R${location.offset}<#else>0</#if>
        <#if data.showHistory>
            <#if data.steps?has_content>
*Последние 20 шагов*
                <#list data.steps as step>
*${step?counter}.* ${step}
                </#list>
            <#else>
Нет данных о шагах
            </#if>
        </#if>
    </#if>
<#else>
Нет данных по прохождению лабиринта
</#if>
