<?xml version="1.0" encoding="utf-8"?>
<jsp:root version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fn="http://java.sun.com/jsp/jstl/functions">
    
	<jsp:directive.tag display-name="formatTestName" dynamic-attributes="false"/>
    
	<jsp:directive.attribute name="value" required="true" type="java.lang.String" rtexprvalue="true"/>
	
	<c:set var="names" value="${fn:split(value,'.')}"/>
	
	<c:set var="count" value="0"/>
	<c:forEach items="${names}" var="n">
		<c:set var="count" value="${count + 1}"/>
	</c:forEach>
	
	<span class="test-name">${names[count-1]}</span>
	
	<span class="test-namespace">
	<c:forEach items="${names}" var="n" end="${count-2}" varStatus="status">
		<c:if test="${status.index != 0}">
			<c:out value="."/>
		</c:if>
		<c:out value="${n}"/>
	</c:forEach>
	</span>
</jsp:root>
