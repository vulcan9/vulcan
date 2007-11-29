<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

	<head>
		<meta name="helpTopic" content="ProjectLabels"/>
	</head>
<html:xhtml/>

<body>

<v:bubble>
<html:form action="/admin/setup/manageLabels" method="post">
<table class="projectConfig">
	<caption><spring:message code="captions.project.label"/></caption>
	<tbody>
		<tr>
			<td>
				<spring:message code="label.project.label"/>
			</td>
			<td>
				<html:text property="name"/>
			</td>
		</tr>
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
		<tr>
			<td class="buttons" colspan="2">
				<button name="action" value="save"><spring:message code="button.save"/></button>
			</td>
		</tr>
	</tbody>
</table>
</html:form>
</v:bubble>

</body>
</html>
