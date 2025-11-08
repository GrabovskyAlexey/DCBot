<#assign location = data.location?if_exists>
<#if location??>
    <#if data.isComplete()>
*Congratulations, you have completed the maze*
    <#else>
Current location:
Level: ${location.level}
Direction: <#if location.direction == "LEFT">L${location.offset}<#elseif location.direction == "RIGHT">R${location.offset}<#else>0</#if>
        <#if data.showHistory>
            <#if data.steps?has_content>
*Last 20 steps*
                <#list data.steps as step>
*${step?counter}.* ${step}
                </#list>
            <#else>
No step data
            </#if>
        </#if>
    </#if>
<#else>
No data about maze progress
</#if>
