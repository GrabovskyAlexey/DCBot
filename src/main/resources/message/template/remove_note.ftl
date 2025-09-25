<#assign lang = (locale?string?lower_case)!''>
<#assign isEn = lang?starts_with('en')>
<#if isEn>
Enter the note number you want to delete
<#else>
Введите номер заметки которую хотите удалить
</#if>
