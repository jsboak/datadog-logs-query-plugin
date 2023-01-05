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

class datadogUtil {

    static final String[] TIME_UNITS = ["Seconds","Minutes","Hours","Days","Weeks"]

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

        return timestamps
    }

    static void query(ApiClient datadogApiClient, String query, int numLogs, String startTime, String endTime, indexes){

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

        try {
            LogsListResponse result = logsApi.listLogs(new LogsApi.ListLogsOptionalParameters().body(body));

            for (Log ddLog : result.getData()) {

                println("Timestamp: " + new Date(ddLog.getAttributes().timestamp.toEpochSecond().toLong()*1000))
                println("Host: " + ddLog.getAttributes().host)
                println("Service: " + ddLog.getAttributes().service)
                println("Tags: " + ddLog.getAttributes().tags)

                if(ddLog.getAttributes().message == null) {

                    println("Content: " + ddLog.getAttributes().attributes + "\n")
                }
                else {
                    println("Content: " + ddLog.getAttributes().message + "\n")
                }

            }

        } catch (ApiException e) {
            System.err.println("Exception when calling LogsApi#listLogs");
            System.err.println("Status code: " + e.getCode());
            System.err.println("Reason: " + e.getResponseBody());
            System.err.println("Response headers: " + e.getResponseHeaders());
            e.printStackTrace();
        }
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
