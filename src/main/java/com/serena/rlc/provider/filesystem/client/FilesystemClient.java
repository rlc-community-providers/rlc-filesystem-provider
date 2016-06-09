/*
 *
 * Copyright (c) 2015 SERENA Software, Inc. All Rights Reserved.
 *
 * This software is proprietary information of SERENA Software, Inc.
 * Use is subject to license terms.
 *
 */

package com.serena.rlc.provider.filesystem.client;

import com.serena.rlc.provider.domain.SessionData;
import com.serena.rlc.provider.filesystem.domain.Directory;
import com.serena.rlc.provider.filesystem.exception.FilesystemClientException;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * @author klee
 */

@Component
public class FilesystemClient {
    private static final Logger logger = LoggerFactory.getLogger(FilesystemClient.class);

    private String baseDir;
    private SessionData session;

    public FilesystemClient() {

    }

    public FilesystemClient(SessionData session, String baseDir, String dirOffset) {
        this.session = session;
        this.baseDir = baseDir;
    }

    public SessionData getSession() {
        return session;
    }

    public void setSession(SessionData session) {
        this.session = session;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public void createConnection(SessionData session, String baseDir) {
        this.session = session;
        this.baseDir = baseDir;
    }

    public ArrayList<Directory> getDirectories(String dirNameFilter) throws FilesystemClientException {
        logger.debug("Using Filesystem Base Directory: " + this.getBaseDir());
        logger.debug("Using Filesystem Directory Name Filter: " + (dirNameFilter != null && !dirNameFilter.isEmpty() ? dirNameFilter : "none defined"));

        String duDir = null;
        duDir = this.getBaseDir() + File.separator;
        logger.debug("Filesystem Deployment Unit directory: " + duDir);

        ArrayList directories = new ArrayList();

        File file = new File(duDir);
        String[] names = file.list();
        int count = 0;
        for (String name : names) {
            File fdir = new File(duDir + name);
            if (fdir.isDirectory()) {
                if (dirNameFilter != null && !dirNameFilter.isEmpty()) {
                    if (fdir.getName().contains(dirNameFilter)) {
                        Directory fsdir = new Directory(name, name, fdir.getAbsolutePath());
                        directories.add(fsdir);
                        logger.debug("Filtered Directory: " + name);
                    }
                } else {
                    Directory fsdir = new Directory(name, name, fdir.getAbsolutePath());
                    directories.add(fsdir);
                    logger.debug("Found Directory: " + name);
                }
            }
        }

        return directories;

    }

    public Directory getDirectory(String dirName) throws FilesystemClientException {
        logger.debug("Using Filesystem Directory: " + dirName);

        File dir = new File(dirName);
        Directory fsdir = null;
        if (dir.isDirectory()) {
            fsdir = new Directory(dir.getName(), dir.getName(), dir.getAbsolutePath());
        }

        return fsdir;
    }

    public boolean directoryExists(String dirname) {
        Path destination = Paths.get(dirname);
        if (!Files.exists(destination)) return false;
        return true;
    }

    public void localCopy(String srcFolderPath, String destFolderPath, boolean preserveDates) throws FilesystemClientException {
        File source = new File(srcFolderPath);
        File destination = new File(destFolderPath);

        try {
            if (!source.exists()) {
                throw new FilesystemClientException("Source directory " + srcFolderPath + " does not exist");
            }

            if (!destination.exists()) {
                logger.debug("Target directory " + destFolderPath + " does not exist, but it will be created...");
            }

            FileUtils.copyDirectory(source, destination, preserveDates);

        } catch (IOException e) {
            logger.debug(e.getLocalizedMessage());
            throw new FilesystemClientException(e.getLocalizedMessage());
        }

    }

    public void localDelete(String destFolderPath, boolean ignoreNotExists) throws FilesystemClientException {
        Path destination = Paths.get(destFolderPath);

        try {
            if (!Files.exists(destination)) {
                if (ignoreNotExists) {
                    logger.debug("Destination directory " + destFolderPath + " does not exist, ignoring...");
                } else {
                    throw new FilesystemClientException("Destination directory " + destFolderPath + " does not exist");
                }
            } else {
                FileUtils.deleteDirectory(destination.toFile());
            }

        } catch (IOException e) {
            logger.debug(e.getLocalizedMessage());
            throw new FilesystemClientException(e.getLocalizedMessage());
        }

    }

    public void localExec(String execScript, String execDir, String execParams, boolean ignoreErrors) throws FilesystemClientException {
        Path script = Paths.get(execDir + File.separatorChar + execScript);

        try {
            if (!Files.exists(script)) {
                if (ignoreErrors) {
                    logger.debug("Execution script " + script.toString() + " does not exist, ignoring...");
                } else {
                    throw new FilesystemClientException("Execution script " + script.toString() + " does not exist");
                }
            } else {
                ProcessBuilder pb = new ProcessBuilder(script.toString());
                pb.directory(new File(script.getParent().toString()));
                System.out.println(pb.directory().toString());
                logger.debug("Executing script " + execScript + " in directory " + execDir + " with parameters: " + execParams);
                Process p = pb.start();     // Start the process.
                p.waitFor();                // Wait for the process to finish.
                logger.debug("Executed script " + execScript + " successfully.");
            }
        } catch (Exception e) {
            logger.debug(e.getLocalizedMessage());
            throw new FilesystemClientException(e.getLocalizedMessage());
        }

    }

    static public void main(String[] args) {
        String versionPath = "C:\\Temp\\serena-provider-filesystem\\app-a";
        FilesystemClient fc = new FilesystemClient(null, versionPath, null);

        System.out.println("Retrieving Filesystem Directories...");
        ArrayList<Directory> directories = new ArrayList<Directory>();
        try {
            directories = fc.getDirectories(null);
        } catch (FilesystemClientException e) {
            System.out.print(e.toString());
        }
        for (Directory dir : directories) {
            System.out.println("Found directory: " + dir.getName());
        }

        System.out.println("Copying files...");
        try {
            fc.localCopy(versionPath+"\\v1.0", versionPath+"\\integration", true);
        } catch (FilesystemClientException e) {
            System.out.print(e.toString());
        }

        System.out.println("Executing Script...");
        try {
            fc.localExec("run.bat", versionPath+"\\integration", "", false);
        } catch (FilesystemClientException e) {
            System.out.print(e.toString());
        }

        System.out.println("Deleting files...");
        try {
            fc.localDelete(versionPath+"\\integration", false);
        } catch (FilesystemClientException e) {
            System.out.print(e.toString());
        }

    }
}
