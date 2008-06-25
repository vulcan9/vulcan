<?xml version="1.0" encoding="UTF-8" ?>

<jsp:root version="2.0"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">
	
<jsp:output omit-xml-declaration="true"/>

<html:xhtml/>

<c:choose>
	<c:when test="${not empty eventPool['ERROR']}">
		<spring:message var="linkTitle" code="health.error"/>
		<html:link styleClass="health" page="/admin/messages.jsp"><html:img page="/images/error.gif" alt="errors" title="${linkTitle}"/></html:link>
	</c:when>
	<c:otherwise>
		<c:if test="${not empty eventPool['WARNING']}">
			<spring:message var="linkTitle" code="health.warning"/>
			<html:link styleClass="health" page="/admin/messages.jsp"><html:img page="/images/warning.gif" alt="warnings" title="${linkTitle}"/></html:link>
		</c:if>
	</c:otherwise>
</c:choose>

<a href="#skipnav" class="hidden">[Skip Navigation]</a>

<div id="logo">
	<h1>Vulcan</h1>
	<h2>Continuous Integration Services</h2>
</div>

<div id="loading-message">Loading...</div>

<ul id="topnav" class="inline">
	<v:topNavSection name="dashboard" page="/" messageKey="link.home" prefix="/index.jsp" />
	
	<c:if test="${stateManager.running}">
		<v:topNavSection name="reports" forward="report" messageKey="link.report.build.history" prefix="/buildHistory"/>
		<v:topNavSection name="custom-build" forward="manualBuildForm" messageKey="link.build.manual" prefix="/buildmanagement"/>
	</c:if>
	
	<v:topNavSection name="setup" forward="setup" messageKey="link.setup" prefix="/admin/setup"/>
</ul>

<ul id="secondarynav" class="inline">
	<c:if test="${pageContext.request.userPrincipal != null}">
		<li>
			<span class="username">${pageContext.request.userPrincipal.name}</span>
		</li>
	</c:if>
	<c:if test="${stateManager.running}">
		<li><html:link forward="preferences"><spring:message code="link.preferences"/></html:link></li>
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

<a name="skipnav" class="hidden" href="#">Content</a>
 
</jsp:root>
