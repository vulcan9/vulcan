<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:html="http://struts.apache.org/tags-html">

<jsp:directive.page session="false"/>

<jsp:output
	omit-xml-declaration="false"
	doctype-root-element="html"
	doctype-public="-//W3C//DTD XHTML 1.1//EN"
	doctype-system="http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"/>


<html:xhtml/>

<head>
	<title>
		<spring:message code="header.directory.listing" arguments="${fileListPath}"/>
	</title>
	<jsp:element name="link">
		<jsp:attribute name="rel">stylesheet</jsp:attribute>
		<jsp:attribute name="type">text/css</jsp:attribute>
		<jsp:attribute name="href"><c:url value="/css/standard.css"/></jsp:attribute>
	</jsp:element>
</head>

<body>
	<ul>
		<li><a href="../">..</a></li>
		<c:forEach var="file" items="${fileList}">
			<c:set var="name" value="${file.name}"/>
			<c:if test="${file.directory}">
				<c:set var="name" value="${name}/"/>
			</c:if>
			
			<li>
				<jsp:element name="a">
					<jsp:attribute name="href"><c:out value="${name}" escapeXml="true"/></jsp:attribute>
					<jsp:body><c:out value="${name}" escapeXml="true"/></jsp:body>
				</jsp:element>
			</li>
		</c:forEach>
	</ul>
</body>
</html>
