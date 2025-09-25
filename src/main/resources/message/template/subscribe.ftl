<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if (data.servers)??>
<#if data.servers?size == 1>
<#if isEn>
You are subscribed to notifications on server ${data.servers[0]}.
<#else>
Ты подписан на уведомления на ${data.servers[0]} сервере.
</#if>
<#elseif data.servers?size gt 1>
<#if isEn>
You are subscribed to notifications on servers ${data.servers?join(", ")}.
<#else>
Ты подписан на уведомления на ${data.servers?join(", ")} серверах.
</#if>
<#else>
<#if isEn>
You are not subscribed to any server.
<#else>
Ты не подписан ни на один сервер.
</#if>
</#if>
<#else>
<#if isEn>
You are not subscribed to any server.
<#else>
Ты не подписан ни на один сервер.
</#if>
</#if>
<#if isEn>
Press the button with a server number to subscribe or unsubscribe.
<#else>
Нажми на кнопку с номером сервера, чтобы подписаться или отписаться.
</#if>