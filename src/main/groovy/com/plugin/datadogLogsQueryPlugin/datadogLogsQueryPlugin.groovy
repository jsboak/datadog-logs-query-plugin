package com.plugin.datadogLogsQueryPlugin

import com.fasterxml.jackson.databind.annotation.JsonAppend
import com.plugin.datadogLogsQueryPlugin.datadogUtil
import com.datadog.api.client.ApiClient
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.plugins.Plugin
import com.dtolabs.rundeck.core.plugins.configuration.PropertyScope
import com.dtolabs.rundeck.core.plugins.configuration.StringRenderingConstants
import com.dtolabs.rundeck.plugins.ServiceNameConstants
import com.dtolabs.rundeck.plugins.descriptions.PluginDescription
import com.dtolabs.rundeck.plugins.descriptions.PluginProperty
import com.dtolabs.rundeck.plugins.descriptions.RenderingOption
import com.dtolabs.rundeck.plugins.descriptions.RenderingOptions
import com.dtolabs.rundeck.plugins.descriptions.SelectValues
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.plugins.step.StepPlugin
import groovy.json.JsonOutput
import groovy.transform.CompileDynamic

import java.time.ZoneId

@Plugin(name = "datadog-query-logs", service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = PROVIDER_TITLE, description = PROVIDER_DESCRIPTION)
@CompileDynamic
public class datadogLogsQueryPlugin implements StepPlugin{
    public static final String PROVIDER_TITLE = "Datadog / Query Logs"
    public static final String PROVIDER_DESCRIPTION = "Query logs from Datadog by providing a logs query.\nFor details on Datadog's log search syntax, see [here](https://docs.datadoghq.com/logs/explorer/search_syntax/)."

    @PluginProperty(title = "App Key Auth", description = "Datadog App Key Auth", required = false, scope = PropertyScope.Instance)
    @RenderingOptions([
            @RenderingOption(
                    key = StringRenderingConstants.GROUP_NAME,
                    value = "Credentials"
            ),
            @RenderingOption(
                    key = StringRenderingConstants.SELECTION_ACCESSOR_KEY,
                    value = "STORAGE_PATH"
            ),
            @RenderingOption(
                    key = StringRenderingConstants.STORAGE_PATH_ROOT_KEY,
                    value = "keys"
            ),
            @RenderingOption(
                    key = StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY,
                    value = "Rundeck-data-type=password"
            )
    ])
    String appKeyAuth

    @PluginProperty(title = "API Key Auth", description = "Datadog API Key Auth", required = false, scope = PropertyScope.Instance)
    @RenderingOptions([
            @RenderingOption(
                    key = StringRenderingConstants.GROUP_NAME,
                    value = "Credentials"
            ),
            @RenderingOption(
                    key = StringRenderingConstants.SELECTION_ACCESSOR_KEY,
                    value = "STORAGE_PATH"
            ),
            @RenderingOption(
                    key = StringRenderingConstants.STORAGE_PATH_ROOT_KEY,
                    value = "keys"
            ),
            @RenderingOption(
                    key = StringRenderingConstants.STORAGE_FILE_META_FILTER_KEY,
                    value = "Rundeck-data-type=password"
            )
    ])
    String apiKeyAuth

    @PluginProperty(title = "Logs Query", description = "Insert the query to be used to surface logs from Datadog.\nUse Datadog's [Log Search Syntax](https://docs.datadoghq.com/logs/explorer/search_syntax/) to query logs.", required = true, scope = PropertyScope.Instance)
    @RenderingOption(key = "displayType", value = "CODE")
    String query

    @PluginProperty(title = "Convert to Time Zone", description = "The desired time zone to convert to for the output log entries.", required = true, defaultValue = "America/New_York", scope = PropertyScope.InstanceOnly)
    @SelectValues(values = ['Africa/Cairo', 'Africa/Lagos', 'America/Anchorage', 'America/Los_Angeles', 'America/Manaus', 'America/Mexico_City', 'America/New_York', 'America/Phoniex', 'America/Santiago', 'America/Sao_Paulo',
            'America/St_Johns', 'Asia/Baku', 'Asia/Karachi', 'Asia/Shanghai', 'Asia/Tehran', 'Asia/Tokyo', 'Australia/Brisbane', 'Australia/Sydney', 'Europe/London', 'Europe/Moscow', 'Pacific/Auckland'], freeSelect = false)
    String timeZone

    @PluginProperty(title = "Number of Logs", description = "Limit the number of logs to retrieve from Datadog", required = true, scope = PropertyScope.InstanceOnly, defaultValue = "5")
    String numberOfLogs

    @PluginProperty(title = "Unit of Time", description = "Select the unit of time measurement for the time range of the query", required = true, scope = PropertyScope.InstanceOnly, defaultValue = "Minutes")
    @SelectValues(values = ["Seconds","Minutes","Hours","Days","Weeks"], freeSelect = false)
    String timeUnit

    @PluginProperty(title = "Time Range", description = "Specify the relative time range to apply to the query. For example: past 5 minutes.", required = true, scope = PropertyScope.InstanceOnly, defaultValue = "5")
    String timeRange

    @PluginProperty(title = "Index", description = "For customers with multiple indexes, optionally provide a comma-separate list of indexes to search.\nDefault of * means all indexes. See [here](https://docs.datadoghq.com/logs/log_configuration/indexes/) for more details on indexes.", required = true, scope = PropertyScope.InstanceOnly, defaultValue = "*")
    @RenderingOptions([
            @RenderingOption(
                    key = StringRenderingConstants.GROUP_NAME,
                    value = "Advanced"
            ),
            @RenderingOption(
                    key = StringRenderingConstants.GROUPING,
                    value = "secondary"
            )
    ])
    String indexes

    @Override
    void executeStep(PluginStepContext context, Map<String, Object> configuration) throws StepException {

        long[] timestamps = datadogUtil.getQueryTimeRange(timeRange as int, timeUnit)
        long startTime = timestamps[0]
        long endTime = timestamps[1]

        ApiClient apiClient = datadogUtil.datadogAuth(datadogUtil.getPasswordFromKeyStorage(apiKeyAuth, context), datadogUtil.getPasswordFromKeyStorage(appKeyAuth, context))

        int numLogs = numberOfLogs.toInteger()

        HashMap<String, Object> output = datadogUtil.query(apiClient, query, numLogs, startTime as String, endTime as String, indexes, timeZone)
        Map<String, String> meta = new HashMap<>();
        meta.put("content-data-type", "application/json");

        context.getExecutionContext().executionLogger.log(2, JsonOutput.toJson(output), meta)

    }

}
