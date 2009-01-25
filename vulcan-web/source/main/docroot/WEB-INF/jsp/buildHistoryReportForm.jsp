<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
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
	<caption><spring:message code="captions.report.build.history"/></caption>
	<tbody>
		<tr>
			<td rowspan="3"><spring:message code="label.range"/></td>
			<td>
				<html:radio property="rangeType" value="date" styleId="rangeByDate"/>
				<label for="rangeByDate"><spring:message code="label.range.by.date"/></label>
				<ul class="dateRanges">
					<li>
						<html:radio property="dateRangeSelector" value="today" styleId="dateRangeToday"/>
						<label for="dateRangeToday"><spring:message code="label.range.date.today"/></label>
					</li>
					<li>
						<html:radio property="dateRangeSelector" value="weekToDate" styleId="dateRangeWeekToDate"/>
						<label for="dateRangeWeekToDate"><spring:message code="label.range.date.week.to.date"/></label>
					</li>
					<li>
						<html:radio property="dateRangeSelector" value="monthToDate" styleId="dateRangeMonthToDate"/>
						<label for="dateRangeMonthToDate"><spring:message code="label.range.date.month.to.date"/></label>
					</li>
					<li>
						<html:radio property="dateRangeSelector" value="yearToDate" styleId="dateRangeYearToDate"/>
						<label for="dateRangeYearToDate"><spring:message code="label.range.date.year.to.date"/></label>
					</li>
					<li>
						<html:radio property="dateRangeSelector" value="specific" styleId="dateRangeSpecific"/>
						<label for="dateRangeSpecific"><spring:message code="label.range.date.specific"/></label>
					</li>
				</ul>
			</td>
			<td class="dateInputs">
				<spring:message code="label.date.start"/>
				<html:text property="startDate" styleClass="buildNumber"/>
				<html:messages property="startDate" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
			<td class="dateInputs">
				<spring:message code="label.date.end"/>
				<html:text property="endDate" styleClass="buildNumber"/>
				<html:messages property="endDate" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<tr>
			<td>
				<html:radio property="rangeType" value="index" styleId="rangeByIndex"/>
				<label for="rangeByIndex"><spring:message code="label.range.by.index"/></label>
			</td>
			<td>
				<spring:message code="label.index.start"/>
				<html:text property="minBuildNumber" styleClass="buildNumber"/>
				<html:messages property="startIndex" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
			<td>
				<spring:message code="label.index.end"/>
				<html:text property="maxBuildNumber" styleClass="buildNumber"/>
				<html:messages property="endIndex" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<tr>
			<td colspan="3">
				<html:radio property="rangeType" value="all" styleId="rangeAll"/>
				<label for="rangeAll"><spring:message code="label.range.include.all"/></label>
			</td>
		</tr>
		<tr>
			<td><spring:message code="label.projects"/></td>
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
			<td><spring:message code="label.status.types"/></td>
			<td colspan="3">
				<ul class="metaDataOptions">
					<li>
						<html:multibox property="statusTypes" value="PASS" styleId="statPass"/>
						<label for="statPass">PASS</label>
					</li>
					<li>
						<html:multibox property="statusTypes" value="FAIL" styleId="statFail"/>
						<label for="statFail">FAIL</label>
					</li>
					<li>
						<html:multibox property="statusTypes" value="SKIP" styleId="statSkip"/>
						<label for="statSkip">SKIP</label>
					</li>
					<li>
						<html:multibox property="statusTypes" value="ERROR" styleId="statError"/>
						<label for="statError">ERROR</label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><spring:message code="label.update.method"/></td>
			<td colspan="3">
				<ul class="metaDataOptions">
					<li>
						<html:radio property="updateType" value="" styleId="allUpdates"/>
						<label for="allUpdates"><spring:message code="label.update.type.any"/></label>
					</li>
					<li>
						<html:radio property="updateType" value="Full" styleId="fullUpdates"/>
						<label for="fullUpdates"><spring:message code="label.update.type.full"/></label>
					</li>
					<li>
						<html:radio property="updateType" value="Incremental" styleId="incrUpdates"/>
						<label for="incrUpdates"><spring:message code="label.update.type.incremental"/></label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><spring:message code="label.build.user"/></td>
			<td colspan="3">
				<html:select property="requestedBy">
					<html:option key="select.empty" value=""/>
					
					<html:option key="select.build.schedulers" value="_s" disabled="true"/>
					
					<c:forEach items="${buildOutcomeStore.buildSchedulers}" var="item">
						<html:option value="${item}">${item}</html:option>
					</c:forEach>

					<html:option key="select.build.users" value="_u" disabled="true"/>
					
					<c:forEach items="${buildOutcomeStore.buildUsers}" var="item">
						<html:option value="${item}">${item}</html:option>
					</c:forEach>
				</html:select>
			</td>
		</tr>
		<tr>
			<td><spring:message code="label.transform"/></td>
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
							<spring:message code="label.report.in.browser"/>
						</label>
					</li>
					<li>
						<html:radio property="download" value="true" styleId="download_t"/>
						<label for="download_t">
							<spring:message code="label.report.download"/>
						</label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td class="buttons" colspan="4">
				<v:button name="action" value="submit"><spring:message code="button.submit"/></v:button>
			</td>
		</tr>
	</tbody>
</table>
</html:form>
</v:bubble>

</body>
</html>
