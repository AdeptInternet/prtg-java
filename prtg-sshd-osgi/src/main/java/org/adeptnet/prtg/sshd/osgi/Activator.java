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
package org.adeptnet.prtg.sshd.osgi;

import org.adeptnet.prtg.config.SensorException;
import org.adeptnet.prtg.sshd.PrtgSshServerFactory;
import org.adeptnet.prtg.sshd.impl.SshdConfigImplementation;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.apache.sshd.SshServer;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class Activator implements BundleActivator {

    private static final Logger LOG = Logger.getLogger(Activator.class.getName());
    private static final String PREFIX = "org.adeptnet";
    private static final String CONFIG_PATH = PREFIX + ".config.path";
    private static final String CONFIG_FILE = PREFIX + ".cfg";
    private static final String OSGI_PREFIX = PREFIX + ".prtg.sshd.osgi";
    private static final String CONFIG = OSGI_PREFIX + ".config";
    private static final String SSHD_AUTHORIZED_USERS = OSGI_PREFIX + ".authorizedUsers";
    private static final String SSHD_AUTHORIZED_KEYS = OSGI_PREFIX + ".authorizedKeys";
    private static final String SSHD_PORT = OSGI_PREFIX + ".sshPort";
    private static final String SSHD_HOST = OSGI_PREFIX + ".sshHost";
    private static final String SSHD_IDLE_TIMEOUT = OSGI_PREFIX + ".sshIdleTimeout";
    private static final String SSHD_AUTH_TIMEOUT = OSGI_PREFIX + ".sshAuthTimeout";
    private static final String SSHD_HOST_KEY = OSGI_PREFIX + ".hostKey";
    private static final String SSHD_KEY_SIZE = OSGI_PREFIX + ".keySize";
    private static final String SSHD_ALGORITHM = OSGI_PREFIX + ".algorithm";

    private static final String PRTG_PATH = OSGI_PREFIX + ".path";
    private static final String JMX_INITIAL_CONTEXT = OSGI_PREFIX + ".jmx.initial_context";

    private SshServer sshd;

    private String path;
    private Properties properties;

    private String resolveEnvVars(String input) {
        if (null == input) {
            return null;
        }
        Pattern p = Pattern.compile("\\$\\{((\\w+)([.](\\w+))*)\\}|\\$((\\w+)([.](\\w+))*)");
        Matcher m = p.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String envVarName = null == m.group(1) ? m.group(2) : m.group(1);
            String envVarValue = System.getProperty(envVarName);
            m.appendReplacement(sb, null == envVarValue ? "" : envVarValue.replace("\\", "\\\\"));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Properties getProperties() throws SensorException {
        if (properties == null) {
            try {
                final File file = new File(path + "/" + CONFIG_FILE);
                final InputStream is = new FileInputStream(file);
                properties = new Properties();
                properties.load(is);
                for (final String prop : properties.stringPropertyNames()) {
                    String value = properties.getProperty(prop);
                    if (!value.contains("${")) {
                        continue;
                    }
                    value = resolveEnvVars(value);
                    properties.setProperty(prop, value);
                }
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

    private int getPropertyInteger(final String name, final int _default) {
        try {
            return Integer.parseInt(getProperty(name));
        } catch (NumberFormatException | SensorException ex) {
            return _default;
        }
    }

    private long getPropertyLong(final String name, final long _default) {
        try {
            return Long.parseLong(getProperty(name));
        } catch (NumberFormatException | SensorException ex) {
            return _default;
        }
    }

    private String getPropertyString(final String name, final String _default) {
        try {
            return getProperty(name);
        } catch (SensorException ex) {
            return _default;
        }
    }

    private String getPath() throws SensorException {
        String result = System.getProperty(CONFIG_PATH);
        if (result != null) {
            return result;
        }

        try {
            final Context ctx = new InitialContext();
            final Object o = ctx.lookup(CONFIG_PATH);
            if (o instanceof String) {
                return (String) o;
            }
            throw new SensorException(String.format("getPath(): cannot find %s (%s)", CONFIG_PATH, o.getClass().getName()));
        } catch (NamingException ex) {
            throw new SensorException(String.format("getPath(): %s", ex.getMessage()), ex);
        }
    }

    public void startSshd(final JaxbManager jaxb) throws IOException, SensorException {
        path = getPath();

        final SshdConfigImplementation config = new SshdConfigImplementation(jaxb)
                .withXml(getProperty(CONFIG))
                .withUsersFile(getProperty(SSHD_AUTHORIZED_USERS))
                .withPublicKeysFile(getProperty(SSHD_AUTHORIZED_KEYS))
                .withPrtgPathPrefix(getPropertyString(PRTG_PATH, "/var/prtg/scriptsxml/"))
                .withJmxInitialContext(getPropertyString(JMX_INITIAL_CONTEXT, null));

        sshd = PrtgSshServerFactory.setUpDefaultServer(config);

        final int sshdPort = getPropertyInteger(SSHD_PORT, 2222);
        final String sshdHost = getPropertyString(SSHD_HOST, "0.0.0.0");
        final long sshIdleTimeout = getPropertyLong(SSHD_IDLE_TIMEOUT, 1800000);
        final long sshAuthTimeout = getPropertyLong(SSHD_AUTH_TIMEOUT, 60000);

        final String hostKey = getProperty(SSHD_HOST_KEY);
        final int keySize = getPropertyInteger(SSHD_KEY_SIZE, 1024);
        final String algorithm = getPropertyString(SSHD_ALGORITHM, "DSA");

        SimpleGeneratorHostKeyProvider keyPairProvider = new SimpleGeneratorHostKeyProvider();
        keyPairProvider.setPath(hostKey);
        keyPairProvider.setKeySize(keySize);
        keyPairProvider.setAlgorithm(algorithm);

        Authenticator authenticator = new Authenticator(config);

        sshd.setPort(sshdPort);
        sshd.setHost(sshdHost);
        sshd.getProperties().put(SshServer.AUTH_TIMEOUT, Long.toString(sshAuthTimeout));
        sshd.getProperties().put(SshServer.IDLE_TIMEOUT, Long.toString(sshIdleTimeout));
        sshd.setKeyPairProvider(keyPairProvider);
        sshd.setPasswordAuthenticator(authenticator);
        sshd.setPublickeyAuthenticator(authenticator);

        sshd.start();
    }

    @Override
    public void start(BundleContext context) throws Exception {
        LOG.log(Level.INFO, "ADEPTNET Starting: {0}", Activator.class);

        final JaxbManager jaxb = new JaxbManager(context.getBundle());
        startSshd(jaxb);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOG.log(Level.INFO, "ADEPTNET Stopping: {0}", Activator.class);
        if (sshd != null) {
            sshd.stop();
        }
    }
}
