*Legends*: Voids(🟣), Maps/Draador(🪆), DH(😈), Crate of Bottles(🫙), Cannon(🔫)
<#assign oweMe = data.oweMe![]>
<#assign iOwe = data.IOwe![]>
*They owe you*
<#if oweMe?has_content>
<#list oweMe as debt>
*${debt.displayNumber}. ${debt.amount}* ${debt.resourceLabel} on ${debt.server} (*${debt.counterparty}*)
</#list>
<#else>
No debts recorded.
</#if>

*You owe*
<#if iOwe?has_content>
<#list iOwe as debt>
*${debt.displayNumber}. ${debt.amount}* ${debt.resourceLabel} on ${debt.server} (*${debt.counterparty}*)
</#list>
<#else>
No debts recorded.
</#if>

_Create or remove debt using the buttons below._