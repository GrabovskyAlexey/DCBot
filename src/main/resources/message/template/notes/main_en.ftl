<#assign notes = data.notes![]>
<#if notes?has_content>
*Notes*
<#list notes as note>
*${note.index}.* ${note.text}
</#list>
<#else>
You don't have any notes yet.
</#if>
