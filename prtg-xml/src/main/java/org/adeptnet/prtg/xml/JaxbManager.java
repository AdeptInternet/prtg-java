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
package org.adeptnet.prtg.xml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.ext.EntityResolver2;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class JaxbManager implements ErrorHandler {

    private static final Logger LOG = Logger.getLogger(JaxbManager.class.getName());
    private final static String META_INF = "META-INF";
    protected final static String META_INF_PRTG = META_INF + "/prtg/";
    private String basePath;

    public List<String> getSchemaNames() {
        final List<String> result = new java.util.ArrayList<>();
        result.add(META_INF_PRTG + "xml.xsd");
        result.add(META_INF_PRTG + "prtg.xsd");
        return result;
    }

    public InputStream getResourceAsStream(final String name) {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
    }

    public Map<String, Source> getSchemaSourcesFromResource(final List<String> schemaNames) throws SAXException {
        final Map<String, Source> sources = new java.util.HashMap<>();
        for (final String schemaResource : schemaNames) {
            final InputStream is = getResourceAsStream(schemaResource);
            if (is == null) {
                throw new SAXException(String.format("resource null: %s", schemaResource));
            }
            sources.put(schemaResource, new javax.xml.transform.stream.StreamSource(is));
        }
        return sources;
    }

    public Map<String, Source> getSchemaSources() throws SAXException {
        return getSchemaSourcesFromResource(getSchemaNames());
    }

    public List<Class<?>> getKnownClasses() {
        final List<Class<?>> result = new java.util.ArrayList<>();
        result.add(org.adeptnet.prtg.xml.ObjectFactory.class);
        return result;
    }

    @SuppressWarnings("rawtypes")
    public JAXBContext getJAXBContext() throws JAXBException {
        final java.util.List<Class<?>> _knownClasses = getKnownClasses();
        return JAXBContext.newInstance(_knownClasses.toArray(new Class[_knownClasses.size()]));
    }

    private InputStream getSchemaSourceFromSystemId(final String systemId) {
        final Map<String, Source> list;
        try {
            list = getSchemaSources();
        } catch (SAXException ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
        if (list.containsKey(systemId)) {
            final Source source = list.get(systemId);
            if (source instanceof StreamSource) {
                return ((StreamSource) source).getInputStream();
            }
            return null;
        }
        return null;
    }

    private LSResourceResolver getResourceResolver() {
        return new LSResourceResolver() {

            @Override
            public LSInput resolveResource(String type, String namespaceURI, String publicId, String systemId, String baseURI) {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(String.format("%s LSResourceResolver - resolveResource type [%s] namespaceURI [%s] publicId [%s] systemId [%s] baseURI [%s]",
                            JaxbManager.class, type, namespaceURI, publicId, systemId, baseURI));
                }

                if (systemId == null) {
                    if (XMLConstants.W3C_XML_SCHEMA_NS_URI.equals(type) && XMLConstants.XML_NS_URI.equals(namespaceURI)) {
                        systemId = META_INF_PRTG + "xml.xsd";
                    } else {
                        try {
                            final URI uri = new URI(namespaceURI);
                            systemId = META_INF + uri.getPath();
                        } catch (URISyntaxException ex) {
                            LOG.log(Level.SEVERE, String.format("Invalid URI: %s", ex.getMessage()), ex);
                            return null;
                        }
                    }
                }

                final InputStream is = getSchemaSourceFromSystemId(systemId);
                if (is != null) {
                    final LSInput ls = new org.apache.xerces.dom.DOMInputImpl();
                    ls.setByteStream(is);
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine(String.format("returning: %s", systemId));
                    }
                    return ls;
                }
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("returning: NULL");
                }

                return null;
            }
        };
    }

    private Source[] getSchemaSourcesArray() throws SAXException {
        final java.util.Collection<Source> sources = getSchemaSources().values();
        return sources.toArray(new Source[sources.size()]);
    }

    private javax.xml.validation.Schema getSchema() throws SAXException {
        final SchemaFactory sf = SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
        sf.setErrorHandler(this);
        sf.setResourceResolver(getResourceResolver());
        return sf.newSchema(getSchemaSourcesArray());
    }

    public String getBasePath() {
        return basePath;
    }

    private EntityResolver2 getEntityResolver() {
        return new EntityResolver2() {

            @Override
            public InputSource getExternalSubset(String name, String baseURI) throws SAXException, IOException {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(String.format("%s getExternalSubset: name [%s] baseURI [%s]", JaxbManager.class, name, baseURI));
                }
                return null;
            }

            @Override
            public InputSource resolveEntity(String name, String publicId, String baseURI, String systemId) throws SAXException, IOException {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(String.format("%s EntityResolver2 - resolveEntity name [%s] publicId [%s] baseURI [%s] systemId [%s]",
                            JaxbManager.class, name, publicId, baseURI, systemId));
                }

                final File _file1 = new File(systemId);
                if (_file1.exists()) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine(String.format("returning1: %s", _file1.getAbsoluteFile().getName()));
                    }
                    return new InputSource(new FileReader(_file1));
                }

                final String path;

                try {
                    final URI uri = new URI(systemId);
                    path = META_INF + uri.getPath();
                } catch (URISyntaxException ex) {
                    LOG.log(Level.SEVERE, String.format("Invalid URI: %s", ex.getMessage()), ex);
                    return null;
                }

                final InputStream is = getSchemaSourceFromSystemId(path);

                if (is != null) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine(String.format("returning2: %s", path));
                    }
                    return new InputSource(is);
                }

                final File _file2 = new File(getBasePath(), systemId);
                if (_file2.exists()) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine(String.format("returning3: %s", _file2.getAbsoluteFile().getName()));
                    }
                    return new InputSource(new FileReader(_file2));
                }

                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine("returning: NULL");
                }
                return null;
            }

            @Override
            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                if (LOG.isLoggable(Level.FINE)) {
                    LOG.fine(String.format("%s EntityResolver2 - resolveEntity publicId [%s] systemId [%s]", JaxbManager.class, publicId, systemId));
                }
                return null;
            }
        };
    }

    protected DocumentBuilder getDocumentBuilder(final javax.xml.validation.Schema schema) throws SAXException {
        final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        builderFactory.setSchema(schema);
        builderFactory.setValidating(true);
        builderFactory.setCoalescing(true);
        builderFactory.setIgnoringComments(true);
        builderFactory.setIgnoringElementContentWhitespace(true);
        builderFactory.setNamespaceAware(true);
        // NOTE HERE we enable XInclude
        builderFactory.setXIncludeAware(true);

        try {
            // VERY VERY IMPORTANT, Disable DTD validation & enable Schema VALIDATION ......!!!!
            builderFactory.setFeature("http://apache.org/xml/features/validation/schema-full-checking", true);
            builderFactory.setFeature("http://apache.org/xml/features/honour-all-schemaLocations", true);
            builderFactory.setFeature("http://xml.org/sax/features/use-entity-resolver2", true);
            builderFactory.setFeature("http://apache.org/xml/features/validation/warn-on-duplicate-attdef", true);
            builderFactory.setFeature("http://apache.org/xml/features/warn-on-duplicate-entitydef", true);
            builderFactory.setFeature("http://xml.org/sax/features/namespaces", true);
            builderFactory.setFeature("http://apache.org/xml/features/validation/schema", true);

            final DocumentBuilder builder = builderFactory.newDocumentBuilder();
            builder.setEntityResolver(getEntityResolver());
            builder.setErrorHandler(this);
            return builder;
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new SAXException(String.format("getDocumentBuilder() - %s", e.getMessage()), e);
        }
    }

    public <T extends Object> T fromXML(final File file, final Class<T> clazz) throws JAXBException {
        try {
            basePath = file.getAbsoluteFile().getParent() + File.separator;
            final Unmarshaller _unm = getJAXBContext().createUnmarshaller();
            final javax.xml.validation.Schema schema = getSchema();
            _unm.setSchema(schema);
            final DocumentBuilder builder = getDocumentBuilder(schema);
            final Document doc = builder.parse(new FileInputStream(file));

            @SuppressWarnings("unchecked")
            final T result = (T) _unm.unmarshal(doc.getDocumentElement());

            if (!clazz.isInstance(result)) {
                throw new JAXBException(String.format("fromXML(): Not assignable [%s] - [%s]", clazz.getName(), result.getClass().getName()));
            }
            return result;
        } catch (SAXException | IOException ex) {
            throw new JAXBException(ex);
        }
    }

    public Prtg toPrtg(final File file) throws JAXBException {
        return fromXML(file, Prtg.class);
    }

    public String toXML(final Object object) throws JAXBException {
        final Marshaller marshal = getJAXBContext().createMarshaller();
        marshal.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshal.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
        final StringWriter sw = new StringWriter();
        marshal.marshal(object, sw);
        return sw.toString();
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        LOG.log(Level.WARNING, "warning", exception);
        throw exception;
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        LOG.log(Level.SEVERE, "error", exception);
        throw exception;
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        LOG.log(Level.SEVERE, "fatalError", exception);
        throw exception;
    }
}
