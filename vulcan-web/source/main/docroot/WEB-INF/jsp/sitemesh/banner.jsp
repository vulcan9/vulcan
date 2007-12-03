<?xml version="1.0" encoding="UTF-8" ?>

<div class="header"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:html="http://struts.apache.org/tags-html">
	
<jsp:directive.page session="false"/>
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

<c:if test="${pageContext.request.userPrincipal != null}">
	<span class="authInfo">
		<spring:message code="label.username"/>
		${pageContext.request.userPrincipal.name}
	</span>
</c:if>

<span class="banner"><spring:message code="application.name"/></span>

<ul class="menu">
	<li><html:link page="/" styleId="homeLink"><spring:message code="link.home"/></html:link></li>

	<c:if test="${stateManager.running}">
		<li><html:link forward="report"><spring:message code="link.report.build.history"/></html:link></li>
		<li><html:link forward="manualBuildForm"><spring:message code="link.build.manual"/></html:link></li>
		<li><html:link forward="preferences"><spring:message code="link.preferences"/></html:link></li>
	</c:if>
	
	<li><html:link forward="setup"><spring:message code="link.setup"/></html:link></li>
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
