<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:x="http://java.sun.com/jsp/jstl/xml"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<html:xhtml/>

<head>
	<meta name="helpTopic" content="ImportProject"/>
</head>

<body>

<v:bubble styleClass="">
<html:form action="/admin/setup/createProjectFromUrl" method="post">
<table>
	<caption><spring:message code="captions.import.project"/></caption>
	<tbody>
		<tr>
			<td>
				<spring:message code="label.project.url"/>
			</td>
			<td>
				<html:text property="url"/>
				<html:messages property="url" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<c:if test="${projectImportForm.authenticationRequired}">
		<tr>
			<td>
				<spring:message code="label.username"/>
			</td>
			<td>
				<html:text property="username"/>
			</td>
		</tr>
		<tr>
			<td>
				<spring:message code="label.password"/>
			</td>
			<td>
				<html:password property="password" value=""/>
			</td>
		</tr>
		</c:if>
		<tr>
			<td>
				<spring:message code="label.child.projects"/>
			</td>
			<td>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="createSubprojects" value="false" styleId="singleProject"/>
						<label for="singleProject"><spring:message code="label.child.projects.single"/></label>
					</li>
					<li>
						<html:radio property="createSubprojects" value="true" styleId="multiProject"/>
						<label for="multiProject"><spring:message code="label.child.projects.multi"/></label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td>
				<spring:message code="label.name.collision.resolution"/>
			</td>
			<td>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="nameCollisionResolutionMode" value="Abort" styleId="nameCollisionResolutionModeAbort"/>
						<label for="nameCollisionResolutionModeAbort"><spring:message code="label.name.collision.resolution.abort"/></label>
					</li>
					<li>
						<html:radio property="nameCollisionResolutionMode" value="UseExisting" styleId="nameCollisionResolutionModeUseExisting"/>
						<label for="nameCollisionResolutionModeUseExisting"><spring:message code="label.name.collision.resolution.use.existing"/></label>
					</li>
					<li>
						<html:radio property="nameCollisionResolutionMode" value="Overwrite" styleId="nameCollisionResolutionModeOverwrite"/>
						<label for="nameCollisionResolutionModeOverwrite"><spring:message code="label.name.collision.resolution.overwrite"/></label>
					</li>
				</ul>
				<html:messages property="nameCollisionResolutionMode" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<tr>
			<td><spring:message code="label.project.build.scheduler"/></td>
			<td>
				<div class="projectCheckboxes">
					<ul>
					<c:forEach items="${stateManager.config.schedulers}" var="scheduler">
						<li>
							<html:multibox property="schedulerNames" value="${scheduler.name}"
								styleId="sched${v:mangle(scheduler.name)}"/>
							<jsp:element name="label">
								<jsp:attribute name="for">sched${v:mangle(scheduler.name)}</jsp:attribute>
								<jsp:body>${scheduler.name}</jsp:body>
							</jsp:element>
						</li>
					</c:forEach>
					</ul>
				</div>
			</td>
		</tr>
		<tr>
			<td><spring:message code="label.project.labels"/></td>
			<td>
				<div class="projectCheckboxes">
					<ul>
					<c:forEach items="${stateManager.projectLabels}" var="label">
						<li>
							<html:multibox property="labels" value="${label}"
								styleId="sched${v:mangle(label)}"/>
							<jsp:element name="label">
								<jsp:attribute name="for">sched${v:mangle(label)}</jsp:attribute>
								<jsp:body>${label}</jsp:body>
							</jsp:element>
						</li>
					</c:forEach>
					<li>
						<spring:message code="label.new"/>
						<html:text property="newLabel" styleClass="new-label"/>
					</li>
					</ul>
				</div>
			</td>
		</tr>
		<tr>
			<td class="buttons" colspan="2">
				<html:submit value="Import"/>
				<html:hidden property="authenticationRequired"/>
			</td>
		</tr>
	</tbody>
</table>
</html:form>
</v:bubble>

<v:messages/>

</body>
</html>
