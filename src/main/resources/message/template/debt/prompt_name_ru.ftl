<#assign creation = data.creation>
${data.title}
<#if data.invalid?? && data.invalid>
Имя не может быть пустым.
</#if>

*Выбрано:* ${creation.direction!"?"}, ${creation.server!"?"}, ${creation.resource!"?"}
Количество: <#if creation.amount??>${creation.amount}<#else>?</#if>
