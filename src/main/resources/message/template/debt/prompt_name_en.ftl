<#assign creation = data.creation>
${data.title}
<#if data.invalid?? && data.invalid>
Name must not be empty.
</#if>

*Selected:* ${creation.direction!"?"}, ${creation.server!"?"}, ${creation.resource!"?"}
Amount: <#if creation.amount??>${creation.amount}<#else>?</#if>
