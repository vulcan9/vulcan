<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://jakarta.apache.org/struts/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<html:xhtml/>

<body>

<c:choose>
	<c:when test="${schedulerConfigForm.daemon}">
		<c:set var="action" value="/admin/setup/manageBuildDaemonConfig"/>
	</c:when>
	<c:otherwise>
		<c:set var="action" value="/admin/setup/manageSchedulerConfig"/>
	</c:otherwise>
</c:choose>

<v:bubble>
<html:form action="${action}" method="post">
<table class="schedulerConfig">
	<caption><fmt:message key="captions.scheduler.config"/></caption>
	<tbody>
		<tr>
			<td class="label"><fmt:message key="label.scheduler.name"/></td>
			<td colspan="2">
				<html:text property="config.name"/>
				<html:messages property="config.name" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<c:choose>
			<c:when test="${schedulerConfigForm.daemon}">
				<tr>
					<td class="label"><fmt:message key="label.scheduler.interval"/></td>
					<td>
						<html:text property="intervalScalar"/>
						<html:messages property="intervalScalar" id="msg">
							<span class="error">${msg}</span>
						</html:messages>
					</td>
					<td>
						<html:select property="intervalMultiplier">
							<c:choose>
								<c:when test="${schedulerConfigForm.daemon}">
									<html:option value="0"><fmt:message key="label.scheduler.disabled"/></html:option>
								</c:when>
								<c:otherwise>
									<html:option value="0"><fmt:message key="label.scheduler.use.cron"/></html:option>
								</c:otherwise>									
							</c:choose>
							<html:option value="1000"><fmt:message key="time.seconds"/></html:option>
							<html:option value="60000"><fmt:message key="time.minutes"/></html:option>
							<html:option value="3600000"><fmt:message key="time.hours"/></html:option>
							<html:option value="86400000"><fmt:message key="time.days"/></html:option>
						</html:select>
					</td>
				</tr>
				<tr>
					<td class="label"><fmt:message key="label.scheduler.timeout"/></td>
					<td>
						<html:text property="timeoutScalar"/>
						<html:messages property="timeoutScalar" id="msg">
							<span class="error">${msg}</span>
						</html:messages>
					</td>
					<td>
						<html:select property="timeoutMultiplier">
							<html:option value="0"><fmt:message key="label.scheduler.disabled"/></html:option>
							<html:option value="1000"><fmt:message key="time.seconds"/></html:option>
							<html:option value="60000"><fmt:message key="time.minutes"/></html:option>
							<html:option value="3600000"><fmt:message key="time.hours"/></html:option>
							<html:option value="86400000"><fmt:message key="time.days"/></html:option>
						</html:select>
					</td>
				</tr>
			</c:when>
			<c:otherwise>
				<tr>
					<td class="label"><fmt:message key="label.scheduler.cron.expression"/></td>
					<td colspan="2">
						<span class="description"><fmt:message key="label.scheduler.cron.description"/></span>
						<br/><br/>
						<html:hidden property="intervalMultiplier" value="0"/>
						<html:text property="config.cronExpression"/>
						<html:messages property="config.cronExpression" id="msg">
							<span class="error">${msg}</span>
						</html:messages>
					</td>
				</tr>
			</c:otherwise>
		</c:choose>

		<tr>
			<td class="buttons" colspan="3">
				<c:choose>
					<c:when test="${schedulerConfigForm.createNew}">
						<html:submit property="action" value="Create"/>
					</c:when>
					<c:otherwise>
						<html:submit property="action" value="Update"/>
						<html:submit property="action" value="Delete"/>
					</c:otherwise>
				</c:choose>
				<html:hidden property="daemon"/>
				<html:hidden property="dirty" styleId="pendingChanges"/>
			</td>
		</tr>
		</tbody>
</table>
</html:form>
</v:bubble>
</body>
</html>
