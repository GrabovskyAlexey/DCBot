<#assign notes = data.notes![]>
<#if notes?has_content>
*Заметки*
<#list notes as note>
*${note.index}.* ${note.text}
</#list>
<#else>
У вас пока нет заметок.
</#if>
