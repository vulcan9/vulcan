<?xml version="1.0" encoding="UTF-8" ?>

<rest-response
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

	<jsp:directive.page contentType="application/xml" session="false"/>
	
	<c:set var="keys" value="${v:getActionErrorPropertyList(pageContext.request)}"/>
	
	<c:if test="${not empty keys}">
		<c:if test="${restResponseCode eq null}">
			<c:set var="restResponseCode" value="400"/>
		</c:if>
		${v:setStatus(pageContext.response, restResponseCode)}
	</c:if>
	
	<c:forEach var="prop" items="${keys}">
		<html:messages id="msg" property="${prop}">
			<error request-parameter="${prop}">${msg}</error>
		</html:messages>
	</c:forEach>

</rest-response>