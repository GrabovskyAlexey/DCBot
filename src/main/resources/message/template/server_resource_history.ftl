<#if (data.history)??>
*Последние 20 операций*
<#list data.history as item>
*${item?counter}.* ${item}
<#else>
Нет данных об операциях
</#list>
</#if>