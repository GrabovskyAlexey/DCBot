<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Enter a note
<#else>
Введите заметку
</#if>
