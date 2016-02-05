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
package org.adeptnet.prtg.config.xml;

import java.io.File;
import java.util.List;
import javax.xml.bind.JAXBException;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class JaxbManager extends org.adeptnet.prtg.xml.JaxbManager {

    public JaxbManager() {
    }

    @Override
    public List<String> getSchemaNames() {
        final List<String> result = super.getSchemaNames();
        result.add(META_INF_PRTG + "prtg-config.xsd");
        return result;
    }

    @Override
    public List<Class<?>> getKnownClasses() {
        final List<Class<?>> result = super.getKnownClasses();
        result.add(org.adeptnet.prtg.config.xml.ObjectFactory.class);
        return result;
    }

    public Config toConfig(final File file) throws JAXBException {
        return fromXML(file, Config.class);
    }

}
