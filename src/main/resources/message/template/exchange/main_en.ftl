<#assign hasUsername = data.username?? && data.username?has_content>
<#if !hasUsername>
*Important: Your Telegram account doesn't have a username set. You can create and search for exchanges, but other players won't see your offers until you set a @username in your Telegram settings.*

</#if>
<#if data.hasServers>
Tap a server to view exchange request details.
Servers marked with a ✅ on the buttons are those where you have active requests.
<#else>
No exchange requests saved yet.
</#if>
