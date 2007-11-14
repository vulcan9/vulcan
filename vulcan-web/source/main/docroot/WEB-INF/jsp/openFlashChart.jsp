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
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/openFlashChart.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<style type="text/css">
		.caption, caption {
			font: Calibri, Helvetica, Verdana, Arial, sans-serif;
			font-weight: normal;
			color: #7E97A6;
			font-size: 16pt;
			
		}
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
		td.numeric {
			text-align: right;
		}
		table.build-report, table.build-report td {
			border: 0 !important;
		}
		table.build-report td {
			padding-left: 2em;
		}
	</style>
</head>
<body>
	<div style="margin-left: 1.5%;">
		<table class="build-report">
			<caption>
				Build Report 
			</caption>
			<tbody>
				<tr>
					<td>Project(s)</td>
					<td>
						<c:forEach items="${reportForm.projectNames}" var="name" varStatus="loop">
							<c:if test="${loop.index eq 0}">
								<c:out value="${name}"/>
							</c:if>
							<c:if test="${loop.index gt 0}">
								<c:out value=", ${name}"/>
							</c:if>
						</c:forEach>
					</td>
				</tr>					
				<tr>
					<td>Range</td>
					<td>
						<c:choose>
							<c:when test="${fromLabel.class.name eq 'java.util.Date'}">
								<fmt:formatDate value="${fromLabel}" type="date" dateStyle="full"/>
								through
								<fmt:formatDate value="${toLabel}" type="date" dateStyle="full"/>
							</c:when>
							<c:otherwise>
								${fromLabel} to ${toLabel}
							</c:otherwise>
						</c:choose>
					</td>
				</tr>
				<tr>
					<td>Success rate</td>
					<td><fmt:formatNumber value="${successCount / sampleCount}" type="percent"/> (${successCount}/${sampleCount} builds)</td>
				</tr>
				<tr>
					<td>Average time to fix a broken build</td>
					<td><v:formatElapsedTime value="${averageTimeToFixBuild}"/></td>
				</tr>
				<tr>
					<td>Longest time to fix a broken build</td>
					<td><v:formatElapsedTime value="${longestTimeToFixBuild}"/></td>
				</tr>
			</tbody>
		</table>
	</div>
	
	<div style="clear: both;"/>
	
	<c:if test="${not empty availableMetrics}">
	<div class="chart">
		<div class="caption">
			Trend Analysis
		</div>
		<form id="metricsForm" action="#" method="post">
			<div class="plotOptions">
				<span>Plot 1: </span>
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
				<span>Plot 2: </span>
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
				<button id="btnRefresh">Refresh</button>
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
			Build Durations
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
			<caption>Top Errors</caption>
			<thead>
				<tr>
					<th>Error</th>
					<th>Count</th>
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
			<caption>Top Test Failures</caption>
			<thead>
				<tr>
					<th>Test Case</th>
					<th>Count</th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${topTestFailures}" var="e">
					<tr>
						<td>${e.name}</td>
						<td class="numeric"><fmt:formatNumber value="${e.count}"/></td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</div>
	</c:if>
</body>
</html>