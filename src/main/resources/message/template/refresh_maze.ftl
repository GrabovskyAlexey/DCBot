<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Maze progress has been reset
<#else>
Прогресс прохождения лабиринта сброшен
</#if>