<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:decorator="http://www.opensymphony.com/sitemesh/decorator">
	
<jsp:directive.page session="false"/>

<jsp:output
	omit-xml-declaration="false"
	doctype-root-element="html"
	doctype-public="-//W3C//DTD XHTML 1.1//EN"
	doctype-system="http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"/>

<head>
	<title><fmt:message key="title.main"/></title>
	
	<jsp:element name="link">
		<jsp:attribute name="rel">stylesheet</jsp:attribute>
		<jsp:attribute name="type">text/css</jsp:attribute>
		<jsp:attribute name="href"><c:url value="/css/standard.css"/></jsp:attribute>
	</jsp:element>
	<jsp:element name="link">
		<jsp:attribute name="rel">shortcut icon</jsp:attribute>
		<jsp:attribute name="type">image/vnd.microsoft.ico</jsp:attribute>
		<jsp:attribute name="href"><c:url value="/images/favicon.ico"/></jsp:attribute>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/cssQuery.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/widgets.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/ieHacks.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="meta">
		<jsp:attribute name="name">confirmMessage</jsp:attribute>
		<jsp:attribute name="content"><fmt:message key="confirmation"/></jsp:attribute>
	</jsp:element>
	<jsp:element name="meta">
		<jsp:attribute name="name">confirmUnsavedChangesMessage</jsp:attribute>
		<jsp:attribute name="content"><fmt:message key="confirmation.unsaved.changes"/></jsp:attribute>
	</jsp:element>
	<decorator:head/>
</head>

<body>
	<jsp:include page="/WEB-INF/jsp/sitemesh/banner.jsp" flush="true"/>
	
	<div class="content">
		<c:if test="${showSetupMenu}">
			<jsp:include page="/WEB-INF/jsp/sitemesh/setupMenu.jsp" flush="true"/>
		</c:if>
		
		<decorator:body/>
	</div>
</body>
</html>
