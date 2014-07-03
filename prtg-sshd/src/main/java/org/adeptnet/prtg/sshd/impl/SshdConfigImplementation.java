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
package org.adeptnet.prtg.sshd.impl;

import org.adeptnet.prtg.config.impl.ConfigImplementation;
import org.adeptnet.prtg.sshd.SshdConfigInterface;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Francois Steyn - Adept Internet (PTY) LTD (francois.s@adept.co.za)
 */
public class SshdConfigImplementation extends ConfigImplementation implements SshdConfigInterface {

    //private static final Log LOG = LogFactory.getLog(SshdConfigImplementation.class);
    private static final Logger LOG = Logger.getLogger(SshdConfigImplementation.class.getName());
    
    private String usersFile;
    private Properties users;
    private long usersModified;
    private String publicKeysFile;
    private List<String> publicKeys;
    private long publicKeysModified;
    private String prtgPathPrefix;

    public String getUsersFile() {
        return usersFile;
    }

    public void setUsersFile(final String usersFile) {
        this.usersFile = usersFile;
        this.users = null;
    }

    public SshdConfigImplementation withUsersFile(final String usersFile) {
        setUsersFile(usersFile);
        return this;
    }

    @Override
    public Properties getUsers() {
        if (users == null) {
            users = new Properties();
            try {
                final File f = new File(usersFile);
                usersModified = f.lastModified();
                users.load(new FileInputStream(f));
                LOG.log(Level.INFO, String.format("getUsers(): Loaded (%d) Users from (%s)",users.size(),usersFile));
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, String.format("getUsers(): %s", ex.getMessage()), ex);
            }
        } else if (usersFile != null) {
            final File f = new File(usersFile);
            if (f.lastModified() != usersModified) {
                users = null;
                return getUsers();
            }
        }
        return users;
    }

    public void setUsers(final Properties users) {
        this.users = users;
        this.usersFile = null;
    }

    public SshdConfigImplementation withUsers(final Properties users) {
        setUsers(users);
        return this;
    }

    public String getPublicKeysFile() {
        return publicKeysFile;
    }

    public void setPublicKeysFile(final String publicKeysFile) {
        this.publicKeysFile = publicKeysFile;
        this.publicKeys = null;
    }

    public void setPublicKeys(final List<String> publicKeys) {
        this.publicKeys = publicKeys;
        this.publicKeysFile = null;
    }

    public SshdConfigImplementation withPublicKeys(final List<String> publicKeys) {
        setPublicKeys(publicKeys);
        return this;
    }

    public SshdConfigImplementation withPublicKeysFile(final String publicKeysFile) {
        setPublicKeysFile(publicKeysFile);
        return this;
    }

    @Override
    public SshdConfigImplementation withXml(final String xml) {
        return (SshdConfigImplementation) super.withXml(xml);
    }
    
    @Override
    public SshdConfigImplementation withJmxInitialContext(final String jmxInitialContext) {
        return (SshdConfigImplementation) super.withJmxInitialContext(jmxInitialContext);
    }
    

    @Override
    public List<String> getPublicKeys() {
        if (publicKeys == null) {
            try {
                final Path path = java.nio.file.Paths.get(publicKeysFile);
                publicKeysModified = path.toFile().lastModified();
                publicKeys = java.nio.file.Files.readAllLines(path, Charset.defaultCharset());
                LOG.log(Level.INFO, String.format("getPublicKeys(): Loaded (%d) Keys from (%s)",publicKeys.size(),publicKeysFile));
            } catch (IOException ex) {
                LOG.log(Level.SEVERE, String.format("getPublicKeys(): %s", ex.getMessage()), ex);
                publicKeys = java.util.Collections.emptyList();
            }
        } else if (publicKeysFile != null) {
            final File f = new File(publicKeysFile);
            if (f.lastModified() != publicKeysModified) {
                publicKeys = null;
                return getPublicKeys();
            }
        }
        return publicKeys;
    }

    @Override
    public String getPrtgPathPrefix() {
        return prtgPathPrefix;
    }

    public void setPrtgPathPrefix(String prtgPathPrefix) {
        this.prtgPathPrefix = prtgPathPrefix;
    }

    public SshdConfigImplementation withPrtgPathPrefix(String prtgPathPrefix) {
        setPrtgPathPrefix(prtgPathPrefix);
        return this;
    }

}
