<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if (data.username)??>
<#if isEn>
Welcome, *${data.username}*!
<#else>
Добро пожаловать, *${data.username}*!
</#if>
<#else>
</#if>
<#if isEn>
This is a helper bot for Dungeon Crusher.

It can notify you about sieges. Subscribe to the servers you care about with /subscribe.

The bot can remember your position in the maze—call /maze and mirror your path both in the game and in the bot.

It tracks maps, voids, and DH per server via /resources.

You can also configure mine notifications or shift siege alerts 5 minutes earlier with /settings.
<#else>
Это - бот-помощник для игры Dungeon Crusher.

Он может уведомлять тебя об осадах. Для этого нужно подписаться на интересующие тебя сервера с помощью команды /subscribe.

Бот может запоминать твою позицию в лабиринте - для этого вызови команду /maze и дублируй свой путь как в игре, так и в боте.

Учёт матрёшек, пустот и КБ по серверам ведётся через команду /resources.

Также можно настроить уведомления о КШ или перенести уведомление об осаде за 5 минут до её начала с помощью команды /settings.
</#if>