<rss version="2.0"
		xmlns:jsp="http://java.sun.com/JSP/Page"
		xmlns:c="http://java.sun.com/jsp/jstl/core"
		xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
		xmlns:spring="http://www.springframework.org/tags">

	<jsp:directive.page contentType="application/rss+xml" session="false"/>
	<jsp:output omit-xml-declaration="false"/>
	
	<fmt:message key="rss.date.format" var="datePattern"/>
	
	<channel>
		<title><fmt:message key="rss.title"/></title>
		<description><fmt:message key="rss.description"/></description>

		<c:set var="req" value="${pageContext.request}"/>
		<c:set var="serverURL">${req.scheme}://${req.serverName}:${req.serverPort}</c:set>
		<c:set var="contextRoot">${serverURL}<c:url value="/"/></c:set>
		<link>${contextRoot}</link>

		<c:forEach items="${eventPool['BUILD']}" var="event">
			<item>
				<title>${event.projectConfig.name} - ${event.status.status} - Build ${event.status.buildNumber} - ${event.status.revision}</title>
				<pubDate><fmt:formatDate value="${event.status.completionDate}" pattern="${datePattern}"/></pubDate>
				<link>${contextRoot}viewProjectStatus.do?transform=xhtml&amp;amp;projectName=${event.projectConfig.name}&amp;amp;index=${event.status.buildNumber}</link>
				<description><spring:message code="${event.key}" arguments="${event.args}" htmlEscape="true"/></description>
			</item>
		</c:forEach>
	</channel>
</rss>
