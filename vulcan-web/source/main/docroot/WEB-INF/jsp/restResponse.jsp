<?xml version="1.0" encoding="UTF-8" ?>

<rest-response
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

	<jsp:directive.page contentType="application/xml" session="false"/>
	
	<c:set var="keys" value="${v:getActionErrorPropertyList(pageContext.request)}"/>
	
	<c:if test="${not empty keys}">
		${v:setStatus(pageContext.response, 400)}
	</c:if>
	
	<c:forEach var="prop" items="${keys}">
		<html:messages id="msg" property="${prop}">
			<error request-parameter="${prop}">${msg}</error>
		</html:messages>
	</c:forEach>

</rest-response>