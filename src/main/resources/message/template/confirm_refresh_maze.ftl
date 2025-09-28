<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Are you sure you want to reset the maze progress?
<#else>
Вы уверены, что хотите сбросить прогресс прохождения лабиринта?
</#if>