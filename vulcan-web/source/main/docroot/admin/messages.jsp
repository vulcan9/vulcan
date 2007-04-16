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
		<c:when test="${empty eventPool['WARNING'] and empty eventPool['ERROR']}">
			<v:bubble styleClass="message"><fmt:message key="messages.none"/></v:bubble>
		</c:when>
		<c:otherwise>
			<c:forEach items="${eventPool['ERROR']}" var="msg">
				<v:bubble styleClass="error">
				<span class="error">
					<fmt:formatDate value="${msg.date}" type="both"/>:  <spring:message code="${msg.key}" arguments="${msg.args}" htmlEscape="true"/>
				</span>
				</v:bubble>
			</c:forEach>	
			<c:forEach items="${eventPool['WARNING']}" var="msg">
				<v:bubble styleClass="warning">
				<span class="warning">
					<fmt:formatDate value="${msg.date}" type="both"/>:  <spring:message code="${msg.key}" arguments="${msg.args}" htmlEscape="true"/>
				</span>
				</v:bubble>
			</c:forEach>
			<span><html:link styleClass="confirm" forward="clearMessages"><fmt:message key="link.clear.messages"/></html:link></span>
		</c:otherwise>
	</c:choose>
</body>
</html>
