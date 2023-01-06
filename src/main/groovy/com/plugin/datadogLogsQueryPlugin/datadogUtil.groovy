package com.plugin.datadogLogsQueryPlugin

import com.datadog.api.client.ApiClient
import com.datadog.api.client.ApiException
import com.datadog.api.client.v2.api.LogsApi
import com.datadog.api.client.v2.model.Log
import com.datadog.api.client.v2.model.LogsListRequest
import com.datadog.api.client.v2.model.LogsListRequestPage
import com.datadog.api.client.v2.model.LogsListResponse
import com.datadog.api.client.v2.model.LogsQueryFilter
import com.datadog.api.client.v2.model.LogsSort
import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.core.storage.ResourceMeta
import com.dtolabs.rundeck.plugins.step.PluginStepContext

import java.time.ZoneId
import java.time.format.DateTimeFormatter

class datadogUtil {

    static long[] getQueryTimeRange(int timeRange, String timeUnit) {
        long[] timestamps = new long[2]
        int multiplier

        switch (timeUnit) {
            case "Seconds": multiplier = 1
                break

            case "Minutes": multiplier = 60
                break

            case "Hours": multiplier = 3600
                break

            case "Days": multiplier = 86400
                break

            case "Weeks": multiplier = 604800
                break

            default:
                throw new IllegalStateException("Unexpected value: " + timeUnit)
        }

        long endTime = (long) System.currentTimeMillis()
        long startTime = endTime - (timeRange * multiplier * 1000l)

        timestamps[0] = startTime
        timestamps[1] = endTime

        println("Start Time: " + startTime)
        println("End Time: " + endTime)

        return timestamps
    }

    static HashMap<String,Object> query(ApiClient datadogApiClient, String query, int numLogs, String startTime, String endTime, String indexes, String timeZone){

        List<String> indexList = indexes.replace(" ","").split(",")

        LogsApi logsApi = new LogsApi(datadogApiClient)

        LogsListRequest body =
                new LogsListRequest()
                        .filter(
                                new LogsQueryFilter()
                                        .query(query)
                                        .indexes(indexList)
                                        .from(startTime)
                                        .to(endTime))
                        .sort(LogsSort.TIMESTAMP_ASCENDING)
                        .page(new LogsListRequestPage().limit(numLogs));
        Map<String,Object> logMap = [:]

        try {
            LogsListResponse result = logsApi.listLogs(new LogsApi.ListLogsOptionalParameters().body(body));

            for (Log ddLog : result.getData()) {

                String message
                if(ddLog.getAttributes().message == null) {
                    message = ddLog.getAttributes().attributes.toString()

                }
                else {
                    message = ddLog.getAttributes().message
                }

                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("MMM dd hh:mm:ss.SSS z").withZone(ZoneId.of(timeZone, ZoneId.SHORT_IDS));

                String logDate = ddLog.getAttributes().timestamp.format(timeFormatter)

                logMap.put(logDate,"Host: " + ddLog.getAttributes().host + "\n"
                        + "Service: " + ddLog.getAttributes().service + "\n"
                        + "Tags: " + ddLog.getAttributes().tags + "\n"
                        + "Message:\n" + message)
            }

        } catch (ApiException e) {
            System.err.println("Exception when calling LogsApi#listLogs");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }

        return logMap
    }

    static String getPasswordFromKeyStorage(String path, PluginStepContext context) throws StepException {
        try{
            ResourceMeta contents = context.getExecutionContext().getStorageTree().getResource(path).getContents();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            contents.writeContent(byteArrayOutputStream);
            String password = new String(byteArrayOutputStream.toByteArray());

            return password;
        }catch(Exception ignored){
            throw new StepException("can't find the password key storage path ${path}", Reason.ExampleReason);
        }
    }

    static enum Reason implements FailureReason{
        ExampleReason
    }

    static ApiClient datadogAuth(String apiKeyAuth, String appKeyAuth) {

        ApiClient defaultClient = ApiClient.getDefaultApiClient();
        HashMap<String, String> secrets = new HashMap<>();

        secrets.put("appKeyAuth", appKeyAuth);
        secrets.put("apiKeyAuth", apiKeyAuth);
        defaultClient.configureApiKeys(secrets)

        return defaultClient

    }

}
