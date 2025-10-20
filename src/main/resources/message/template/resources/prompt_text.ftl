${data.text}
<#if data.notes?size gt 0>

<#list data.notes as note>
${note}
</#list>
</#if>
