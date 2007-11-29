<jsp:root version="2.0"
		xmlns:jsp="http://java.sun.com/JSP/Page"
		xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

	<jsp:directive.page contentType="application/xml" session="false"/>
	<jsp:output omit-xml-declaration="false"/>

	<v:projectStatusXml labels="${paramValues['label']}"/>
</jsp:root>