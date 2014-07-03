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
package org.adeptnet.prtg.config;

import bsh.EvalError;
import org.adeptnet.prtg.xml.Prtg;
import org.adeptnet.prtg.config.xml.ChannelTypeAttribute;
import org.adeptnet.prtg.config.xml.ChannelTypeBase;
import org.adeptnet.prtg.config.xml.ChannelTypeOperation;
import org.adeptnet.prtg.config.xml.ChannelTypeScript;
import org.adeptnet.prtg.config.xml.Config;
import org.adeptnet.prtg.config.xml.JMXConfigType;
import org.adeptnet.prtg.config.xml.SensorType;
import org.adeptnet.prtg.xml.BooleanType;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import javax.naming.InitialContext;
import javax.xml.bind.JAXBException;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za) F
 */
public class SensorProcess {

    private static final Logger LOG = Logger.getLogger(SensorProcess.class.getName());
    private final Prtg prtg;
    private String sensorName;
    private MBeanServerConnection mbsc;
    private Config config;
    private final ConfigInterface configInterface;

    public SensorProcess(final ConfigInterface configInterface) {
        this.prtg = new Prtg();
        this.configInterface = configInterface;
    }

    public String getSensorName() {
        return sensorName;
    }

    public void setSensorName(final String sensorName) {
        this.sensorName = sensorName;
    }

    public SensorProcess withSensorName(final String sensorName) {
        setSensorName(sensorName);
        return this;
    }

    private String getScript(MBeanServerConnection mbsc, ObjectName bean) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException, MBeanException, AttributeNotFoundException {
        final StringBuilder sb = new StringBuilder();
        final MBeanInfo info = mbsc.getMBeanInfo(bean);

        for (final MBeanAttributeInfo attrInfo : info.getAttributes()) {
            final String type;
            if ((attrInfo.getType().startsWith("[")) || (attrInfo.getType().contains("$"))) {
                final Object value = mbsc.getAttribute(bean, attrInfo.getName());
                type = value.getClass().getCanonicalName();
            } else {
                type = attrInfo.getType();
            }
            sb.append(String.format("%s get%s() {\n", type, attrInfo.getName()));
            sb.append(String.format("\treturn jmx.getAttribute(bean, \"%s\");\n", attrInfo.getName()));
            sb.append("}\n\n");
        }

        for (final MBeanOperationInfo opInfo : info.getOperations()) {
            final StringBuilder parmsCall = new StringBuilder();
            final StringBuilder parmsValue = new StringBuilder();
            final StringBuilder parmsType = new StringBuilder();

            for (final MBeanParameterInfo parmInfo : opInfo.getSignature()) {
                if (parmsCall.length() > 0) {
                    parmsCall.append(",");
                }
                if (parmsValue.length() > 0) {
                    parmsValue.append(",");
                }
                if (parmsType.length() > 0) {
                    parmsType.append(",");
                }
                final String type;
                if ((parmInfo.getType().startsWith("[")) || (parmInfo.getType().contains("$"))) {
                    type = "Object";
                } else {
                    type = parmInfo.getType();
                }
                parmsCall.append(String.format("%s %s ", type, parmInfo.getName()));
                parmsValue.append(String.format("%s ", parmInfo.getName()));
                parmsType.append(String.format("\"%s\" ", type));
            }

            final String type;
            if ((opInfo.getReturnType().startsWith("[")) || (opInfo.getReturnType().contains("$"))) {
                type = "Object";
            } else {
                type = opInfo.getReturnType();
            }
            sb.append(String.format("%s %s(%s) {\n",
                    type,
                    opInfo.getName(),
                    parmsCall.toString().trim()));
            sb.append(String.format("\treturn jmx.invoke(bean, \"%s\", new Object[]{%s}, new String[]{%s});\n",
                    opInfo.getName(),
                    parmsValue.toString().trim(),
                    parmsType.toString().trim()));
            sb.append("}\n\n");
        }

        return sb.toString();
    }

    private Object interpretBeanShell(String text, Map<String, Object> params) throws SensorException {
        bsh.Interpreter bsh = new bsh.Interpreter();
        try {
            for (String name : params.keySet()) {
                Object val = params.get(name);
                if (val instanceof Number) {
                    if (val instanceof Integer) {
                        bsh.set(name, ((Integer) val).intValue());
                    } else if (val instanceof Long) {
                        bsh.set(name, ((Long) val).longValue());
                    } else if (val instanceof Double) {
                        bsh.set(name, ((Double) val).doubleValue());
                    } else if (val instanceof Float) {
                        bsh.set(name, ((Float) val).floatValue());
                    } else {
                        bsh.set(name, val);
                    }
                } else if (val instanceof Boolean) {
                    bsh.set(name, ((Boolean) val).booleanValue());
                } else {
                    bsh.set(name, params.get(name));
                }
            }
            return bsh.eval(text);
        } catch (EvalError e) {
            throw new SensorException("Error executing bean script: " + e.getMessage(), e);
        }

    }

