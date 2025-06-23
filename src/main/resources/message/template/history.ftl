<#if (data.steps)??>
*Последние 20 шагов*
<#list data.steps as step>
*${step?counter}.* ${step}
<#else>
Нет данных о шагах
</#list>
</#if>