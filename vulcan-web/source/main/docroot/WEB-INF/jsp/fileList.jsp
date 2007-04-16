<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://struts.apache.org/tags-html">

<jsp:directive.page session="false"/>

<jsp:output
	omit-xml-declaration="false"
	doctype-root-element="html"
	doctype-public="-//W3C//DTD XHTML 1.1//EN"
	doctype-system="http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"/>


<html:xhtml/>
<body>
	<fmt:message key="header.directory.listing">
		<fmt:param value="${fileListPath}"/>
	</fmt:message>
	<ul>
		<li><a href="../">..</a></li>
		<c:forEach var="file" items="${fileList}">
			<c:set var="name" value="${file.name}"/>
			<c:if test="${file.directory}">
				<c:set var="name" value="${name}/"/>
			</c:if>
			
			<li>
				<jsp:element name="a">
					<jsp:attribute name="href">${name}</jsp:attribute>
					<jsp:body>${name}</jsp:body>
				</jsp:element>
			</li>
		</c:forEach>
	</ul>
</body>
</html>
