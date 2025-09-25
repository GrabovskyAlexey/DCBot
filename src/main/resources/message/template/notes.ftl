<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if (data.notes)??>
<#if isEn>
*Notes*
<#else>
*Заметки*
</#if>
<#list data.notes as item>
*${item?counter}.* ${item}
<#else>
<#if isEn>
You don't have any notes
<#else>
У вас нет ни одной заметки
</#if>
</#list>
</#if>
