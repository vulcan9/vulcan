<?xml version="1.0" encoding="UTF-8" ?>

<jsp:root version="2.0"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core">
	
	<jsp:directive.page session="false"/>

	<jsp:output omit-xml-declaration="true"/>

	<c:set var="showSetupMenu" value="true" scope="request"/>

	<jsp:include page="/WEB-INF/jsp/sitemesh/main.jsp" flush="true"/>

</jsp:root>