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

import org.adeptnet.prtg.config.ConfigInterface;
import org.adeptnet.prtg.config.SensorProcess;
import org.adeptnet.prtg.config.xml.SensorType;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class Servlet extends javax.servlet.http.HttpServlet {

    private static final long serialVersionUID = 20140101001L;
    private static final Logger LOG = Logger.getLogger(Activator.class.getName());
    private final ConfigInterface configInterface;

    public Servlet(ConfigInterface configInterface) {
        this.configInterface = configInterface;
    }

    private String getHelp() throws JAXBException {
        final StringBuilder sb = new StringBuilder();
        for (final SensorType sensor : configInterface.getConfig().getSensors()) {
            sb.append(String.format("URI: prtg?name=%s\n", sensor.getName()));
        }
        return sb.toString();
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        LOG.info(String.format("doGet: %s ServletPath [%s] PathInfo [%s] QueryString [%s]", request.getRemoteHost(), request.getServletPath(), request.getPathInfo(), request.getQueryString()));
        try {
            if (request.getPathInfo() == null) {
                response.getWriter().write(getHelp());
            } else {
                switch (request.getPathInfo()) {
                    case "/prtg":
                        response.getWriter().write(
                                new SensorProcess(configInterface)
                                .withSensorName(request.getParameter("name"))
                                .run());
                        break;
                    default:
                        response.getWriter().write(getHelp());
                        break;
                }
            }
        } catch (JAXBException ex) {
            LOG.log(Level.SEVERE, String.format("JaxbManager.toXML1: ServletPath [%s] PathInfo [%s] QueryString [%s]", request.getServletPath(), request.getPathInfo(), request.getQueryString()));
            LOG.log(Level.SEVERE, String.format("JaxbManager.toXML2: %s", ex.getMessage()), ex);
            response.setContentType("text/plain");
            response.sendError(500, ex.getMessage());
        }
    }

}
