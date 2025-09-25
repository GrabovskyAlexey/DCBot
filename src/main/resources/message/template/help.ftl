<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
This is a helper bot for Dungeon Crusher—it never eats or sleeps, but it's always ready to help!

What it can do:

Notify you about sieges. Subscribe to the servers you care about with /subscribe so you never miss a siege again!

Remember your position in the maze. Just send /maze and mirror your path—even if your memory is like a goldfish's.

Track voids, maps, and other useful stuff via /resources.

It can also mute notifications for servers that are already cleared this week—use /resources to avoid extra pings.

In /settings you can enable mine notifications, shift siege alerts 5 minutes earlier (enough time to make tea), and even activate DH tracking—just in case.
<#else>
Это бот-помощник для игры Dungeon Crusher - не ест, не спит, но всегда готов помочь!

Что он умеет:

Уведомлять об осадах. Подпишись на интересующие тебя сервера командой /subscribe, и больше ни одна осада не пройдёт мимо!

Запоминать твою позицию в лабиринте. Просто напиши /maze и дублируй свой путь - да, даже если у тебя память как у золотой рыбки.

Вести учёт пустот, матрёшек и другой полезной ерунды через /resources.

А ещё умеет отключать уведомления по серверам, где на этой неделе всё уже наловлено и набито - воспользуйся /resources, чтобы не получать лишние пинги.

В /settings можно включить уведомления о КШ, перенести оповещение об осаде на 5 минут раньше (чтобы успеть сделать чай) и даже активировать учёт КБ - вдруг пригодится.
</#if>