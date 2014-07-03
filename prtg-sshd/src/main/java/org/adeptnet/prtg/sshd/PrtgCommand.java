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

import org.adeptnet.prtg.config.xml.SensorType;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;
import org.apache.sshd.server.Environment;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class PrtgCommand extends BaseCommand {

    private static final Logger LOG = Logger.getLogger(PrtgCommand.class.getName());
    private final String command;

    public PrtgCommand(final SshdConfigInterface configInterface) {
        this(configInterface,null);
    }

    public PrtgCommand(final SshdConfigInterface configInterface, final String command) {
        super(configInterface);
        this.command = command;
    }

    @Override
    public void start(Environment env) throws IOException {
        int exitValue = 0;
        String exitMessage = null;
        try {
            if (handlePRTG(command)) {
                return;
            }
            if (command.startsWith("ls ") || command.equals("ls")) {
                try {
                    for (SensorType sensor : getConfigInterface().getConfig().getSensors()) {
                        printLine(sensor.getName());
                    }
                } catch (JAXBException ex) {
                    exitMessage = String.format("Error Executing Command: %s", ex.getMessage());
                    LOG.log(Level.SEVERE,exitMessage, ex);
                    printLineError(exitMessage);
                    exitValue = 1;
                }
                return;
            }
            exitMessage = String.format("Unknown command: %s", command);
            printLineError(exitMessage);
        } finally {
            doExit(exitValue, exitMessage);
        }
    }

    @Override
    public void destroy() {
    }

}
