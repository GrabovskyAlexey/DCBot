<#assign servers = data.servers![]> 
<#if servers?size == 0>
You're not subscribed to any server yet.
Tap a server number below to subscribe.
<#elseif servers?size == 1>
<#assign serverId = servers[0]>
You're subscribed to notifications on server .
Tap the server number again to unsubscribe or choose another server to subscribe.
<#else>
<#assign list = servers?join(", ")>
You're subscribed to notifications on servers .
Tap a subscribed server to unsubscribe or pick another to subscribe.
</#if>
