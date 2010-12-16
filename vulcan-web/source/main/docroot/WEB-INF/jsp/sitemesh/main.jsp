<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fn="http://java.sun.com/jsp/jstl/functions"
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
		<jsp:attribute name="media">screen</jsp:attribute>
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
	<c:url value="/" var="contextRoot"/>
	<c:if test="${fn:substringBefore(contextRoot, ';') != ''}">
		<c:set var="contextRoot" value="${fn:substringBefore(contextRoot, ';')}"/>
	</c:if>
	<script type="text/javascript">window.contextRoot = '${contextRoot}';</script>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/jquery-1.2.6.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/jquery-ui-1.5.2.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/jquery.tablesorter.min.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/widgets.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	
	<!-- IE specific javascript.  Use c:out to render comments since these comments
		are removed by jspx or sitemesh (not really sure which) -->
	<c:out value="&lt;!--[if lte IE 8]&gt;" escapeXml="false"/>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/ieHacks.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<c:out value="&lt;![endif]--&gt;" escapeXml="false"/>
	
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
	<jsp:include page="/WEB-INF/jsp/sitemesh/topnav.jsp" flush="true"/>

	<div id="content" class="container">
		<c:choose>
			<c:when test="${showSetupMenu}">
				<jsp:include page="/WEB-INF/jsp/sitemesh/setupMenu.jsp" flush="true"/>
				<div id="setup-content">
					<decorator:body/>
				</div>
			</c:when>
			<c:otherwise>
				<decorator:body/>
			</c:otherwise>
		</c:choose>
	</div>
	
	<div id="site-info" class="container">
		<p><a href="http://code.google.com/p/vulcan/" class="help">Vulcan</a> ${stateManager.version} Copyright (c) 2005-2010</p>
		<p><a href="http://www.gnu.org/licenses/old-licenses/gpl-2.0.html" class="help">GNU General Public License v2</a></p>
	</div>
</body>
</html>
