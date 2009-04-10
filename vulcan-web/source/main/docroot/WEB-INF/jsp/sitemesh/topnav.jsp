<?xml version="1.0" encoding="UTF-8" ?>

<jsp:body class="header"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<jsp:directive.page session="false"/>
<jsp:output omit-xml-declaration="true"/>

<html:xhtml/>

<a href="#content" class="hidden">[Skip Navigation]</a>

<div id="top-nav" class="container">
	<div id="brand">
		<html:link page="/"><html:img page="/images/brand.png" alt="Home"/></html:link> 
		<h1><spring:message code="brand.name"/></h1>
		<h2><spring:message code="brand.info" htmlEscape="true"/></h2>
	</div>
	
	<ul id="primary-nav">
		<v:topNavSection name="dashboard" page="/" messageKey="link.home" prefix="/index.jsp" />
		<c:if test="${stateManager.running}">
			<v:topNavSection name="reports" forward="report" messageKey="link.report.build.history" prefix="/buildHistory"/>
			<v:topNavSection name="manual-build" forward="manualBuildForm" messageKey="link.build.manual" prefix="/buildmanagement"/>
		</c:if>
		<v:topNavSection name="messages" page="/admin/messages.jsp" messageKey="link.messages" prefix="/admin/messages">
			<c:choose>
				<c:when test="${not empty v:getEvents('ERROR')}">
					<spring:message var="linkTitle" code="health.error"/>
					<html:img page="/images/error.gif" alt="errors" title="${linkTitle}" styleClass="health"/>
				</c:when>
				<c:otherwise>
					<c:if test="${not empty v:getEvents('WARNING')}">
						<spring:message var="linkTitle" code="health.warning"/>
						<html:img page="/images/warning.gif" alt="errors" title="${linkTitle}" styleClass="health"/>
					</c:if>
				</c:otherwise>
			</c:choose>
		</v:topNavSection>	
	</ul>
	
	<ul id="utility-nav">
		<c:if test="${pageContext.request.userPrincipal != null}">
			<li class="username">
				<span id="username">${pageContext.request.userPrincipal.name}</span>:
			</li>
		</c:if>
		
		<c:if test="${stateManager.running}">
			<li><html:link forward="preferences"><spring:message code="link.preferences"/></html:link></li>
			<li><html:link forward="setup"><spring:message code="link.setup"/></html:link></li>
		</c:if>
		
	
		<li><html:link forward="about"><spring:message code="link.about"/></html:link></li>
		
		<c:choose>
			<c:when test="${helpUrl ne null}">
				<li><a class="help" href="${helpUrl}"><spring:message code="link.help"/></a></li>
			</c:when>
			<c:otherwise>
				<li><html:link forward="help" styleId="helpLink" ><spring:message code="link.help"/></html:link></li>
			</c:otherwise>
		</c:choose>
	</ul>
</div>
</jsp:body>
