<?xml version="1.0" encoding="utf-8"?>
<jsp:root version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fn="http://java.sun.com/jsp/jstl/functions">
    
	<jsp:directive.tag display-name="formatElapsedTime" dynamic-attributes="false"/>
    
	<jsp:directive.attribute name="projectName" required="true" type="java.lang.String" rtexprvalue="true"/>
	<jsp:directive.attribute name="buildNumber" required="false" type="java.lang.Integer" rtexprvalue="true"/>
	<jsp:directive.attribute name="text" required="false" type="java.lang.String" rtexprvalue="true"/>
	
	<jsp:element name="a">
		<jsp:attribute name="class">external</jsp:attribute>
		<jsp:attribute name="href">
			<c:url value="/viewProjectStatus.do?transform=xhtml&amp;amp;projectName=${projectName}"/>
			<c:if test="${buildNumber != null}">
				<c:out value="&amp;amp;" escapeXml="false"/>
				<c:out value="buildNumber=${buildNumber}"/>
			</c:if>
		</jsp:attribute>
		<jsp:body>
			<c:choose>
				<c:when test="${text != null}">
					<c:out value="${text}"/>
				</c:when>
				<c:when test="${buildNumber != null}">
					<c:out value="${projectName} ${buildNumber}"/>
				</c:when>
				<c:otherwise>
					<c:out value="${projectName}"/>
				</c:otherwise>
			</c:choose>
		</jsp:body>
	</jsp:element>
</jsp:root>
