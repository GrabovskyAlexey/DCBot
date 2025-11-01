<#if (data.servers)??>
<#if data.before>
<#assign singleLine = "A siege will start in 5 minutes on *${data.servers[0]} server*">
<#assign multiHeader = "Sieges will start in 5 minutes on the following servers:">
<#else>
<#assign singleLine = "A siege has started on *${data.servers[0]} server*">
<#assign multiHeader = "Sieges have started on the following servers:">
</#if>
<#if data.servers?size == 1>
${singleLine}
<#elseif data.servers?size gt 1>
${multiHeader}
<#list data.servers as server>
*${server} server*
</#list>
</#if>
</#if>
