prtg-servlet-osgi
=================

Simple Servlet OSGi implementation to run in ServiceMix (http://servicemix.apache.org/)


Configuration example:
----------------------

**system.properties**
org.adeptnet.config.path=${karaf.home}/etc

**org.adeptnet.cfg**
org.adeptnet.prtg.servlet.osgi.alias=/adept
org.adeptnet.prtg.servlet.osgi.config=etc/org.adeptnet.prtg.xml

**org.adeptnet.prtg.xml**
Example in root