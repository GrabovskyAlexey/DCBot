<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#assign hasUsername = data.username?? && data.username?has_content>
<#if !hasUsername>
  <#if isEn>
*Important: Your Telegram account doesn’t have a username set. You can create and search for exchanges, but other players won’t see your offers until you set a @username in your Telegram settings.*
  <#else>
*Важно: У вашего аккаунта Telegram не указан username. Вы можете создавать и искать обмены, но другие игроки не увидят ваши предложения, пока не зададите @username в настройках Telegram.*
  </#if>

</#if>
<#if data.hasServers>
  <#if isEn>
Tap a server to view exchange request details.
Servers marked with a ✅ on the buttons are those where you have active requests.
  <#else>
Выберите сервер, чтобы посмотреть заявки на обмен.
Символом ✅ на кнопках помечены сервера на которых у вас есть активные заявки
  </#if>
<#else>
  <#if isEn>
No exchangers request saved yet.
  <#else>
Сохранённых заявок пока нет.
  </#if>
</#if>
