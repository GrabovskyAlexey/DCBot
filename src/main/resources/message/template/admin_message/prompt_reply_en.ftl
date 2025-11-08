<#assign prompt=data>
Write a reply for *${prompt.firstName}*<#if (prompt.userName)?has_content> (@${prompt.userName})</#if> (id: *${prompt.userId?c}*).
<#if prompt.invalid?? && prompt.invalid>

The reply cannot be empty.
</#if>
