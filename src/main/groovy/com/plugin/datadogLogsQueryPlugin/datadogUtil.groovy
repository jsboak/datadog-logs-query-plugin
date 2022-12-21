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

public class datadogUtil {

    static void query(ApiClient datadogApiClient, String query){

        LogsApi logsApi = new LogsApi(datadogApiClient)

        LogsListRequest body =
                new LogsListRequest()
                        .filter(
                                new LogsQueryFilter()
                                        .query(query)
                                        .indexes(Collections.singletonList("main"))
                                        .from("1671234572954")
                                        .to("1671235572954"))
                        .sort(LogsSort.TIMESTAMP_ASCENDING)
                        .page(new LogsListRequestPage().limit(10));

        try {
            LogsListResponse result = logsApi.listLogs(new LogsApi.ListLogsOptionalParameters().body(body));

            for (Log ddLog : result.getData()) {

                println(ddLog.getAttributes().message)

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
