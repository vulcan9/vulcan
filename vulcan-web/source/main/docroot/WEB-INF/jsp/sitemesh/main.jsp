<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:decorator="http://www.opensymphony.com/sitemesh/decorator">
	
<jsp:output
	omit-xml-declaration="false"
	doctype-root-element="html"
	doctype-public="-//W3C//DTD XHTML 1.1//EN"
	doctype-system="http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"/>

<head>
	<title><spring:message code="title.main"/></title>
	
	<c:choose>
		<c:when test="${preferences.stylesheet != null}">
			<c:url var="cssUrl" value="/css/${preferences.stylesheet}.css"/>
		</c:when>
		<c:otherwise>
			<c:url var="cssUrl" value="/css/standard.css"/>
		</c:otherwise>
	</c:choose>
	
	<jsp:element name="link">
		<jsp:attribute name="rel">stylesheet</jsp:attribute>
		<jsp:attribute name="type">text/css</jsp:attribute>
		<jsp:attribute name="href">${cssUrl}</jsp:attribute>
	</jsp:element>
	<jsp:element name="link">
		<jsp:attribute name="rel">stylesheet</jsp:attribute>
		<jsp:attribute name="type">text/css</jsp:attribute>
		<jsp:attribute name="media">print</jsp:attribute>
		<jsp:attribute name="href"><c:url value="/css/print.css"/></jsp:attribute>
	</jsp:element>
	<jsp:element name="link">
		<jsp:attribute name="rel">shortcut icon</jsp:attribute>
		<jsp:attribute name="type">image/vnd.microsoft.ico</jsp:attribute>
		<jsp:attribute name="href"><c:url value="/images/favicon.ico"/></jsp:attribute>
	</jsp:element>
	<script type="text/javascript">
		window.contextRoot = '<c:url value="/"/>';
	</script>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/jquery.js"/></jsp:attribute>
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
		<jsp:attribute name="content"><spring:message code="confirmation"/></jsp:attribute>
	</jsp:element>
	<jsp:element name="meta">
		<jsp:attribute name="name">confirmUnsavedChangesMessage</jsp:attribute>
		<jsp:attribute name="content"><spring:message code="confirmation.unsaved.changes"/></jsp:attribute>
	</jsp:element>
	<jsp:element name="meta">
		<jsp:attribute name="name">popupMode</jsp:attribute>
		<jsp:attribute name="content">${preferences.popupMode}</jsp:attribute>
	</jsp:element>
	<decorator:head/>
</head>

<body>
	<div id="main" class="yui-navset">
		<jsp:include page="/WEB-INF/jsp/sitemesh/topnav.jsp" flush="true"/>
		
		<div id="content">
			<c:if test="${showSetupMenu}">
				<!--jsp:include page="/WEB-INF/jsp/sitemesh/setupMenu.jsp" flush="true"/-->
			</c:if>
			
			<decorator:body/>
		</div>
		
		<div id="bottom">
			<p>Version ${stateManager.version} Copyright (C) 2005-2008</p>
			<p>GNU General Public License v2</p>
		</div>
	</div>
</body>
</html>
