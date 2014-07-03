/*
 * Copyright 2014 Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za).
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.adeptnet.prtg.config.SensorException;
import org.adeptnet.prtg.config.impl.ConfigImplementation;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class Activator implements BundleActivator {

    private static final Logger LOG = Logger.getLogger(Activator.class.getName());
    private static final String PREFIX = "org.adeptnet";
    private static final String CONFIG_PATH = PREFIX + ".config.path";
    private static final String CONFIG_FILE = PREFIX + ".cfg";
    private static final String OSGI_PREFIX = PREFIX + ".prtg.servlet.osgi";
    private static final String CONFIG = OSGI_PREFIX + ".config";
    private static final String ALIAS = OSGI_PREFIX + ".alias";
    private ServiceRegistration registration;
    private String path;
    private Properties properties;

    private Properties getProperties() throws SensorException {
        if (properties == null) {
            try {
                final File file = new File(path + "/" + CONFIG_FILE);
                final InputStream is = new FileInputStream(file);
                properties = new Properties();
                properties.load(is);
            } catch (IOException ex) {
                throw new SensorException("getProperties(): " + ex.getMessage(), ex);
            }
        }
        return properties;
    }

    private String getProperty(final String name) throws SensorException {
        final String result = getProperties().getProperty(name);
        if (result == null) {
            throw new SensorException(String.format("getProperty(): %s is null", name));
        }
        return result;
    }

    @Override
    public void start(BundleContext context) {
        try {
            LOG.log(Level.INFO, "ADEPTNET Starting: {0}", Activator.class);
            path = System.getProperty(CONFIG_PATH);
            if (path == null) {
                throw new SensorException(String.format("%s is null", CONFIG_PATH));
            }

            Servlet servlet = new Servlet(
                    new ConfigImplementation()
                    .withXml(getProperty(CONFIG))
            );

            Hashtable<String, Object> props = new Hashtable<>();
            props.put("alias", getProperty(ALIAS));
            registration = context.registerService(javax.servlet.Servlet.class.getName(), servlet, props);
            LOG.log(Level.INFO, "ADEPTNET Started: {0}", Activator.class);
        } catch (SensorException ex) {
            LOG.log(Level.SEVERE, "Cannot Start Activator: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void stop(BundleContext context) {
        LOG.log(Level.INFO, "ADEPTNET Stopping: {0}", Activator.class);
        if (registration != null) {
            registration.unregister();
        }
    }

}
