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
	<c:set var="messages" value="${v:getEvents('AUDIT,INFO,WARNING,ERROR')}"/>
	<c:choose>
		<c:when test="${empty messages}">
			<spring:message code="messages.none"/>
		</c:when>
		<c:otherwise>
			<table class="sortable">
				<caption>System Messages</caption>
				<thead>
					<tr>
						<th class="timestamp">Date</th>
						<th>Type</th>
						<th>Message</th>
					</tr>
				</thead>
				<tbody>
					<c:forEach items="${messages}" var="msg">
						<tr>
							<td class="timestamp"><fmt:formatDate value="${msg.date}" type="both"/></td>
							<td>${msg.class.name}</td>
							<td><spring:message code="${msg.key}" arguments="${msg.args}" htmlEscape="true"/></td>
						</tr>
					</c:forEach>
				</tbody>
			</table>	
			<html:link styleClass="confirm" forward="clearMessages"><spring:message code="link.clear.messages"/></html:link>
		</c:otherwise>
	</c:choose>
</body>
</html>
