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
	<c:if test="${preferences.reloadInterval > 0}">
		<script type="text/javascript">refreshDashboard(null, ${preferences.reloadInterval * 1000}, "<c:url value="/"/>");</script>
	</c:if>
	<!--  get the current time for calculating elapsed times and such. -->
	<jsp:useBean id="now" class="java.util.Date" scope="request"/>
</head>
<body>

<div id="loading-message">
	<spring:message code="ajax.loading"/>
</div>

<c:choose>
<c:when test="${not stateManager.running}">
	<v:bubble styleClass="error"><span class="error"><spring:message code="errors.not.running"/></span></v:bubble>
</c:when>
<c:otherwise>

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

<div id="current-activity">
	<c:if test="${preferences.showBuildDaemons}">
		<c:set var="buildingProjects" value="${stateManager.buildManager.projectsBeingBuilt}"/>
		
		<h3 class="caption"><spring:message code="captions.build.daemons"/></h3>
		<ul>
		<c:forEach items="${stateManager.buildDaemons}" var="daemon">
			<c:set var="isBuilding" value="${daemon.building}"/>
			<c:set var="target" value="${buildingProjects[daemon.currentTarget.name]}"/>
			<c:choose>
				<c:when test="${isBuilding}">
					<li>
						<spring:message code="messages.build.deamon.status" arguments="${daemon.name},${target.name}"/>
						<p class="phase">
							<spring:message code="${daemon.phaseMessageKey}"/>
							<c:if test="${daemon.detail ne null}">
								(<spring:message code="${daemon.detail}" arguments="${daemon.detailArgs}"/>)
							</c:if>
						</p>
						<c:if test="${target.estimatedBuildTimeMillis gt 0}">
							<c:set var="elapsedTime" value="${now.time - target.startDate.time}"/>
							<c:set var="pct" value="${elapsedTime / target.estimatedBuildTimeMillis}"/>
							
							<c:if test="${pct gt 1}">
								<c:set var="pct" value="1"/>
							</c:if>
							
							<c:set var="statusClass" value="progress-bar-success"/>
							
							<c:if test="${not empty target.errors}">
								<c:set var="statusClass" value="progress-bar-failure"/>
							</c:if>
							<div class="progress-bar">
								<jsp:element name="div">
									<jsp:attribute name="class">${statusClass}</jsp:attribute>
									<jsp:attribute name="style">width:<fmt:formatNumber value="${pct}" type="percent"/></jsp:attribute>
									<jsp:body><c:out value=""/></jsp:body>
								</jsp:element>
							</div>
							<c:if test="${target.estimatedBuildTimeMillis - elapsedTime gt 5}">
								<p class="time-remaining">
									About <v:formatElapsedTime value="${target.estimatedBuildTimeMillis - elapsedTime}" verbosity="1"/> remaining.
								</p>
							</c:if>
						</c:if>
						<html:link forward="killBuild" paramId="daemonName"
								paramName="daemon" paramProperty="name"
								styleClass="confirm async cancel-build">
							<spring:message code="link.build.abort"/>
						</html:link>
					</li>
				</c:when>
				<c:otherwise>
					<li class="idle">
						<spring:message code="messages.build.daemon.idle" arguments="${daemon.name}"/>
					</li>
				</c:otherwise>
			</c:choose>
<!-- 				<td>
					<c:if test="${isBuilding}">
						<html:link forward="killBuild" paramId="daemonName"
								paramName="daemon" paramProperty="name"
								styleClass="confirm async">
							<spring:message code="link.build.abort"/>
						</html:link>
					</c:if>				
				</td>
			</tr>-->
		</c:forEach>
		</ul>
	</c:if>
	
	<c:if test="${preferences.showBuildQueue}">
		<c:set var="pendingTargets" value="${stateManager.buildManager.pendingTargets}"/>
		<table class="dashboard">
			<caption><spring:message code="captions.build.queue"/></caption>
			<c:choose>
				<c:when test="${empty pendingTargets}">
					<tr>
						<td><spring:message code="label.empty"/></td>
					</tr>
				</c:when>
				<c:otherwise>
					<thead>
						<tr>
							<th><spring:message code="th.project"/></th>
							<th><spring:message code="th.status"/></th>
						</tr>
					</thead>
					<tbody>
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
									<html:link forward="flushQueue" styleClass="confirm async"><spring:message code="link.flush.queue"/></html:link>					
								</td>
							</tr>
						</c:if>
					</tbody>
				</c:otherwise>
			</c:choose>
		</table>
	</c:if>
	
	<c:if test="${preferences.showSchedulers}">
	<table class="dashboard">
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
								paramName="sched" paramProperty="name" styleClass="async">
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
</div>
</c:otherwise>
</c:choose>
</body>
</html>
