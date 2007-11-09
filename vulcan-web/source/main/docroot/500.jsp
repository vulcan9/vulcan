<?xml version="1.0" encoding="UTF-8" ?>

<page:applyDecorator
		xmlns="http://www.w3.org/1999/xhtml"
		xmlns:jsp="http://java.sun.com/JSP/Page"
		xmlns:c="http://java.sun.com/jsp/jstl/core"
		xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
		xmlns:page="http://www.opensymphony.com/sitemesh/page"
		xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags"
		name="main">

	<jsp:output	omit-xml-declaration="true"/>
	
	<jsp:directive.page session="false" contentType="text/html"/>

	<v:bubble styleClass="error">
		<span class="error"><fmt:message key="errors.internal"/></span>
	</v:bubble>
	
</page:applyDecorator>
