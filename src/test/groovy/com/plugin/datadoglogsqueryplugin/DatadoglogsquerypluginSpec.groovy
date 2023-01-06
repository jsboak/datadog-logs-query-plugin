package com.plugin.datadoglogsqueryplugin

import com.dtolabs.rundeck.core.execution.ExecutionContext
import com.dtolabs.rundeck.core.execution.ExecutionListener
import com.dtolabs.rundeck.core.storage.StorageTree
import com.dtolabs.rundeck.plugins.step.PluginStepContext
import com.dtolabs.rundeck.core.execution.workflow.steps.StepException
import com.dtolabs.rundeck.plugins.PluginLogger
import com.plugin.datadogLogsQueryPlugin.datadogLogsQueryPlugin
import org.rundeck.storage.api.Resource
import com.dtolabs.rundeck.core.storage.ResourceMeta


import spock.lang.Specification

class DatadoglogsquerypluginSpec extends Specification {

    def getContext(PluginLogger logger){
        Mock(PluginStepContext){
            getLogger()>>logger
        }
    }

    def "check Boolean parameter"(){

        given:

        def example = new datadogLogsQueryPlugin()
        def context = getContext(Mock(PluginLogger))
        def configuration = [example:"example123",exampleBoolean:"true"]

        when:
        example.executeStep(context,configuration)

        then:
        thrown StepException
    }

    def "run OK"(){

        given:

        def example = new datadogLogsQueryPlugin()
        def logger = Mock(PluginLogger)
        def context = getContext(logger)
        def configuration = [example:"example123",exampleBoolean:"false",exampleFreeSelect:"Beige"]

        when:
        example.executeStep(context,configuration)

        then:
        1 * logger.log(2, 'Example step configuration: {example=example123, exampleBoolean=false, exampleFreeSelect=Beige}')
    }

}