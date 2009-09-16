<?xml version="1.0" encoding="UTF-8" ?>

<page:applyDecorator
		xmlns="http://www.w3.org/1999/xhtml"
		xmlns:page="http://www.opensymphony.com/sitemesh/page"
		xmlns:jsp="http://java.sun.com/JSP/Page"
		xmlns:spring="http://www.springframework.org/tags"
		xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags"
		name="main">

	<jsp:output	omit-xml-declaration="true"/>
	
	<jsp:directive.page session="false" contentType="text/html;charset=UTF-8"/>

	<v:bubble styleClass="error">
		<span class="error"><spring:message code="errors.internal"/></span>
	</v:bubble>
	
</page:applyDecorator>
