<#if (data.notes)??>
*Заметки*
<#list data.notes as item>
*${item?counter}.* ${item}
<#else>
У вас нет ни одной заметки
</#list>
</#if>