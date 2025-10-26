${data.text}
<#if data.notes?has_content>

<#list data.notes as note>
${note}
</#list>
</#if>
