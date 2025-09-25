<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if (data)??>
<#if isEn>
Received a message from *${data.firstName}*, <#if (data.userName)??> username: *${data.userName}*,</#if> id: *${data.userId}*
<#else>
Получено сообщение от  *${data.firstName}*, <#if (data.userName)??> username: *${data.userName}*,</#if> id: *${data.userId}*
</#if>
${data.text}
<#else>
<#if isEn>
Unknown error
<#else>
Неопознанная ошибка
</#if>
</#if>
