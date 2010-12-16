<?xml version="1.0" encoding="utf-8"?>
<jsp:root version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:c="http://java.sun.com/jsp/jstl/core"
    xmlns:fn="http://java.sun.com/jsp/jstl/functions"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:html="http://struts.apache.org/tags-html">
    
    
    <jsp:directive.tag display-name="topNavSection" dynamic-attributes="false"/>

	<jsp:directive.attribute name="page" required="false" type="java.lang.String" rtexprvalue="false"/>
	<jsp:directive.attribute name="forward" required="false" type="java.lang.String" rtexprvalue="false"/>
	<jsp:directive.attribute name="name" required="true" type="java.lang.String" rtexprvalue="false"/>
	<jsp:directive.attribute name="messageKey" required="true" type="java.lang.String" rtexprvalue="true"/>
	<jsp:directive.attribute name="prefix" required="true" type="java.lang.String" rtexprvalue="true"/>

	<!-- determine which section is active if not already defined. -->
	<c:if test="${activeNav eq null}">
		<c:choose>
			<c:when test="${fn:startsWith(pageContext.request.servletPath, '/buildHistory')}">
				<c:set scope="request" var="activeNav" value="reports"/>
			</c:when>
			<c:when test="${fn:startsWith(pageContext.request.servletPath, '/viewProjectBuildHistory')}">
				<c:set scope="request" var="activeNav" value="reports"/>
			</c:when>
			<c:when test="${fn:startsWith(pageContext.request.servletPath, '/buildmanagement')}">
				<c:set scope="request" var="activeNav" value="manual-build"/>
			</c:when>
			<c:when test="${fn:startsWith(pageContext.request.servletPath, '/admin/setup')}">
				<c:set scope="request" var="activeNav" value="setup"/>
			</c:when>
			<c:when test="${fn:startsWith(pageContext.request.servletPath, '/admin/messages')}">
				<c:set scope="request" var="activeNav" value="messages"/>
			</c:when>
			<c:otherwise>
				<c:set scope="request" var="activeNav" value="dashboard"/>
			</c:otherwise>
		</c:choose>
	</c:if>
	
	<c:set var="styleClass" value=""/>
	
	<c:if test="${activeNav eq name}">
		<c:set var="styleClass" value="active"/>
	</c:if>

	<jsp:element name="li">
		<jsp:attribute name="class">${styleClass}</jsp:attribute>
		<jsp:body>
			<c:choose>
				<c:when test="${page ne null}">
					<html:link page="${page}" styleId="nav-${name}">
						<spring:message code="${messageKey}"/>
						<jsp:doBody/>
					</html:link>
				</c:when>
				<c:when test="${forward ne null}">
					<html:link forward="${forward}" styleId="nav-${name}">
						<spring:message code="${messageKey}"/>
						<jsp:doBody/>
					</html:link>
				</c:when>
			</c:choose>
			
		</jsp:body>
	</jsp:element>
</jsp:root>
