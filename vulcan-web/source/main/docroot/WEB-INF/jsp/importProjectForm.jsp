<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:x="http://java.sun.com/jsp/jstl/xml"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://jakarta.apache.org/struts/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<html:xhtml/>

<head>
	<meta name="helpTopic" content="ImportProject"/>
</head>

<body>

<v:bubble styleClass="">
<html:form action="/admin/setup/createProjectFromUrl" method="post">
<table>
	<caption><fmt:message key="captions.import.project"/></caption>
	<tbody>
		<tr>
			<td>
				<fmt:message key="label.project.url"/>
			</td>
			<td>
				<html:text property="url"/>
				<html:messages property="url" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<tr>
			<td>
				<fmt:message key="label.child.projects"/>
			</td>
			<td>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="createSubprojects" value="false" styleId="singleProject"/>
						<label for="singleProject"><fmt:message key="label.child.projects.single"/></label>
					</li>
					<li>
						<html:radio property="createSubprojects" value="true" styleId="multiProject"/>
						<label for="multiProject"><fmt:message key="label.child.projects.multi"/></label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td>
				<fmt:message key="label.name.collision.resolution"/>
			</td>
			<td>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="nameCollisionResolutionMode" value="Abort" styleId="nameCollisionResolutionModeAbort"/>
						<label for="nameCollisionResolutionModeAbort"><fmt:message key="label.name.collision.resolution.abort"/></label>
					</li>
					<li>
						<html:radio property="nameCollisionResolutionMode" value="UseExisting" styleId="nameCollisionResolutionModeUseExisting"/>
						<label for="nameCollisionResolutionModeUseExisting"><fmt:message key="label.name.collision.resolution.use.existing"/></label>
					</li>
					<li>
						<html:radio property="nameCollisionResolutionMode" value="Overwrite" styleId="nameCollisionResolutionModeOverwrite"/>
						<label for="nameCollisionResolutionModeOverwrite"><fmt:message key="label.name.collision.resolution.overwrite"/></label>
					</li>
				</ul>
				<html:messages property="nameCollisionResolutionMode" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<tr>
			<td><fmt:message key="label.project.build.scheduler"/></td>
			<td colspan="2">
				<div class="projectCheckboxes">
					<ul>
					<c:forEach items="${stateManager.config.schedulers}" var="scheduler">
						<li>
							<html:multibox property="schedulerNames" value="${scheduler.name}"
								styleId="sched${scheduler.name}"/>
							<jsp:element name="label">
								<jsp:attribute name="for">sched${scheduler.name}</jsp:attribute>
								<jsp:body>${scheduler.name}</jsp:body>
							</jsp:element>
						</li>
					</c:forEach>
					</ul>
				</div>
			</td>
		</tr>
		<tr>
			<td class="buttons" colspan="2">
				<html:submit value="Import"/>
			</td>
		</tr>
	</tbody>
</table>
</html:form>
</v:bubble>

<v:messages/>

</body>
</html>
