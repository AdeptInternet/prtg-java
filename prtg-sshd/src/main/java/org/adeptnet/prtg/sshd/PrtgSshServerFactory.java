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

import org.apache.sshd.SshServer;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class PrtgSshServerFactory {

    public static SshServer setUpDefaultServer(final SshdConfigInterface configInterface) {
        SshServer sshd = SshServer.setUpDefaultServer();

        sshd.setCommandFactory(new PrtgCommandFactory(configInterface));
        sshd.setShellFactory(new PrtgShellFactory(configInterface));

        return sshd;
    }

}
