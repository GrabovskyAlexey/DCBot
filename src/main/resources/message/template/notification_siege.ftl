<#if (data.servers)??>
    <#if data.servers?size == 1>
        Началась осада на *${data.servers[0]} сервере*
    <#elseif data.servers?size gt 1>
        Началась осада на следующих серверах:
        <#list data.servers as server>
*${server} сервер*
        </#list>
    </#if>
</#if>
