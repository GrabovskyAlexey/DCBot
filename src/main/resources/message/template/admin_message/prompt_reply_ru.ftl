<#assign prompt=data>
Ответь пользователю *${prompt.firstName}*<#if (prompt.userName)?has_content> (@${prompt.userName})</#if> (id: *${prompt.userId?c}*).
<#if prompt.invalid?? && prompt.invalid>

Ответ не может быть пустым.
</#if>
