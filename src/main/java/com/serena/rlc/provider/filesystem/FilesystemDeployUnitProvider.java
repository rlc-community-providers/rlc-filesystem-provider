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
import com.serena.rlc.provider.domain.*;
import com.serena.rlc.provider.exceptions.ProviderException;
import com.serena.rlc.provider.filesystem.domain.Directory;
import com.serena.rlc.provider.filesystem.exception.FilesystemClientException;
import com.serena.rlc.provider.spi.IDeployUnitProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesystemDeployUnitProvider extends FilesystemBaseProvider implements IDeployUnitProvider {

    static final Logger logger = LoggerFactory.getLogger(FilesystemDeployUnitProvider.class);

    //================================================================================
    // Configuration Properties
    // -------------------------------------------------------------------------------
    // The configuration properties are marked with the @ConfigProperty annotaion
    // and will be displayed in the provider administration page when creating a
    // configuration of this plugin for use.
    //================================================================================

    @ConfigProperty(name = "deploy_unit_provider_name",
            displayName = "Deploy Unit - Provider Name",
            description = "provider name",
            defaultValue = "Filesystem Deploy Unit Provider",
            dataType = DataType.TEXT)
    private String providerName;

    @ConfigProperty(name = "deploy_unit_provider_description",
            displayName = "Deploy Unit - Provider Description",
            description = "provider description",
            defaultValue = "",
            dataType = DataType.TEXT)
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
    // IDeployUnitProvider Overrides
    //================================================================================

    @Override
    @Service(name = FIND_DEPLOY_UNITS, displayName = "Find Deploy Units", description = "Find versioned directories to use as deployment units.")
    @Params(params = {
            @Param(fieldName = DIR_NAME_FILTER, displayName = "Deploy Unit Name Filter", description = "Deploy Unit name filter."),})
    public ProviderInfoResult findDeployUnits(List<Field> properties, Long startIndex, Long resultCount) throws ProviderException  {
        String dirNameFilter = null;
        Field field = Field.getFieldByName(properties, DIR_NAME_FILTER);
        if (field != null) {
            dirNameFilter = field.getValue();
        }

        List<ProviderInfo> list = new ArrayList<>();

        setFilesystemConnectionDetails();
        try {
            List<Directory> directories = filesystemClient.getDirectories(dirNameFilter);
            if (directories != null) {
                ProviderInfo pDUInfo;
                for (Directory fsdir : directories) {
                    pDUInfo = new ProviderInfo(fsdir.getId(), fsdir.getName(), "Directory", fsdir.getName());
                    if (StringUtils.isEmpty(fsdir.getId())) {
                        pDUInfo.setId(fsdir.getName());
                    }
                    pDUInfo.setUrl("file:///" + this.getBaseDir() + File.separator + fsdir.getName());
                    pDUInfo.setDescription(fsdir.getDescription());
                    list.add(pDUInfo);
                }
            }
        } catch (FilesystemClientException e) {
            logger.debug(e.getLocalizedMessage());
            throw new ProviderException(e.getLocalizedMessage());
        }
        return new ProviderInfoResult(0, list.size(), list.toArray(new ProviderInfo[list.size()]));

    }

    @Service(name = GET_DEPLOY_UNIT, displayName = "Get Deploy Unit", description = "Get file system directory information.")
    @Params(params = {
            @Param(fieldName = DIR_NAME, displayName = "Directory Name", description = "Directory Name", required = true, deployUnit = true),}
    )
    public ProviderInfo getDeployUnit(Field property) throws ProviderException {
        if (StringUtils.isEmpty(property.getValue())) {
            throw new ProviderException("Missing required field: " + DIR_NAME);
        }

        setFilesystemConnectionDetails();
        try {
            Directory fsdir = filesystemClient.getDirectory(property.getValue());
            ProviderInfo pDUInfo = new ProviderInfo(fsdir.getId(), fsdir.getName(), "Directory", fsdir.getName());
            pDUInfo.setDescription(fsdir.getDescription());
            return pDUInfo;
        }
        catch (FilesystemClientException e) {
            logger.debug(e.getLocalizedMessage());
            throw new ProviderException(e.getLocalizedMessage());
        }

    }

    @Override
    public FieldInfo getFieldValues(String fieldName, List<Field> properties) throws ProviderException {
        switch (fieldName) {
            default: // ignore
        }

        return null;
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

    //================================================================================
    // Private Methods
    //================================================================================

}
