package com.plugin.datadogLogsQueryPlugin

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

@Plugin(name = "datadog-query-logs", service = ServiceNameConstants.WorkflowStep)
@PluginDescription(title = PROVIDER_TITLE, description = PROVIDER_DESCRIPTION)
@CompileDynamic
public class datadogLogsQueryPlugin implements StepPlugin{
    public static final String PROVIDER_TITLE = "Datadog / Query Logs"
    public static final String PROVIDER_DESCRIPTION = "Query Datadog Logs..."

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

    @PluginProperty(title = "Query String", description = "Logs Query", required = true, scope = PropertyScope.Instance)
    @RenderingOption(key = "displayType", value = "CODE")
    String query

    @PluginProperty(title = "Number of Logs", description = "Number of logs to retrieve from Datadog", required = true, scope = PropertyScope.InstanceOnly, defaultValue = "5")
    String numberOfLogs

    @PluginProperty(title = "Unit of Time", description = "Select the unit of time measurement for the time range of the query", required = true, scope = PropertyScope.InstanceOnly, defaultValue = "Minutes")
    @SelectValues(values = ["Seconds","Minutes","Hours","Days","Weeks"], freeSelect = false)
    String timeUnit

    @PluginProperty(title = "Time Range", description = "Specify the relative time range to apply to the query. E.g. past 5 minutes.", required = true, scope = PropertyScope.InstanceOnly, defaultValue = "5")
    String timeRange

    @PluginProperty(title = "Index", description = "For customers with multiple indexes, the indexes to search.  Default of * means all indexes.", required = true, scope = PropertyScope.InstanceOnly, defaultValue = "*")
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

        HashMap<String, Object> output = datadogUtil.query(apiClient, query, numLogs, startTime as String, endTime as String, indexes, context)
        Map<String, String> meta = new HashMap<>();
        meta.put("content-data-type", "application/json");

        context.getExecutionContext().executionLogger.log(2, JsonOutput.toJson(output), meta)

    }

}
