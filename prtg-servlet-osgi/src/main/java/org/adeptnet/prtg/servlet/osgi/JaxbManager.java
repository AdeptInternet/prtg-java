/*
 * Copyright 2016 Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.adeptnet.prtg.servlet.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.osgi.framework.Bundle;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class JaxbManager extends org.adeptnet.prtg.config.xml.JaxbManager {

    private static final Logger LOG = Logger.getLogger(JaxbManager.class.getName());

    private final Bundle bundle;

    public JaxbManager(final Bundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public InputStream getResourceAsStream(final String name) {
        final URL url = bundle.getResource(name);
        try {
            return url.openStream();
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }

}
