*Легенда*: Пустота(🟣), Карта(🪆), КБ(😈), Ящик банок(🫙), Пушка(🔫)
<#assign oweMe = data.oweMe![]>
<#assign iOwe = data.IOwe![]>
*Вам должны*
<#if oweMe?has_content>
<#list oweMe as debt>
*${debt.displayNumber}. ${debt.amount}* ${debt.resourceLabel} на ${debt.server} (*${debt.counterparty}*)
</#list>
<#else>
Долгов в вашу пользу нет.
</#if>

*Вы должны*
<#if iOwe?has_content>
<#list iOwe as debt>
*${debt.displayNumber}. ${debt.amount}* ${debt.resourceLabel} на ${debt.server} (*${debt.counterparty}*)
</#list>
<#else>
Записей о долгах нет.
</#if>

_Создайте или удалите долг кнопками ниже._