/*
 *
 * Copyright (c) 2016 SERENA Software, Inc. All Rights Reserved.
 *
 * This software is proprietary information of SERENA Software, Inc.
 * Use is subject to license terms.
 *
 */
package com.serena.rlc.provider.filesystem;

import com.serena.rlc.provider.annotations.*;
import com.serena.rlc.provider.data.model.IActionInfo;
import com.serena.rlc.provider.domain.*;
import com.serena.rlc.provider.exceptions.ProviderException;
import com.serena.rlc.provider.filesystem.domain.Directory;
import com.serena.rlc.provider.filesystem.exception.FilesystemClientException;
import com.serena.rlc.provider.spi.IExecutionProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesystemExecutionProvider extends FilesystemBaseProvider implements IExecutionProvider {

    static final Logger logger = LoggerFactory.getLogger(FilesystemExecutionProvider.class);

    protected String srcDir;
    protected String destDir;
    protected String deleteDir;
    protected boolean preserveDates = true;
    protected boolean ignoreNotExists = true;
    protected String execScript;
    protected String execDir;
    protected String execParams;

    //================================================================================
    // Configuration Properties
    // -------------------------------------------------------------------------------
    // The configuration properties are marked with the @ConfigProperty annotaion
    // and will be displayed in the provider administration page when creating a
    // configuration of this plugin for use.
    //================================================================================

    @ConfigProperty(name = "execution_provider_name",
            displayName = "Execution - Provider Name",
            description = "provider name",
            defaultValue = "Filesystem Execution Provider",
            dataType = DataType.TEXT
    )
    private String providerName;

    @ConfigProperty(name = "execution_provider_description",
            displayName = "Execution - Provider Description",
            description = "provider description",
            defaultValue = "",
            dataType = DataType.TEXT
    )
    private String providerDescription;

    @Override
    public String getProviderName() {
        return this.providerName;
    }

    @Autowired(required = false)
    public void setProviderName(String providerName) {
        if (StringUtils.isNotEmpty(providerName)) {
            providerName = providerName.trim();
        }
        this.providerName = providerName;
    }

    @Override
    public String getProviderDescription() {
        return this.providerDescription;
    }

    @Autowired(required = false)
    public void setProviderDescription(String providerDescription) {
        if (StringUtils.isNotEmpty(providerDescription)) {
            providerDescription = providerDescription.trim();
        }

        this.providerDescription = providerDescription;
    }

    //================================================================================
    // IExecutionProvider Overrides
    //================================================================================

    @Service(name = EXECUTE, displayName = "Execute", description = "Execute Filesystem action.")
    @Params(params = {@Param(fieldName = ACTION, displayName = "Action", description = "Filesystem action to execute", required = true, dataType = DataType.SELECT),
            @Param(fieldName = PROPERTIES, description = "Filesystem action properties", required = true)
    })
    public ExecutionInfo execute(String action, String taskTitle, String taskDescription, List<Field> properties) throws ProviderException {
        if (action.equalsIgnoreCase(COPY_DIR))
            return localCopy(properties, false);
        else if (action.equalsIgnoreCase(DELETE_DIR))
            return localDelete(properties, false);
        else if (action.equalsIgnoreCase(EXEC_SCRIPT))
            return localExec(properties, false);

        throw new ProviderException("Unsupported execution action: " + action);
    }

    @Service(name = VALIDATE, displayName = "Validate", description = "Validate Filesystem action.")
    @Params(params = {@Param(fieldName = ACTION, description = "Filesystem action to validate", required = true, dataType = DataType.SELECT),
            @Param(fieldName = PROPERTIES, description = "Filesystem action properties", required = true)
    })
    @Override
    public ExecutionInfo validate(String action, String taskTitle, String taskDescription, List<Field> properties) throws ProviderException {
        if (action.equalsIgnoreCase(COPY_DIR))
            return localCopy(properties, true);
        else if (action.equalsIgnoreCase(DELETE_DIR))
            return localDelete(properties, true);
        else if (action.equalsIgnoreCase(EXEC_SCRIPT))
            return localExec(properties, true);

        throw new ProviderException("Unsupported execution action: " + action);
    }

    @Override
    public FieldInfo getFieldValues(String fieldName, List<Field> properties)
            throws ProviderException {

        if (fieldName.equalsIgnoreCase(ACTION)) {
            FieldInfo fieldInfo;
            List<FieldValueInfo> values;
            ActionInfoResult results = getActions();
            if (results == null || results.getTotal() < 1)
                return null;

            fieldInfo = new FieldInfo(fieldName);
            values = new ArrayList<FieldValueInfo>();
            for (IActionInfo actionInfo : results.getResults()) {
                values.add(new FieldValueInfo("name", actionInfo.getAction()));
            }

            fieldInfo.setValues(values);
            return fieldInfo;
        }

        throw new ProviderException("Unsupported get values for field name: " + fieldName);
    }

    public Boolean validateCopyDir(List<Field> properties) throws ProviderException {
        if (properties == null || properties.size() < 1)
            throw new ProviderException("Missing required field properties!");

        Field field = Field.getFieldByName(properties, SRC_DIR);
        if (field == null || StringUtils.isEmpty(field.getValue())) {
            throw new ProviderException("Task needs to be related to deployment unit");
        } else {
            String depUnit = field.getValue();
            srcDir = this.getBaseDir() + File.separator + depUnit;
            if (!filesystemClient.directoryExists(srcDir))
                throw new ProviderException("Directory " + srcDir + " does not exist");
            logger.debug("Using deployment unit source directory: " + srcDir);
        }

        field = Field.getFieldByName(properties, DEST_DIR);
        if (field == null || StringUtils.isEmpty(field.getValue())) {
            throw new ProviderException("A destination directory needs to be supplied");
        } else {
            this.destDir = field.getValue();
            if (!filesystemClient.directoryExists(destDir))
                throw new ProviderException("Directory " + destDir + " does not exist");
            logger.debug("Using destination directory: " + destDir);
        }

        field = Field.getFieldByName(properties, PRESERVE_DATES);
        if (field != null && StringUtils.isNotEmpty(field.getValue())) {
            preserveDates = Boolean.parseBoolean(field.getValue());
            logger.debug("Using preserve dates option: " + preserveDates);
        }

        return true;
    }

    @Action(name = COPY_DIR, displayName = "Copy Directory", description = "Execute Local Copy action.")
    @Params(params = {
            @Param(fieldName = DEST_DIR, displayName = "Destination Directory", description = "Destination Directory", required = true, environmentProperty = true, deployUnit = false, dataType = DataType.TEXT),
            @Param(fieldName = PRESERVE_DATES, displayName = "Preserve Dates", description = "Try and preserve dates and time Files and Directories", required = false, deployUnit = false, dataType = DataType.BOOLEAN, defaultValue = "true"),
            @Param(fieldName = SRC_DIR, displayName = "Source Directory", description = "Source Directory", required = true, deployUnit = true, dataType = DataType.SELECT)
    })
    public ExecutionInfo localCopy(List<Field> properties, Boolean validateOnly) throws ProviderException {
        ExecutionInfo execInfo = new ExecutionInfo();
        try {
            Boolean bValid = validateCopyDir(properties);
            if (validateOnly) {
                execInfo.setSuccess(bValid);
                execInfo.setMessage("Valid Filesystem action: " + COPY_DIR);
                return execInfo;
            }

            //execInfo.setStatus(ExecutionStatus.IN_PROGRESS);
            filesystemClient.localCopy(srcDir, destDir, preserveDates);
            execInfo.setSuccess(true);
            execInfo.setStatus(ExecutionStatus.COMPLETED);
            return execInfo;

        } catch (FilesystemClientException e) {
            execInfo.setSuccess(false);
            if (validateOnly) {
                execInfo.setMessage(e.getLocalizedMessage());
            } else {
                execInfo.setMessage("Unable to execute Filesystem action: " + COPY_DIR);
                execInfo.setStatus(ExecutionStatus.FAILED);
            }
            return execInfo;

        } catch (ProviderException e) {
            if (validateOnly) {
                execInfo.setSuccess(false);
                execInfo.setMessage(e.getLocalizedMessage());
                return execInfo;
            }

            throw e;
        }
    }

    public Boolean validateDeleteDir(List<Field> properties) throws ProviderException {
        if (properties == null || properties.size() < 1)
            throw new ProviderException("Missing required field properties!");

        Field field = Field.getFieldByName(properties, DELETE_DIR);
        if (field == null || StringUtils.isEmpty(field.getValue())) {
            throw new ProviderException("Task needs a source directory to delete");
        } else {
            deleteDir = field.getValue();
            if (!filesystemClient.directoryExists(destDir))
                throw new ProviderException("Directory " + deleteDir + " does not exist");
            logger.debug("Using delete directory: " + deleteDir);
        }

        field = Field.getFieldByName(properties, IGNORE_ERRORS);
        if (field != null && StringUtils.isNotEmpty(field.getValue())) {
            ignoreNotExists = Boolean.parseBoolean(field.getValue());
            logger.debug("Using ignore not exists option: " + ignoreNotExists);
        }

        return true;
    }

    @Action(name = DELETE_DIR, displayName = "Delete Directory (local)", description = "Execute Local Delete action.")
    @Params(params = {
            @Param(fieldName = DELETE_DIR, displayName = "Base Directory", description = "Directory to be deleted", required = true, environmentProperty = true, deployUnit = false, dataType = DataType.TEXT),
            @Param(fieldName = IGNORE_ERRORS, displayName = "Ignore Not Exists", description = "Ignore error if the directory does not exist", required = false, deployUnit = false, dataType = DataType.BOOLEAN, defaultValue = "true"),
    })
    public ExecutionInfo localDelete(List<Field> properties, Boolean validateOnly) throws ProviderException {
        ExecutionInfo execInfo = new ExecutionInfo();
        try {
            Boolean bValid = validateDeleteDir(properties);
            if (validateOnly) {
                execInfo.setSuccess(bValid);
                execInfo.setMessage("Valid Filesystem action: " + DELETE_DIR);
                return execInfo;
            }

            filesystemClient.localDelete(deleteDir, ignoreNotExists);
            execInfo.setSuccess(true);
            execInfo.setStatus(ExecutionStatus.COMPLETED);
            return execInfo;

        } catch (FilesystemClientException e) {
            execInfo.setSuccess(false);
            if (validateOnly) {
                execInfo.setMessage(e.getLocalizedMessage());
            } else {
                execInfo.setMessage("Unable to execute Filesystem action: " + DELETE_DIR);
                execInfo.setStatus(ExecutionStatus.FAILED);
            }
            return execInfo;

        } catch (ProviderException e) {
            if (validateOnly) {
                execInfo.setSuccess(false);
                execInfo.setMessage(e.getLocalizedMessage());
                return execInfo;
            }

            throw e;
        }
    }

    public Boolean validateExecScript(List<Field> properties) throws ProviderException {
        if (properties == null || properties.size() < 1)
            throw new ProviderException("Missing required field properties!");

        Field field = Field.getFieldByName(properties, EXEC_DIR);
        if (field == null || StringUtils.isEmpty(field.getValue())) {
            throw new ProviderException("Task needs an execution directory");
        } else {
            execScript = field.getValue();
            if (!filesystemClient.directoryExists(destDir))
                throw new ProviderException("Directory " + execDir + " does not exist");
            logger.debug("Using execution directory: " + execDir);
        }

        field = Field.getFieldByName(properties, EXEC_SCRIPT);
        if (field == null || StringUtils.isEmpty(field.getValue())) {
            throw new ProviderException("Task needs an execution script");
        } else {
            execScript = field.getValue();
            logger.debug("Using execution script: " + execScript);
        }

        field = Field.getFieldByName(properties, EXEC_PARAMS);
        if (field != null && StringUtils.isNotEmpty(field.getValue())) {
            execParams = field.getValue();
            logger.debug("Using execution params: " + execParams);
        }

        field = Field.getFieldByName(properties, IGNORE_ERRORS);
        if (field != null && StringUtils.isNotEmpty(field.getValue())) {
            ignoreNotExists = Boolean.parseBoolean(field.getValue());
            logger.debug("Using ignore errors option: " + ignoreNotExists);
        }

        return true;
    }

    @Action(name = EXEC_SCRIPT, displayName = "Execute Script (local)", description = "Execute Local Script action.")
    @Params(params = {
            @Param(fieldName = EXEC_DIR, displayName = "Execution Directory", description = "Directory containing script to be executed", required = true, environmentProperty = true, deployUnit = false, dataType = DataType.TEXTAREA),
            @Param(fieldName = EXEC_SCRIPT, displayName = "Execution Script", description = "Script to be executed", required = true, environmentProperty = true, deployUnit = false, dataType = DataType.TEXTAREA),
            @Param(fieldName = EXEC_PARAMS, displayName = "Script Parameters", description = "The parameters or arguments to be passed to the script", required = false, environmentProperty = true, deployUnit = false, dataType = DataType.TEXTAREA, defaultValue = "true"),
            @Param(fieldName = IGNORE_ERRORS, displayName = "Ignore Errors", description = "Ignore errors if the script fails", required = false, deployUnit = false, dataType = DataType.BOOLEAN, defaultValue = "true"),
    })
    public ExecutionInfo localExec(List<Field> properties, Boolean validateOnly) throws ProviderException {
        ExecutionInfo execInfo = new ExecutionInfo();
        try {
            Boolean bValid = validateExecScript(properties);
            if (validateOnly) {
                execInfo.setSuccess(bValid);
                execInfo.setMessage("Valid Filesystem action: " + EXEC_SCRIPT);
                return execInfo;
            }

            //execInfo.setStatus(ExecutionStatus.IN_PROGRESS);
            filesystemClient.localExec(execScript, execDir, execParams, ignoreNotExists);
            execInfo.setSuccess(true);
            execInfo.setStatus(ExecutionStatus.COMPLETED);
            return execInfo;

        } catch (FilesystemClientException e) {
            execInfo.setSuccess(false);
            if (validateOnly) {
                execInfo.setMessage(e.getLocalizedMessage());
            } else {
                execInfo.setMessage("Unable to execute Filesystem action: " + EXEC_SCRIPT);
                execInfo.setStatus(ExecutionStatus.FAILED);
            }
            return execInfo;

        } catch (ProviderException e) {
            if (validateOnly) {
                execInfo.setSuccess(false);
                execInfo.setMessage(e.getLocalizedMessage());
                return execInfo;
            }

            throw e;
        }
    }

    @Override
    public ExecutionInfo cancelExecution(ExecutionInfo executionInfo, String action, String taskTitle, String taskDescription, List<Field> properties) throws ProviderException {
        return new ExecutionInfo("Cancellation not required", true);
    }

    @Override
    public ExecutionInfo retryExecution(ExecutionInfo executionInfo, String action, String taskTitle, String taskDescription, List<Field> properties) throws ProviderException {
        return execute(action, taskTitle, taskDescription, properties);
    }

    @Override
    public ServiceInfo getServiceInfo(String service)
            throws ProviderException {

        return AnnotationUtil.getServiceInfo(this.getClass(), service);
    }

    @Override
    public ServiceInfoResult getServices()
            throws ProviderException {

        List<ServiceInfo> services = AnnotationUtil.getServices(this.getClass());
        return new ServiceInfoResult(0, services.size(), services.toArray(new ServiceInfo[services.size()]));
    }

    @Override
    public ActionInfo getActionInfo(String action)
            throws ProviderException {

        return AnnotationUtil.getActionInfo(this.getClass(), action);
    }

    @Override
    public ActionInfoResult getActions()
            throws ProviderException {

        List<ActionInfo> actions = AnnotationUtil.getActions(this.getClass());
        return new ActionInfoResult(0, actions.size(), actions.toArray(new ActionInfo[actions.size()]));
    }

    @Override
    public FieldValuesGetterFunction findFieldValuesGetterFunction(String fieldName)
            throws ProviderException {

        return AnnotationUtil.findFieldValuesGetterFunction(this.getClass(), fieldName);
    }

    @Override
    public FieldValuesGetterFunctionResult findFieldValuesGetterFunctions()
            throws ProviderException {
        List<FieldValuesGetterFunction> getters = AnnotationUtil.findFieldValuesGetterFunctions(this.getClass());
        return new FieldValuesGetterFunctionResult(0, getters.size(), getters.toArray(new FieldValuesGetterFunction[getters.size()]));
    }

    @Override
    public ConfigurationPropertyResult getConfigurationProperties() throws ProviderException {

        List<ConfigurationProperty> configProps = AnnotationUtil.getConfigurationProperties(this.getClass(), this);
        return new ConfigurationPropertyResult(0, configProps.size(), configProps.toArray(new ConfigurationProperty[configProps.size()]));
    }

}
