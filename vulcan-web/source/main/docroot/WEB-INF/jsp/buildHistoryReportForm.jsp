<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:x="http://java.sun.com/jsp/jstl/xml"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<html:xhtml/>

<head>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/reportForm.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
</head>
<body>

<v:messages/>

<v:bubble styleClass="buildHistoryReportForm">
<html:form action="/viewProjectBuildHistory" method="get">
<table class="buildHistoryReportForm">
	<caption><fmt:message key="captions.report.build.history"/></caption>
	<tbody>
		<tr>
			<td rowspan="3"><fmt:message key="label.range"/></td>
			<td>
				<html:radio property="rangeType" value="date" styleId="rangeByDate"/>
				<label for="rangeByDate"><fmt:message key="label.range.by.date"/></label>
				<ul class="dateRanges">
					<li>
						<html:radio property="dateRangeSelector" value="today" styleId="dateRangeToday"/>
						<label for="dateRangeToday"><fmt:message key="label.range.date.today"/></label>
					</li>
					<li>
						<html:radio property="dateRangeSelector" value="weekToDate" styleId="dateRangeWeekToDate"/>
						<label for="dateRangeWeekToDate"><fmt:message key="label.range.date.week.to.date"/></label>
					</li>
					<li>
						<html:radio property="dateRangeSelector" value="monthToDate" styleId="dateRangeMonthToDate"/>
						<label for="dateRangeMonthToDate"><fmt:message key="label.range.date.month.to.date"/></label>
					</li>
					<li>
						<html:radio property="dateRangeSelector" value="yearToDate" styleId="dateRangeYearToDate"/>
						<label for="dateRangeYearToDate"><fmt:message key="label.range.date.year.to.date"/></label>
					</li>
					<li>
						<html:radio property="dateRangeSelector" value="specific" styleId="dateRangeSpecific"/>
						<label for="dateRangeSpecific"><fmt:message key="label.range.date.specific"/></label>
					</li>
				</ul>
			</td>
			<td class="dateInputs">
				<fmt:message key="label.date.start"/>
				<html:text property="startDate" styleClass="buildNumber"/>
				<html:messages property="startDate" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
			<td class="dateInputs">
				<fmt:message key="label.date.end"/>
				<html:text property="endDate" styleClass="buildNumber"/>
				<html:messages property="endDate" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<tr>
			<td>
				<html:radio property="rangeType" value="index" styleId="rangeByIndex"/>
				<label for="rangeByIndex"><fmt:message key="label.range.by.index"/></label>
			</td>
			<td>
				<fmt:message key="label.index.start"/>
				<html:text property="minBuildNumber" styleClass="buildNumber"/>
				<html:messages property="startIndex" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
			<td>
				<fmt:message key="label.index.end"/>
				<html:text property="maxBuildNumber" styleClass="buildNumber"/>
				<html:messages property="endIndex" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<tr>
			<td colspan="3">
				<html:radio property="rangeType" value="all" styleId="rangeAll"/>
				<label for="rangeAll"><fmt:message key="label.range.include.all"/></label>
			</td>
		</tr>
		<tr>
			<td><fmt:message key="label.projects"/></td>
			<td colspan="3">
				<div class="projectCheckboxes">
					<ul>
						<c:forEach items="${stateManager.projectConfigNames}" var="projectName">
							<li>
								<c:choose>
									<c:when test="${reportForm.dateMode}">
										<html:multibox property="projectNames" value="${projectName}"
											styleId="target_${v:mangle(projectName)}"/>
									</c:when>
									<c:otherwise>
										<html:radio property="projectNames" value="${projectName}"
											styleId="target_${v:mangle(projectName)}"/>
									</c:otherwise>
								</c:choose>
								<jsp:element name="label">
									<jsp:attribute name="for">target_${v:mangle(projectName)}</jsp:attribute>
									<jsp:body>${projectName}</jsp:body>
								</jsp:element>
							</li>
						</c:forEach>
					</ul>
				</div>
				<html:messages property="projectNames" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<tr>
			<td><fmt:message key="label.options"/></td>
			<td colspan="3">
				<ul class="metaDataOptions">
					<li>
						<html:multibox property="omitTypes" value="SKIP" styleId="omitSkip"/>
						<label for="omitSkip"><fmt:message key="label.report.omit.skip"/></label>
					</li>
					<li>
						<html:multibox property="omitTypes" value="ERROR" styleId="omitError"/>
						<label for="omitError"><fmt:message key="label.report.omit.error"/></label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><fmt:message key="label.transform"/></td>
			<td colspan="3">
				<html:select property="transform" styleClass="transform">
					<html:option value="OpenFlashChart">Open Flash Chart</html:option>
					<html:option value="xhtml">XHTML</html:option>
					<html:option value="">XML</html:option>
				</html:select>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="download" value="false" styleId="download_f"/>
						<label for="download_f">
							<fmt:message key="label.report.in.browser"/>
						</label>
					</li>
					<li>
						<html:radio property="download" value="true" styleId="download_t"/>
						<label for="download_t">
							<fmt:message key="label.report.download"/>
						</label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td class="buttons" colspan="4">
				<html:submit property="action" value="Submit"/>
			</td>
		</tr>
	</tbody>
</table>
</html:form>
</v:bubble>

</body>
</html>
