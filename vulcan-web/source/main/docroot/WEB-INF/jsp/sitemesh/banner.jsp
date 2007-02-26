<?xml version="1.0" encoding="UTF-8" ?>

<div class="header"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://jakarta.apache.org/struts/tags-html">
	
<jsp:directive.page session="false"/>
<jsp:output omit-xml-declaration="true"/>

<html:xhtml/>

<c:choose>
	<c:when test="${not empty eventPool['ERROR']}">
		<fmt:message var="linkTitle" key="health.error"/>
		<html:link styleClass="health" page="/admin/messages.jsp"><html:img page="/images/error.gif" alt="errors" title="${linkTitle}"/></html:link>
	</c:when>
	<c:otherwise>
		<c:if test="${not empty eventPool['WARNING']}">
			<fmt:message var="linkTitle" key="health.warning"/>
			<html:link styleClass="health" page="/admin/messages.jsp"><html:img page="/images/warning.gif" alt="warnings" title="${linkTitle}"/></html:link>
		</c:if>
	</c:otherwise>
</c:choose>

<c:if test="${pageContext.request.userPrincipal != null}">
	<span class="authInfo">
		<fmt:message key="label.username"/>
		${pageContext.request.userPrincipal.name}
	</span>
</c:if>

<span class="banner"><fmt:message key="application.name"/></span>

<ul class="menu">
	<li><html:link page="/" styleId="homeLink"><fmt:message key="link.home"/></html:link></li>
	<c:if test="${stateManager.running}">
		<li><html:link forward="report"><fmt:message key="link.report.build.history"/></html:link></li>
		<li><html:link forward="manualBuildForm"><fmt:message key="link.build.manual"/></html:link></li>
	</c:if>
	<li><html:link forward="viewPreferences"><fmt:message key="link.preferences"/></html:link></li>
	<li><html:link forward="setup"><fmt:message key="link.setup"/></html:link></li>
</ul>


</div>
