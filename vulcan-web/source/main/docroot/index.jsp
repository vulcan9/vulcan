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
		<script type="text/javascript">
			$(document).ready(function() {
				//TODO
			});
		</script>
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
			(<fmt:formatNumber value="${pctComplete}" maxFractionDigits="0"/>
			<c:out value="% complete; "/>
			<fmt:formatNumber value="${buildOutcomeConverter.convertedCount}" type="number"/>
			<c:out value=" out of "/>
			<fmt:formatNumber value="${buildOutcomeConverter.totalCount}" type="number"/>
			<c:out value=" builds converted)."/>
		</span>
	</v:bubble>
</c:when>
<c:otherwise>

<div id="content-header">
	<span class="">Latest activity</span>
	<ul class="inline flow">
		<li><html:link page="/managePreferences.do?action=save&amp;amp;clearLabels=true">All Projects</html:link></li>
		<c:forEach items="${stateManager.projectLabels}" var="label">
			<li>
				<c:set var="labelClass" value="project-label"/>
				<c:forEach items="${preferences.labels}" var="requestedLabel">
					<c:if test="${label eq requestedLabel}">
						<c:set var="labelClass" value="project-label project-label-active"/>
					</c:if>
				</c:forEach>
				
				<html:link styleClass="${labelClass}" page="/managePreferences.do?action=save" paramId="toggleLabel" paramName="label">
					<c:out value="${label}" escapeXml="true"/>
				</html:link>
			</li>
		</c:forEach>
	</ul>
</div>

<c:choose>
	<c:when test="${preferences.groupByLabel}">
		<c:choose>
			<c:when test="${not empty preferences.labels}">
				<c:set var="labels" value="${preferences.labels}"/>
			</c:when>
			<c:otherwise>
				<c:set var="labels" value="${stateManager.projectLabels}"/>
			</c:otherwise>
		</c:choose>
		<c:forEach items="${labels}" var="label">
			<v:projectStatusXml transform="true" labels="${label}" caption="${label}"/>
		</c:forEach>
	</c:when>
	<c:otherwise>
		<v:projectStatusXml transform="true" labels="${preferences.labels}"/>
	</c:otherwise>
</c:choose>

<c:if test="${preferences.showBuildDaemons}">
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
</c:if>

<c:if test="${preferences.showBuildQueue}">
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
</c:if>

<c:if test="${preferences.showSchedulers}">
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
</c:if>
</c:otherwise>
</c:choose>
</body>
</html>
