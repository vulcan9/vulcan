<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<jsp:directive.page session="false"/>

<html:xhtml/>

<head>
	<meta name="helpTopic" content="UserPreferences"/>
</head>

<body>

<v:bubble styleClass="preferences">
	<html:form action="/managePreferences" method="post">
		<table>
			<caption><spring:message code="captions.preferences"/></caption>
			<tbody>
				<tr>
					<th><spring:message code="th.external.resources"/></th>
					<td>
						<ul class="metaDataOptions">
							<li>
								<html:radio property="config.popupMode"
									value="modeSame" styleId="modeSame"/>
								<label for="modeSame">
									<spring:message code="label.same.window"/>
								</label>
							</li>
							<li>
								<html:radio property="config.popupMode"
									value="modeNew" styleId="modeNew"/>
								<label for="modeNew">
									<spring:message code="label.new.window"/>
								</label>
							</li>
							<li>
								<html:radio property="config.popupMode"
									value="modePopup" styleId="modePopup"/>
								<label for="modePopup">
									<spring:message code="label.new.window.single"/>
								</label>
							</li>
						</ul>
					</td>
				</tr>
				<tr>
					<th>
						<spring:message code="label.dashboard.options"/>
					</th>
					<td>
						<ul class="metaDataOptions">
							<li>
								<html:checkbox property="config.groupByLabel"
									styleId="chkGroupByLabel"/>
								<label for="chkGroupByLabel"><spring:message code="label.group.by.label"/></label>
							</li>
							<li>
								<html:checkbox property="config.showBuildDaemons"
									styleId="chkShowBuildDaemons"/>
								<label for="chkShowBuildDaemons"><spring:message code="label.show.build.daemons"/></label>
							</li>
							<li>
								<html:checkbox property="config.showBuildQueue"
									styleId="chkShowBuildQueue"/>
								<label for="chkShowBuildQueue"><spring:message code="label.show.build.queue"/></label>
							</li>
							<li>
								<html:checkbox property="config.showSchedulers"
									styleId="chkShowSchedulers"/>
								<label for="chkShowSchedulers"><spring:message code="label.show.schedulers"/></label>
							</li>
						</ul>
						<spring:message code="label.dashboard.refresh.interval"/>
						<html:select property="config.reloadInterval" styleClass="reloadInterval">
							<html:option value="0"><spring:message code="label.never"/></html:option>
							<html:option value="5">5 <spring:message code="time.seconds"/></html:option>
							<html:option value="30">30 <spring:message code="time.seconds"/></html:option>
							<html:option value="60">1 <spring:message code="time.minute"/></html:option>
							<html:option value="300">5 <spring:message code="time.minutes"/></html:option>
						</html:select>
					</td>
				</tr>
				<tr>
					<th>
						<spring:message code="th.style.sheet"/>
					</th>
					<td>
						<html:select property="config.stylesheet">
							<html:options property="availableStylesheets"/>
						</html:select>
					</td>
				</tr>
				<tr>
					<th>
						<spring:message code="label.project.labels"/>
					</th>
					<td>
						<div class="projectCheckboxes">
							<ul>
								<c:forEach items="${stateManager.projectLabels}" var="label">
									<li>
										<html:multibox property="config.labels" value="${label}" styleId="lbl${v:mangle(label)}"/>
										<label for="lbl${v:mangle(label)}"><c:out value="${label}" escapeXml="true"/></label>
									</li>
								</c:forEach>
							</ul>
						</div>
					</td>
				</tr>
				<tr>
					<td colspan="2">
						<button type="submit" name="action" value="save"><spring:message code="button.save"/></button>
						<input type="hidden" name="fullReset" value="true"/>
						<html:hidden property="config.sortColumn"/>
						<html:hidden property="config.sortOrder"/>
					</td>
				</tr>
			</tbody>
		</table>
	</html:form>
</v:bubble>

</body>
</html>
