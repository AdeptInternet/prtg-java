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

import org.adeptnet.prtg.sshd.SshdConfigInterface;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPublicKey;
import javax.security.auth.login.FailedLoginException;
import org.apache.sshd.server.PasswordAuthenticator;
import org.apache.sshd.server.PublickeyAuthenticator;
import org.apache.sshd.server.session.ServerSession;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class Authenticator implements PasswordAuthenticator, PublickeyAuthenticator {

    private final SshdConfigInterface configInterface;

    public Authenticator(SshdConfigInterface configInterface) {
        this.configInterface = configInterface;
    }

    @Override
    public boolean authenticate(String username, String password, ServerSession session) {
        if (username == null) {
            return false;
        }
        final String _password = configInterface.getUsers().getProperty(username);
        if (_password == null) {
            return false;
        }
        return _password.equals(password);
    }

    @Override
    public boolean authenticate(String username, PublicKey key, ServerSession session) {
        final String keyValue;
        try {
            keyValue = getKeyValue(key);
        } catch (FailedLoginException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
        for (final String keyLine : configInterface.getPublicKeys()) {
            final String[] parts = keyLine.split("\\s+");
            if (parts.length < 2) {
                continue;
            }
            if (parts[1].equals(keyValue)) {
                return true;
            }
        }
        return false;
    }

    private String base64Encode(final byte[] data) {
        return javax.xml.bind.DatatypeConverter.printBase64Binary(data);
    }

    private String getKeyValue(PublicKey key) throws FailedLoginException {
        try {
            if (key instanceof DSAPublicKey) {
                DSAPublicKey dsa = (DSAPublicKey) key;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (DataOutputStream dos = new DataOutputStream(baos)) {
                    write(dos, "ssh-dss");
                    write(dos, dsa.getParams().getP());
                    write(dos, dsa.getParams().getQ());
                    write(dos, dsa.getParams().getG());
                    write(dos, dsa.getY());
                }
                return base64Encode(baos.toByteArray());
            } else if (key instanceof RSAKey) {
                RSAPublicKey rsa = (RSAPublicKey) key;
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (DataOutputStream dos = new DataOutputStream(baos)) {
                    write(dos, "ssh-rsa");
                    write(dos, rsa.getPublicExponent());
                    write(dos, rsa.getModulus());
                }
                return base64Encode(baos.toByteArray());
            } else {
                throw new FailedLoginException("Unsupported key type " + key.getClass().toString());
            }
        } catch (IOException ex) {
            throw new FailedLoginException("Unable to check public key: " + ex.getMessage());
        }
    }

    private void write(DataOutputStream dos, BigInteger integer) throws IOException {
        byte[] data = integer.toByteArray();
        dos.writeInt(data.length);
        dos.write(data, 0, data.length);
    }

    private void write(DataOutputStream dos, String str) throws IOException {
        byte[] data = str.getBytes();
        dos.writeInt(data.length);
        dos.write(data);
    }
}
