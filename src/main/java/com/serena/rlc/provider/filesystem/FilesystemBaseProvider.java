/*
 *
 * Copyright (c) 2016 SERENA Software, Inc. All Rights Reserved.
 *
 * This software is proprietary information of SERENA Software, Inc.
 * Use is subject to license terms.
 *
 */
package com.serena.rlc.provider.filesystem;


import com.serena.rlc.provider.annotations.ConfigProperty;
import com.serena.rlc.provider.domain.DataType;
import com.serena.rlc.provider.domain.Field;
import com.serena.rlc.provider.domain.SessionData;
import com.serena.rlc.provider.filesystem.client.FilesystemClient;
import com.serena.rlc.provider.spi.IBaseServiceProvider;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public abstract class FilesystemBaseProvider implements IBaseServiceProvider {

    static final Logger logger = LoggerFactory.getLogger(FilesystemBaseProvider.class);

    final static String DIR_NAME = "dirName";
    final static String DIR_NAME_FILTER = "dirNameFilter";
    static final String SRC_DIR = "sourceDir";
    static final String DEST_DIR = "destinationDir";
    static final String DELETE_DIR = "deleteDir";
    static final String COPY_DIR = "copyDir";
    static final String EXEC_DIR = "execDir";
    static final String EXEC_SCRIPT = "execScript";
    static final String EXEC_PARAMS = "execParams";
    static final String PRESERVE_DATES = "preserveDates";
    static final String IGNORE_ERRORS = "ignoreErrors";

    private SessionData session;
    private Long providerId;
    private String providerUuid;
    private String providerNamespaceId;

    @Autowired
    FilesystemClient filesystemClient;

    //================================================================================
    // Configuration Properties
    // -------------------------------------------------------------------------------
    // The configuration properties are marked with the @ConfigProperty annotaion
    // and will be displayed in the provider administration page when creating a
    // configuration of this plugin for use.
    //================================================================================

    @Override
    public void setSession(SessionData session) {
        this.session = null;
    }

    public SessionData getSession() {
        return session;
    }

    @Override
    public Long getProviderId() {
        return providerId;
    }

    @Override
    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }

    @Override
    public String getProviderNamespaceId() {
        return providerNamespaceId;
    }

    @Override
    public void setProviderNamespaceId(String providerNamespaceId) {
        this.providerNamespaceId = providerNamespaceId;
    }


    @Override
    public String getProviderUuid() {
        return providerUuid;
    }

    @Override
    public void setProviderUuid(String providerUuid) {
        this.providerUuid = providerUuid;
    }

    @ConfigProperty(name = "deploy_unit_base_dir", displayName = "Filesystem Base Directory",
            description = "Filesystem Base Directory.",
            defaultValue = "\\\\<SERVER_NAME\\serena-provider-filesystem",
            dataType = DataType.TEXT)
    private String baseDir;

    @Autowired(required = false)
    public void setBaseDir(String baseDir) {
        if (StringUtils.isNotEmpty(baseDir))
            baseDir.trim();
        this.baseDir = baseDir;
    }

    public String getBaseDir() {
        return this.baseDir;
    }

    //================================================================================
    // Protected Methods
    //================================================================================

    protected void addField(List<Field> fieldCollection, String fieldName, String fieldDisplayName, String fieldValue) {
        if (StringUtils.isNotEmpty(fieldValue)) {
            Field field = new Field(fieldName, fieldDisplayName);
            field.setValue(fieldValue);
            fieldCollection.add(field);
        }
    }

    protected void setFilesystemConnectionDetails() {
        filesystemClient.createConnection(getSession(), getBaseDir());
    }

    //================================================================================
    // Private Methods
    //================================================================================

}
