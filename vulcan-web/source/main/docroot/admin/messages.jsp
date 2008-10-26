<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<jsp:directive.page session="false"/>

<html:xhtml/>

<body>
	<c:choose>
		<c:when test="${empty eventPool['WARNING'] and empty eventPool['ERROR'] and empty eventPool['AUDIT']}">
			<v:bubble styleClass="message"><spring:message code="messages.none"/></v:bubble>
		</c:when>
		<c:otherwise>
			<c:forEach items="${eventPool['ERROR']}" var="msg">
				<div class="error">
					<fmt:formatDate value="${msg.date}" type="both"/>:  <spring:message code="${msg.key}" arguments="${msg.args}" htmlEscape="true"/>
				</div>
			</c:forEach>	
			<c:forEach items="${eventPool['WARNING']}" var="msg">
				<div class="warning">
					<fmt:formatDate value="${msg.date}" type="both"/>:  <spring:message code="${msg.key}" arguments="${msg.args}" htmlEscape="true"/>
				</div>
			</c:forEach>
			<c:forEach items="${eventPool['AUDIT']}" var="msg">
				<div>
					<fmt:formatDate value="${msg.date}" type="both"/>:  <spring:message code="${msg.key}" arguments="${msg.args}" htmlEscape="true"/>
				</div>
			</c:forEach>	
			<div><html:link styleClass="confirm" forward="clearMessages"><spring:message code="link.clear.messages"/></html:link></div>
		</c:otherwise>
	</c:choose>
</body>
</html>
