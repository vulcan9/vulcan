<?xml version="1.0" encoding="UTF-8" ?>

<div
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

	<jsp:output omit-xml-declaration="true"/>
	<jsp:directive.page contentType="application/xml"/>

	<c:if test="${projectImportStatus ne null}">
		<html:img page="/images/progressbar.gif" alt="Processing..."/>
		
		<p class="processing">
			<spring:message code="messages.project.import.status"
				arguments="${projectImportStatus.numProjectsCreated},${projectImportStatus.numRemainingModules}"/> 
		</p>
		<p class="status-detail">
			<spring:message code="messages.project.import.current.url"
				arguments="${projectImportStatus.currentUrl}"/>
		</p>
	</c:if>
</div>