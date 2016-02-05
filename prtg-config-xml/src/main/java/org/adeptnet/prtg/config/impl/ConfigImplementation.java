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
package org.adeptnet.prtg.config.impl;

import org.adeptnet.prtg.config.ConfigInterface;
import org.adeptnet.prtg.config.xml.Config;
import org.adeptnet.prtg.config.xml.JaxbManager;
import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class ConfigImplementation implements ConfigInterface {

    private static final Logger LOG = Logger.getLogger(ConfigImplementation.class.getName());
    private Config config;
    private String xml;
    private long xmlModified;
    private String jmxInitialContext;
    private final JaxbManager jaxb;

    public ConfigImplementation(final JaxbManager jaxb) {
        this.jaxb = jaxb;
    }

    @Override
    public Config getConfig() throws JAXBException {
        if (config == null) {
            final File f = new File(xml);
            xmlModified = f.lastModified();
            config = jaxb.toConfig(f);
            LOG.log(Level.INFO, String.format("getConfig(): Loaded config from (%s)", xml));
        } else if (xml != null) {
            final File f = new File(xml);
            if (f.lastModified() != xmlModified) {
                config = null;
                return getConfig();
            }
        }
        return config;
    }

    public void setConfig(final Config config) {
        this.config = config;
        this.xml = null;
    }

    public String getXml() {
        return xml;
    }

    public void setXml(final String xml) {
        this.xml = xml;
        this.config = null;
    }

    public ConfigImplementation withConfig(final Config config) {
        setConfig(config);
        return this;
    }

    public ConfigImplementation withXml(final String xml) {
        setXml(xml);
        return this;
    }

    @Override
    public String getJmxInitialContext() {
        return jmxInitialContext;
    }

    public void setJmxInitialContext(final String jmxInitialContext) {
        this.jmxInitialContext = jmxInitialContext;
    }

    public ConfigImplementation withJmxInitialContext(final String jmxInitialContext) {
        setJmxInitialContext(jmxInitialContext);
        return this;
    }
}
