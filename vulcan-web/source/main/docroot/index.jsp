<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:x="http://java.sun.com/jsp/jstl/xml"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://jakarta.apache.org/struts/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<jsp:directive.page session="false"/>

<html:xhtml/>

<head>
	<jsp:element name="link">
		<jsp:attribute name="title">RSS</jsp:attribute>
		<jsp:attribute name="rel">alternate</jsp:attribute>
		<jsp:attribute name="type">application/rss+xml</jsp:attribute>
		<jsp:attribute name="href"><c:url value="/rss.jsp"/></jsp:attribute>
	</jsp:element>
	<jsp:element name="meta">
		<jsp:attribute name="http-equiv">Refresh</jsp:attribute>
		<jsp:attribute name="content">30</jsp:attribute>
	</jsp:element>
</head>
<body>

<c:choose>
<c:when test="${not stateManager.running}">
	<v:bubble styleClass="error"><span class="error"><fmt:message key="errors.not.running"/></span></v:bubble>
</c:when>
<c:otherwise>
<div class="tables">

<c:import url="/xsl/projects.xsl" var="xslt"/>

<v:bubble>
	<x:transform xslt="${xslt}">
		<x:param name="caption">
			<fmt:message key="captions.projects.status"/>
		</x:param>
		<x:param name="detailLink">
			<c:url value="/viewProjectStatus.do?transform=xhtml&amp;projectName="/>
		</x:param>
		<x:param name="nameHeader">
			<fmt:message key="th.project.name"/>
		</x:param>
		<x:param name="buildNumberHeader">
			<fmt:message key="th.build.number"/>
		</x:param>
		<x:param name="ageHeader">
			<fmt:message key="th.age"/>
		</x:param>
		<x:param name="tagHeader">
			<fmt:message key="th.tagName"/>
		</x:param>
		<x:param name="revisionHeader">
			<fmt:message key="th.revision"/>
		</x:param>
		<x:param name="statusHeader">
			<fmt:message key="th.project.status"/>
		</x:param>
		<x:param name="timestampLabel">
			<fmt:message key="label.build.timestamp"/>
		</x:param>
		<x:param name="sortSelect">${preferences.sortColumn}</x:param>
		<x:param name="sortOrder">${preferences.sortOrder}</x:param>
		<c:import url="/projects.jsp"/>
	</x:transform>
</v:bubble>

<v:bubble>
<table class="buildDaemons">
	<caption><fmt:message key="captions.build.daemons"/></caption>
	<thead>
		<tr>
			<th><fmt:message key="th.build.daemon.name"/></th>
			<th><fmt:message key="th.build.daemon.project"/></th>
			<th><fmt:message key="th.build.daemon.status"/></th>
			<th><fmt:message key="th.build.daemon.detail"/></th>
			<th><fmt:message key="th.build.daemon.control"/></th>
		</tr>
	</thead>
	<tbody>
	<c:forEach items="${stateManager.buildDaemons}" var="daemon">
		<c:set var="isBuilding" value="${daemon.building}"/>
		<tr>
			<td>${daemon.name}</td>
			<td>
				<c:if test="${isBuilding}">
					${daemon.currentTarget.name}
				</c:if>
			</td>
			<td>
				<c:choose>
					<c:when test="${isBuilding}">
						<c:set var="key" value="${daemon.phase}"/>
						<c:if test="${key == null}">
							<c:set var="key" value="build.phase.build"/>
						</c:if>
						<fmt:message key="${key}"/>
					</c:when>
					<c:otherwise>
						<fmt:message key="build.daemon.idle"/>
					</c:otherwise>										
				</c:choose>
			</td>
			<td>
				<c:if test="${daemon.detail ne null}">
					${daemon.detail}
				</c:if>
			</td>
			<td>
				<c:if test="${isBuilding}">
					<html:link forward="killBuild" paramId="daemonName"
							paramName="daemon" paramProperty="name"
							styleClass="confirm">
						<fmt:message key="link.build.abort"/>
					</html:link>
				</c:if>				
			</td>
		</tr>
	</c:forEach>
	</tbody>
</table>
</v:bubble>

<v:bubble>
<table class="buildQueue">
	<caption><fmt:message key="captions.build.queue"/></caption>
	<thead>
		<tr>
			<th><fmt:message key="th.project.name"/></th>
			<th><fmt:message key="th.project.status"/></th>
		</tr>
	</thead>
	<tbody>
		<c:set var="pendingTargets" value="${stateManager.buildManager.pendingTargets}"/>
		<c:choose>
			<c:when test="${empty pendingTargets}">
				<tr>
					<td colspan="2"><fmt:message key="label.empty"/></td>
				</tr>
			</c:when>
			<c:when test="${not empty pendingTargets}">
				<c:set var="hasPending" value="false"/>
				<c:forEach items="${pendingTargets}" var="status">
					<tr>
						<td>${status.name}</td>
						<td>
							<c:if test="${status.inQueue}">
								<fmt:message key="build.queue.project.waiting"/>
								<c:set var="hasPending" value="true"/>
							</c:if>
							<c:if test="${status.building}">
								<fmt:message key="build.queue.project.building"/>
							</c:if>
						</td>
					</tr>
				</c:forEach>
				<c:if test="${hasPending}">
					<tr>
						<td colspan="2">
							<html:link forward="flushQueue" styleClass="confirm"><fmt:message key="link.flush.queue"/></html:link>					
						</td>
					</tr>
				</c:if>
			</c:when>
		</c:choose>
	</tbody>
</table>
</v:bubble>

<v:bubble>
<table class="schedulers">
	<caption><fmt:message key="captions.schedulers"/></caption>
	<thead>
		<tr>
			<th><fmt:message key="th.scheduler.name"/></th>
			<th><fmt:message key="th.scheduler.timestamp"/></th>
		</tr>
	</thead>
	<tbody>
	<fmt:message key="scheduler.timestamp.pattern" var="schedPattern"/>
	<c:forEach items="${stateManager.schedulers}" var="sched">
		<tr>
			<td>${sched.name}</td>
			<td>
				<fmt:formatDate value="${sched.nextExecutionDate}" var="date" pattern="${schedPattern}"/>
				<c:out value="${date}" default="(disabled)"/>
			</td>
		</tr>
	</c:forEach>
	</tbody>
</table>
</v:bubble>
</div>
</c:otherwise>
</c:choose>
</body>
</html>
