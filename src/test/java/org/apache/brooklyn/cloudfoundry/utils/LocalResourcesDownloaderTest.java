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


import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.brooklyn.util.exceptions.PropagatedRuntimeException;
import org.apache.brooklyn.util.os.Os;
import org.apache.brooklyn.util.text.Strings;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.Test;

public class LocalResourcesDownloaderTest {

    private static String ARTIFACT_NAME = "brooklyn-example-hello-world-sql-webapp-in-paas.war";
    private static String ARTIFACT_URL = "classpath://" + ARTIFACT_NAME;
    private static String TEMP_FOLDER = new Os.TmpDirFinder().get().get();

    @Test
    public void testDownloadResourceInLocalDir() {
        File localFile = LocalResourcesDownloader.downloadResourceInLocalDir(ARTIFACT_URL);
        assertTrue(localFile.exists());
        assertTrue(localFile.canRead());
        assertTrue(localFile.getAbsolutePath().startsWith(TEMP_FOLDER));
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testExceptionDownloadingInLocalDir() {
        LocalResourcesDownloader.downloadResourceInLocalDir(Strings.makeRandomId(2));
    }

    @Test
    public void testFindATmpDir() {
        assertTrue(LocalResourcesDownloader.findATmpDir().matches(TEMP_FOLDER + File.separator
                + LocalResourcesDownloader.BROOKLYN_DIR + File.separator + "[a-zA-Z0-9]+"));
    }

    @Test
    @SuppressWarnings("all")
    public void testDownloadResource() throws URISyntaxException, IOException {
        File artifact = new File(getClass().getClassLoader().getResource(ARTIFACT_NAME).toURI());
        File tmpFile = new File("tmp-file");
        LocalResourcesDownloader.downloadResource(ARTIFACT_URL, tmpFile);
        assertTrue(FileUtils.contentEquals(artifact, tmpFile));
        tmpFile.delete();
    }

    @Test(expectedExceptions = PropagatedRuntimeException.class)
    public void testExceptionDownloadingResource() throws URISyntaxException {
        File file = new File("tmp-file");
        LocalResourcesDownloader.downloadResource(Strings.makeRandomId(8), file);
    }
}
