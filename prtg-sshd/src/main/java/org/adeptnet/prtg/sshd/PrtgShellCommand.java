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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.sshd.server.Environment;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class PrtgShellCommand extends BaseCommand implements Runnable,Thread.UncaughtExceptionHandler {

    private static final Logger LOG = Logger.getLogger(PrtgShellCommand.class.getName());
    private final String ECHO = "echo";
    private final String EXIT = "exit";
    private final String MULTILINE = ";";

    private Thread thread;

    public PrtgShellCommand(final SshdConfigInterface configInterface) {
        super(configInterface);
    }

    @Override
    public void start(final Environment env) throws IOException {
        setEnvironment(env);
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setUncaughtExceptionHandler(this);
        thread.start();
    }

    @Override
    public void destroy() {
        thread.interrupt();
    }

    @Override
    public void run() {
        int exitValue = 0;
        String exitMessage = null;
        try {
            final InputStreamReader isr = new InputStreamReader(getInputStream());
            final BufferedReader br = new BufferedReader(isr);

            String line;
            while ((line = br.readLine()) != null) {
                handleLine(line);
            }
        } catch (InterruptedIOException ex) {
            //log.error(ex.getMessage(), ex);
        } catch (IOException ex) {
            exitValue = 1;
            exitMessage = ex.getMessage();
            LOG.log(Level.SEVERE,exitMessage, ex);
        } finally {
            doExit(exitValue, exitMessage);
        }
    }

    private void handleLine(final String line) throws InterruptedIOException, IOException {
        if (line.contains(MULTILINE)) {
            final String[] lines = line.split(MULTILINE);
            for (String _line : lines) {
                handleLine(_line);
            }
            return;
        }

        if (line.equalsIgnoreCase(EXIT)) {
            throw new InterruptedIOException();
        }

        if (line.startsWith(ECHO)) {
            if (line.length() >= 5) {
                printLine(line.substring(5));
            } else {
                printLine("");
            }
            return;
        }

        if (handlePRTG(line))  {
            return;
        }

        printLine(String.format("Unknown command: %s", line));
    }

    @Override
    public void uncaughtException(Thread thread, Throwable thrwbl) {
        LOG.log(Level.SEVERE,"Unhandled Exception in ShellCommand: "+thrwbl.getMessage(),thrwbl);
    }
}
