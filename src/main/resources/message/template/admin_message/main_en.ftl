<#if (data)??>
Received a message from *${data.firstName}*, <#if (data.userName)??> username: *${data.userName}*,</#if> id: *${data.userId?c}*
${data.text}
<#else>
Unknown error
</#if>