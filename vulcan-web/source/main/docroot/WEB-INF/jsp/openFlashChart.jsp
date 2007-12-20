<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:x="http://java.sun.com/jsp/jstl/xml"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:fn="http://java.sun.com/jsp/jstl/functions"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">
<html:xhtml/>
<head>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/openFlashChart.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<style type="text/css">
		div.pane, div.chart {
			float: left;
			width: 45%;
			margin-left: 2%;
			margin-right: 2%;
			margin-top: 2ex;
			margin-bottom: 2ex;
		}
		div.chart {
			height: 360px;		
		}
		div.pane table {
			width: 100%;
			border: 1px solid #bdbabd;
		}
		div.plotOptions {
			margin-top: 2ex;
			margin-left: 2em;
		}
		div.plotOptions, div.plotOptions select, div.plotOptions button {
			font-size: 8pt;
		}
		div.plotOptions select {
			margin-right: 1em;
		}
		caption {
			background-color: white !important;
		}
		table.build-report, table.build-report td {
			border: 0 !important;
		}
		table.build-report td {
			padding-left: 2em;
		}
		.longest-broken-build-details {
			margin-left: 2em;
			color: #666666;
			font-style: italic;
		}
	</style>
</head>
<body>
	<div style="margin-left: 1.5%;">
		<table class="build-report">
			<caption>
				<spring:message code="captions.report.build.history"/>
			</caption>
			<tbody>
				<tr>
					<td><spring:message code="label.projects"/></td>
					<td>${fn:join(reportForm.projectNames, ", ")}</td>
				</tr>					
				<tr>
					<td><spring:message code="label.range"/></td>
					<td>
						<c:choose>
							<c:when test="${fromLabel.class.name eq 'java.util.Date'}">
								<fmt:formatDate value="${fromLabel}" type="date" dateStyle="full"/>
								<c:out value=" "/><spring:message code="label.through"/><c:out value=" "/>
								<fmt:formatDate value="${toLabel}" type="date" dateStyle="full"/>
							</c:when>
							<c:otherwise>
								${fromLabel} to ${toLabel}
							</c:otherwise>
						</c:choose>
					</td>
				</tr>
				<tr>
					<td><spring:message code="label.success.rate"/></td>
					<td>
						<fmt:formatNumber value="${successCount / sampleCount}" type="percent"/>
						(${successCount}/${sampleCount} <spring:message code="label.builds"/>)
					</td>
				</tr>
				<tr>
					<td><spring:message code="label.avg.time.to.fix.build"/></td>
					<td><v:formatElapsedTime value="${averageTimeToFixBuild}" verbosity="2"/></td>
				</tr>
				<tr>
					<td><spring:message code="label.max.time.to.fix.build"/></td>
					<td>
						<v:formatElapsedTime value="${longestTimeToFixBuild}" verbosity="2"/>
						<span class="longest-broken-build-details">
							<spring:message code="label.failed.build"/>
							<c:out value=" "/>
							<v:buildReportLink projectName="${longestElapsedFailureName}"
								buildNumber="${failingBuildNumber}" text="${failingBuildNumber}"/>
							<c:if test="${fn:join(reportForm.projectNames, ',') != longestElapsedFailureName}">
								<c:out value=" "/>
								<spring:message code="label.of.project"/>
								<c:out value=" ${longestElapsedFailureName}"/>
							</c:if>
							<c:if test="${fixedInBuildNumber != null}">
								<c:out value=" "/>
								<spring:message code="label.fixed.in.build"/>
								<c:out value=" "/>
								<v:buildReportLink projectName="${longestElapsedFailureName}"
									buildNumber="${fixedInBuildNumber}" text="${fixedInBuildNumber}"/>
							</c:if>
						</span>
					</td>
				</tr>
			</tbody>
		</table>
	</div>
	
	<div style="clear: both;"/>
	
	<c:if test="${not empty availableMetrics}">
	<div class="chart">
		<div class="caption">
			<spring:message code="label.trend.analysis"/>
		</div>
		<form id="metricsForm" action="#" method="post">
			<div class="plotOptions">
				<span><spring:message code="label.trend.analysis.first.plot"/></span>
				<select id="metric1">
					<option value="none">&amp;nbsp;</option>
					<c:forEach items="${availableMetrics}" var="metricKey">
						<c:choose>
							<c:when test="${metricKey eq selectedMetric1}">
								<option value="${metricKey}" selected="selected">
									<spring:message code="${metricKey}" htmlEscape="true"/>
								</option>
							</c:when>
							<c:otherwise>
								<option value="${metricKey}">
									<spring:message code="${metricKey}" htmlEscape="true"/>
								</option>
							</c:otherwise>
						</c:choose>
					</c:forEach>
				</select>
				<span><spring:message code="label.trend.analysis.second.plot"/></span>
				<select id="metric2">
					<option value="none">&amp;nbsp;</option>
					<c:forEach items="${availableMetrics}" var="metricKey">
						<c:choose>
							<c:when test="${metricKey eq selectedMetric2}">
								<option value="${metricKey}" selected="selected">
									<spring:message code="${metricKey}" htmlEscape="true"/>
								</option>
							</c:when>
							<c:otherwise>
								<option value="${metricKey}">
									<spring:message code="${metricKey}" htmlEscape="true"/>
								</option>
							</c:otherwise>
						</c:choose>
					</c:forEach>
				</select>
				<button id="btnRefresh"><spring:message code="button.refresh"/></button>
				<c:url value="/getBuildHistory.do?transform=OpenFlashChart-Metrics" var="metricsUrl"/>
				<input type="hidden" id="metricsUrl" value="${metricsUrl}"/> 
			</div>
		</form>
		
		<v:openFlashChart
			name="metricsChart"
			width="100%"
			height="300"
			dataUrl=""/>
	</div>
	</c:if>
	<div class="chart">
		<div class="caption" style="margin-bottom: 3ex;">
			<spring:message code="label.trend.build.durations"/>
		</div>
	
		<v:openFlashChart
			name="buildDurationChart"
			width="100%"
			height="300"
			dataUrl="/getBuildHistory.do?transform=OpenFlashChart-BuildDuration"/>
	</div>
	
	<div style="clear: both;"/>
	
	<c:if test="${not empty topErrors}">
	<div class="pane">
		<table>
			<caption><spring:message code="captions.top.errors"/></caption>
			<thead>
				<tr>
					<th><spring:message code="th.message"/></th>
					<th><spring:message code="th.count"/></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${topErrors}" var="e">
					<tr>
						<td>${e.message}</td>
						<td class="numeric"><fmt:formatNumber value="${e.count}"/></td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</div>
	</c:if>

	<c:if test="${not empty topTestFailures}">
	<div class="pane">
		<table>
			<caption><spring:message code="captions.top.test.failures"/></caption>
			<thead>
				<tr>
					<th><spring:message code="label.test.failure.name"/></th>
					<th><spring:message code="th.count"/></th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${topTestFailures}" var="e">
					<tr>
						<td><v:formatTestName value="${e.name}"/></td>
						<td class="numeric"><fmt:formatNumber value="${e.count}"/></td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</div>
	</c:if>
</body>
</html>