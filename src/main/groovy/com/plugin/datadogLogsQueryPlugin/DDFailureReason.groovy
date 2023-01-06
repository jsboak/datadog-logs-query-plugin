package com.plugin.datadogLogsQueryPlugin

import com.dtolabs.rundeck.core.execution.workflow.steps.FailureReason

enum DDFailureReason implements FailureReason {

    AuthenticationError,
    KeyStorage

}
