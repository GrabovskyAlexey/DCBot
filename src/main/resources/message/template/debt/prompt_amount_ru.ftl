<#assign creation = data.creation>
${data.title}
<#if data.invalid?? && data.invalid>
Введите целое положительное число.
</#if>

*Выбрано:* ${creation.direction!"?"}, ${creation.server!"?"}, ${creation.resource!"?"}
