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
	<meta name="helpTopic" content="DeleteProjects"/>
</head>

<body>

<v:bubble styleClass="">
<html:form action="/admin/setup/deleteProjects" method="post">
<table class="projectConfig">
	<caption><spring:message code="captions.delete.projects"/></caption>
	<tbody>
		<tr>
			<td>
				<spring:message code="label.projects"/>
			</td>
			<td>
				<div class="projectCheckboxes">
					<ul>
						<c:forEach items="${stateManager.projectConfigNames}" var="projectName">
							<li>
								<html:multibox property="projectNames" value="${projectName}"
									styleId="proj${v:mangle(projectName)}"/>
								<jsp:element name="label">
									<jsp:attribute name="for">proj${v:mangle(projectName)}</jsp:attribute>
									<jsp:body>${projectName}</jsp:body>
								</jsp:element>
							</li>
						</c:forEach>
					</ul>
				</div>
			</td>
		</tr>
		<c:if test="${not empty projectsWithDependents}">
			<tr>
				<td colspan="2">
					<span class="warning"><spring:message code="messages.dependent.projects.pre"/></span>
					<ul>
						<c:forEach items="${projectsWithDependents}" var="projectName">
							<li>${projectName}</li>
						</c:forEach>
					</ul>
					<span><spring:message code="messages.dependent.proejcts.mid"/></span>
					<ul>
						<c:forEach items="${dependentProjects}" var="projectName">
							<li>
								${projectName}
								<html:hidden property="projectNames" value="${projectName}"/>
							</li>
						</c:forEach>
					</ul>
					<span><spring:message code="messages.dependent.projects.will.be.deleted"/></span>
				</td>
			</tr>
		</c:if>
		<tr>
			<td class="buttons" colspan="2">
				<button><spring:message code="label.delete.projects"/></button>
			</td>
		</tr>
	</tbody>
</table>
</html:form>
</v:bubble>

<v:messages/>

</body>
</html>
