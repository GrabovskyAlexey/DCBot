<#assign c = data>
*New debt: ${c.direction!"not set"}*
*Server:* ${c.server!"not selected"}
*Resource:* ${c.resource!"not selected"}
*Amount:* <#if c.amount??>${c.amount}<#else>not set</#if>
*Counterparty:* ${c.counterparty!"not set"}

<#switch c.phase?string>
    <#case "SERVER">
Pick the server for this debt.
        <#break>
    <#case "RESOURCE">
Pick the resource for this debt.
        <#break>
    <#case "AMOUNT">
Send the resource amount as a message below.
        <#break>
    <#case "NAME">
Enter the debtor/creditor name.
        <#break>
</#switch>
