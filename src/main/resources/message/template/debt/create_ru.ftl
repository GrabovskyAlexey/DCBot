<#assign c = data>
*Новый долг: ${c.direction!"не выбрано"}*
*Сервер:* ${c.server!"не выбран"}
*Ресурс:* ${c.resource!"не выбран"}
*Количество:* <#if c.amount??>${c.amount}<#else>не указано</#if>
*Контрагент:* ${c.counterparty!"не указан"}

<#switch c.phase?string>
    <#case "SERVER">
Выберите сервер, к которому относится долг.
        <#break>
    <#case "RESOURCE">
Выберите ресурс, по которому заводим долг.
        <#break>
    <#case "AMOUNT">
Отправьте количество выбранного ресурса сообщением ниже.
        <#break>
    <#case "NAME">
Введите имя должника/кредитора.
        <#break>
</#switch>
