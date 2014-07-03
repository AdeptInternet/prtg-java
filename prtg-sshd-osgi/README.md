prtg-sshd-osgi
==============

Simple SSHD OSGi Implementation to run in ServiceMix(http://servicemix.apache.org/) or Glassfish

Configuration example:
----------------------

**system.properties**
org.adeptnet.config.path=${karaf.home}/etc

**org.adeptnet.cfg**
org.adeptnet.prtg.sshd.osgi.config=${karaf.base}/etc/org.adeptnet.prtg.xml
org.adeptnet.prtg.sshd.osgi.sshPort=2222
org.adeptnet.prtg.sshd.osgi.sshHost=0.0.0.0
org.adeptnet.prtg.sshd.osgi.sshIdleTimeout=1800000
org.adeptnet.prtg.sshd.osgi.sshAuthTimeout=60000
org.adeptnet.prtg.sshd.osgi.hostKey=${karaf.base}/etc/host.key
org.adeptnet.prtg.sshd.osgi.keySize=1024
org.adeptnet.prtg.sshd.osgi.algorithm=DSA
org.adeptnet.prtg.sshd.osgi.authorizedKeys=${karaf.home}/etc/authorized_keys
org.adeptnet.prtg.sshd.osgi.authorizedUsers=${karaf.home}/etc/org.adeptnet.users.cfg


**org.adeptnet.prtg.xml**
Example in root