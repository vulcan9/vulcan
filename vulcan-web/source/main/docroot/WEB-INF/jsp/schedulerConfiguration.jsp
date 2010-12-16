<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
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
	<caption><spring:message code="captions.scheduler.config"/></caption>
	<tbody>
		<tr>
			<td class="label"><spring:message code="label.scheduler.name"/></td>
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
					<td class="label"><spring:message code="label.scheduler.interval"/></td>
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
									<html:option value="0"><spring:message code="label.scheduler.disabled"/></html:option>
								</c:when>
								<c:otherwise>
									<html:option value="0"><spring:message code="label.scheduler.use.cron"/></html:option>
								</c:otherwise>									
							</c:choose>
							<html:option value="1000"><spring:message code="time.seconds"/></html:option>
							<html:option value="60000"><spring:message code="time.minutes"/></html:option>
							<html:option value="3600000"><spring:message code="time.hours"/></html:option>
							<html:option value="86400000"><spring:message code="time.days"/></html:option>
						</html:select>
					</td>
				</tr>
				<tr>
					<td class="label"><spring:message code="label.scheduler.timeout"/></td>
					<td>
						<html:text property="timeoutScalar"/>
						<html:messages property="timeoutScalar" id="msg">
							<span class="error">${msg}</span>
						</html:messages>
					</td>
					<td>
						<html:select property="timeoutMultiplier">
							<html:option value="0"><spring:message code="label.scheduler.disabled"/></html:option>
							<html:option value="1000"><spring:message code="time.seconds"/></html:option>
							<html:option value="60000"><spring:message code="time.minutes"/></html:option>
							<html:option value="3600000"><spring:message code="time.hours"/></html:option>
							<html:option value="86400000"><spring:message code="time.days"/></html:option>
						</html:select>
					</td>
				</tr>
			</c:when>
			<c:otherwise>
				<tr>
					<td class="label"><spring:message code="label.scheduler.cron.expression"/></td>
					<td colspan="2">
						<span class="description"><spring:message code="label.scheduler.cron.description"/></span>
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
						<v:button name="action" value="create"><spring:message code="button.create"/></v:button>
					</c:when>
					<c:otherwise>
						<v:button name="action" value="update"><spring:message code="button.update"/></v:button>
						<v:button name="action" value="delete"><spring:message code="button.delete"/></v:button>
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