    private void processChannel(final ChannelTypeBase channel) throws SensorException {
        try {
            final ObjectName bean = new ObjectName(channel.getJmxObjectName());
            final Object result;
            if (channel instanceof ChannelTypeAttribute) {
                final ChannelTypeAttribute _channel = channel.upCast(ChannelTypeAttribute.class);
                result = _channel.getLookupValue(mbsc.getAttribute(bean, _channel.getJmxMethod()));
            } else if (channel instanceof ChannelTypeOperation) {
                final ChannelTypeOperation _channel = channel.upCast(ChannelTypeOperation.class);
                final List<Serializable> values = _channel.getJmxParameters().getValues();
                final Object[] parms = new Object[values.size()];
                final String[] parmsType = new String[values.size()];
                for (int x = 0; x < values.size(); x++) {
                    parms[x] = values.get(x);
                    parmsType[x] = values.get(x).getClass().getName();
                }
                result = _channel.getLookupValue(mbsc.invoke(bean, _channel.getJmxMethod(), parms, parmsType));
            } else if (channel instanceof ChannelTypeScript) {
                final ChannelTypeScript _channel = channel.upCast(ChannelTypeScript.class);
                final String data = String.format("%s%s", getScript(mbsc, bean), _channel.getJmxScript());
                final Map<String, Object> params = new HashMap<>();
                params.put("jmx", mbsc);
                params.put("bean", bean);

                try {
                    result = interpretBeanShell(data, params);
                } catch (SensorException ex) {
                    throw new SensorException(String.format("ChannelTypeScript: %s", ex.getMessage()), ex);
                }
            } else {
                throw new SensorException(String.format("Unknown ChannelType: %s", channel.getClass().getName()));
            }
            if (!channel.isCreateChannel()) {
                return;
            }

            prtg.getResult().add(
                    channel
                    .downCast()
                    .withValue(result)
            );
        } catch (MalformedObjectNameException | MBeanException | AttributeNotFoundException | InstanceNotFoundException | ReflectionException | IOException | IntrospectionException ex) {
            throw new SensorException(String.format("processChannel: %s - (%s)%s", channel.getName(), ex.getClass().getName(), ex.getMessage()), ex);
        }
    }

    private void process() throws SensorException, MalformedURLException, IOException {
        final SensorType sensor = config.getSensorByName(sensorName);
        if (sensor == null) {
            throw new SensorException(String.format("Cannot find SensorName: %s", sensorName));
        }

        final JMXConfigType jmx = config.getJMXByName(sensor.getJmxName());
        if (jmx == null) {
            throw new SensorException(String.format("Cannot find JMX: %s", sensor.getJmxName()));
        }

        Map<String, Object> env = new HashMap<>();
        String[] credentials = new String[]{jmx.getUserName(), jmx.getPassword()};
        env.put(JMXConnector.CREDENTIALS, credentials);
        if (configInterface.getJmxInitialContext() != null) {
            /*
             Some osgi containers does not load the RMI url correctly.
             For Glassfish the following is required: com.sun.jndi.rmi.registry.RegistryContextFactory
             eg: http://forum.petalslink.com/Problem-with-webconsole-2-05-td2984246.html
             */
            env.put(InitialContext.INITIAL_CONTEXT_FACTORY, configInterface.getJmxInitialContext());
        }

        final JMXServiceURL url = new JMXServiceURL(jmx.getUrl());
        try (final JMXConnector jmxc = JMXConnectorFactory.connect(url, env)) {
            mbsc = jmxc.getMBeanServerConnection();

            for (final ChannelTypeBase channel : sensor.getChannels()) {
                processChannel(channel);
            }
        }
    }

    public String run() throws JAXBException {
        try {
            config = configInterface.getConfig();
            if (config == null) {
                throw new SensorException("Please setup configInterface");
            }
            if ((sensorName == null) || (sensorName.isEmpty())) {
                throw new SensorException("No parameter: name");
            }
            process();
        } catch (Throwable ex) {
            final Throwable _ex = (ex instanceof javax.xml.bind.JAXBException) ? ((javax.xml.bind.JAXBException) ex).getLinkedException() : ex;
            prtg.setError(BooleanType.TRUE);
            prtg.setErrorText(String.format("%s: %s", ex.getClass().getName(), _ex));
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
        }
        return org.adeptnet.prtg.xml.JaxbManager.toXML(prtg);
    }

}
