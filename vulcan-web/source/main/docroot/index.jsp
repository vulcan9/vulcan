<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:x="http://java.sun.com/jsp/jstl/xml"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<html:xhtml/>

<head>
	<jsp:element name="link">
		<jsp:attribute name="title">RSS</jsp:attribute>
		<jsp:attribute name="rel">alternate</jsp:attribute>
		<jsp:attribute name="type">application/rss+xml</jsp:attribute>
		<jsp:attribute name="href"><c:url value="/rss.jsp"/></jsp:attribute>
	</jsp:element>
	<c:if test="${preferences.reloadInterval > 0}">
		<jsp:element name="meta">
			<jsp:attribute name="http-equiv">Refresh</jsp:attribute>
			<jsp:attribute name="content">${preferences.reloadInterval}</jsp:attribute>
		</jsp:element>
	</c:if>
</head>
<body>

<c:choose>
<c:when test="${not stateManager.running and not buildOutcomeConverter.running}">
	<v:bubble styleClass="error"><span class="error"><spring:message code="errors.not.running"/></span></v:bubble>
</c:when>
<c:when test="${buildOutcomeConverter.running}">
	<c:set var="pctComplete" value="${buildOutcomeConverter.convertedCount * 100 / buildOutcomeConverter.totalCount}"/>
	<v:bubble styleClass="warning">
		<span class="warning">
			Vulcan is converting build history to a new storage layer 
			(<fmt:formatNumber value="${pctComplete}" maxFractionDigits="0"/>% complete).
		</span>
	</v:bubble>
</c:when>
<c:otherwise>
<div class="tables">

<c:import url="/xsl/projects.xsl" var="xslt"/>

<v:bubble>
	<x:transform xslt="${xslt}">
		<x:param name="caption">
			<spring:message code="captions.projects.status"/>
		</x:param>
		<x:param name="sortUrl">
			<c:url value="/managePreferences.do?action=save"/>
		</x:param>
		<x:param name="detailLink">
			<c:url value="/viewProjectStatus.do?transform=xhtml&amp;projectName="/>
		</x:param>
		<x:param name="nameHeader">
			<spring:message code="th.project.name"/>
		</x:param>
		<x:param name="buildNumberHeader">
			<spring:message code="th.build.number"/>
		</x:param>
		<x:param name="ageHeader">
			<spring:message code="th.age"/>
		</x:param>
		<x:param name="tagHeader">
			<spring:message code="th.tagName"/>
		</x:param>
		<x:param name="revisionHeader">
			<spring:message code="th.revision"/>
		</x:param>
		<x:param name="statusHeader">
			<spring:message code="th.project.status"/>
		</x:param>
		<x:param name="timestampLabel">
			<spring:message code="label.build.timestamp"/>
		</x:param>
		<x:param name="sortSelect">${preferences.sortColumn}</x:param>
		<x:param name="sortOrder">${preferences.sortOrder}</x:param>
		
		<v:projectStatusXml labels="${preferences.labels}"/>
	</x:transform>
</v:bubble>

<c:if test="${preferences.showBuildDaemons}">
<v:bubble>
<table class="buildDaemons">
	<caption><spring:message code="captions.build.daemons"/></caption>
	<thead>
		<tr>
			<th><spring:message code="th.build.daemon.name"/></th>
			<th><spring:message code="th.build.daemon.project"/></th>
			<th><spring:message code="th.build.daemon.status"/></th>
			<th><spring:message code="th.build.daemon.detail"/></th>
			<th><spring:message code="th.control"/></th>
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
						<c:set var="key" value="${daemon.phaseMessageKey}"/>
						<c:if test="${key == null}">
							<c:set var="key" value="build.phase.build"/>
						</c:if>
						<spring:message code="${key}"/>
					</c:when>
					<c:otherwise>
						<spring:message code="build.daemon.idle"/>
					</c:otherwise>										
				</c:choose>
			</td>
			<td>
				<c:if test="${daemon.detail ne null}">
					<spring:message code="${daemon.detail}" arguments="${daemon.detailArgs}"/>
				</c:if>
			</td>
			<td>
				<c:if test="${isBuilding}">
					<html:link forward="killBuild" paramId="daemonName"
							paramName="daemon" paramProperty="name"
							styleClass="confirm">
						<spring:message code="link.build.abort"/>
					</html:link>
				</c:if>				
			</td>
		</tr>
	</c:forEach>
	</tbody>
</table>
</v:bubble>
</c:if>

<c:if test="${preferences.showBuildQueue}">
<v:bubble>
<table class="buildQueue">
	<caption><spring:message code="captions.build.queue"/></caption>
	<thead>
		<tr>
			<th><spring:message code="th.project.name"/></th>
			<th><spring:message code="th.project.status"/></th>
		</tr>
	</thead>
	<tbody>
		<c:set var="pendingTargets" value="${stateManager.buildManager.pendingTargets}"/>
		<c:choose>
			<c:when test="${empty pendingTargets}">
				<tr>
					<td colspan="2"><spring:message code="label.empty"/></td>
				</tr>
			</c:when>
			<c:when test="${not empty pendingTargets}">
				<c:set var="hasPending" value="false"/>
				<c:forEach items="${pendingTargets}" var="status">
					<tr>
						<td>${status.name}</td>
						<td>
							<c:if test="${status.inQueue}">
								<spring:message code="build.queue.project.waiting"/>
								<c:set var="hasPending" value="true"/>
							</c:if>
							<c:if test="${status.building}">
								<spring:message code="build.queue.project.building"/>
							</c:if>
						</td>
					</tr>
				</c:forEach>
				<c:if test="${hasPending}">
					<tr>
						<td colspan="2">
							<html:link forward="flushQueue" styleClass="confirm"><spring:message code="link.flush.queue"/></html:link>					
						</td>
					</tr>
				</c:if>
			</c:when>
		</c:choose>
	</tbody>
</table>
</v:bubble>
</c:if>

<c:if test="${preferences.showSchedulers}">
<v:bubble>
<table class="schedulers">
	<caption><spring:message code="captions.schedulers"/></caption>
	<thead>
		<tr>
			<th><spring:message code="th.scheduler.name"/></th>
			<th><spring:message code="th.scheduler.timestamp"/></th>
			<th><spring:message code="th.control"/></th>
		</tr>
	</thead>
	<tbody>
	<spring:message code="scheduler.timestamp.pattern" var="schedPattern"/>
	<c:forEach items="${stateManager.schedulers}" var="sched">
		<fmt:formatDate value="${sched.nextExecutionDate}" var="date" pattern="${schedPattern}"/>
		<tr>
			<td>${sched.name}</td>
			<td>
				<c:out value="${date}" default="(paused)"/>
			</td>
			<td>
				<html:link forward="toggleScheduler" paramId="schedulerName"
							paramName="sched" paramProperty="name">
					<c:choose>
						<c:when test="${date ne null}">
							<spring:message code="label.pause.scheduler"/>
						</c:when>
						<c:otherwise>
							<spring:message code="label.unpause.scheduler"/>
						</c:otherwise>
					</c:choose>
				</html:link>
			</td>
		</tr>
	</c:forEach>
	</tbody>
</table>
</v:bubble>
</c:if>
</div>
</c:otherwise>
</c:choose>
</body>
</html>
