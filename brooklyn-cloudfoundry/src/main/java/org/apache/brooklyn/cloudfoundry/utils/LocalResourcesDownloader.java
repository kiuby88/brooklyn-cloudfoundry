/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.brooklyn.cloudfoundry.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Collection;

import org.apache.brooklyn.util.core.ResourceUtils;
import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.os.Os;
import org.apache.brooklyn.util.text.Strings;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

public class LocalResourcesDownloader {

    public static final String BROOKLYN_DIR = "brooklyn";
    public static final Logger log = LoggerFactory
            .getLogger(LocalResourcesDownloader.class);

    public static File downloadResourceInLocalDir(String saveAs, Collection<String> urls) {
        for (String url : urls) {
            try {
                File file = downloadResourceInLocalDir(saveAs, url);
                if (file.exists()) {
                    return file;
                }
            } catch (Exception e) {
                log.warn("Error downloading url {} by LocalResourceDownloader", url);
            }
        }
        throw new PropagatedRuntimeException(new FileNotFoundException("Any file was found by " +
                "LocalResourceDownloader"));
    }

    public static File downloadResourceInLocalDir(String saveAs, String url) {
        File localResource = createLocalFilePathName(saveAs);
        LocalResourcesDownloader.downloadResource(url, localResource);
        return localResource;
    }

    public static String findATmpDir() {
        String osTmpDir = new Os.TmpDirFinder().get().get();
        return osTmpDir + File.separator +
                BROOKLYN_DIR + File.separator +
                Strings.makeRandomId(8);
    }

    private static File createLocalFilePathName(String fileName) {
        return new File(createLocalPathName(fileName));
    }

    @SuppressWarnings("all")
    private static String createLocalPathName(String fileName) {
        String targetDirName = LocalResourcesDownloader.findATmpDir();
        String filePathName = targetDirName + File.separator + fileName;

        File targetDir = new File(targetDirName);
        targetDir.mkdirs();

        return filePathName;
    }

    public static void downloadResource(String url, File target) {
        try {
            FileUtils.copyInputStreamToFile(new ResourceUtils(null).getResourceFromUrl(url), target);
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

}
