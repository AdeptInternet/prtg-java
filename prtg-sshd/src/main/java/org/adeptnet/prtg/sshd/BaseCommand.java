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
package org.adeptnet.prtg.sshd;

import org.adeptnet.prtg.config.ConfigInterface;
import org.adeptnet.prtg.config.SensorProcess;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public abstract class BaseCommand implements Command {

    private static final Logger LOG = Logger.getLogger(BaseCommand.class.getName());
    private final String CRLF = "\r\n";
    private final SshdConfigInterface configInterface;
    private InputStream in;
    private OutputStream out;
    private OutputStream err;
    private ExitCallback callback;
    private Environment environment;

    public BaseCommand(SshdConfigInterface configInterface) {
        this.configInterface = configInterface;
    }

    public ConfigInterface getConfigInterface() {
        return configInterface;
    }

    public void printLine(final String line) throws IOException {
        out.write(String.format("%s%s", line, CRLF).getBytes());
        out.flush();
    }

    public void printLineError(final String line) throws IOException {
        err.write(String.format("%s%s", line, CRLF).getBytes());
        err.flush();
    }

    public void doExit(final int exitValue) {
        doClose();
        if (callback != null) {
            callback.onExit(exitValue);
        }
    }

    public void doClose() {
        doClose(in, out, err);
    }

    public void doClose(Closeable... closeables) {
        for (Closeable c : closeables) {
            try {
                if (c != null) {
                    c.close();
                }
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    public void doExit(final int exitValue, final String exitMessage) {
        doClose();
        if (callback != null) {
            callback.onExit(exitValue, exitMessage);
        }
    }

    protected boolean handlePRTG(final String line) throws IOException {
        if (!line.startsWith(configInterface.getPrtgPathPrefix())) {
            return false;
        }
        try {
            printLine(new SensorProcess(configInterface)
                    .withSensorName(line.replace(configInterface.getPrtgPathPrefix(), "").trim())
                    .run());
        } catch (JAXBException ex) {
            final String error = String.format("(%s) %s",ex.getClass().getName(),ex.getMessage());
            final StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
            sb.append("<prtg xmlns=\"urn:ietf:params:xml:ns:prtg-1.0\">");
            sb.append("<error>1</error>");
            sb.append("<text>");
            sb.append(error);
            sb.append("</text>");
            sb.append("</prtg>");
            printLineError(sb.toString());
            LOG.log(Level.SEVERE, error, ex);
        }
        return true;
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public OutputStream getErrorStream() {
        return err;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setInputStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
    }

}
