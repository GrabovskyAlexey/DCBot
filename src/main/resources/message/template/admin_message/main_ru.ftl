<#if (data)??>
Получено сообщение от *${data.firstName}*, <#if (data.userName)??> username: *${data.userName}*,</#if> id: *${data.userId?c}*
${data.text}
<#else>
Неопознанная ошибка
</#if>