<?xml version="1.0" encoding="utf-8"?>
<jsp:root version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:c="http://java.sun.com/jsp/jstl/core">
    
    <jsp:directive.tag display-name="button" dynamic-attributes="false"/>

	<jsp:directive.attribute name="name" required="true" type="java.lang.String" rtexprvalue="true"/>
	<jsp:directive.attribute name="value" required="true" type="java.lang.String" rtexprvalue="true"/>
	
	<c:choose>
		<c:when test="${browserIE}">
			<button name="${name}-button" value="${value}" type="button" actual-value="${value}" class="ie-submit-button">
				<jsp:doBody/>
			</button>
			
			<c:if test="${ieSubmit eq null}">
				<c:set var="ieSubmit" value="true" scope="request"/>
				<input type="submit" name="${name}" id="ieSubmit" style="display: none;"/>
			</c:if>
		</c:when>
		<c:otherwise>
			<button name="${name}" value="${value}" type="submit">
				<jsp:doBody/>
			</button>
		</c:otherwise>
	</c:choose>
</jsp:root>
