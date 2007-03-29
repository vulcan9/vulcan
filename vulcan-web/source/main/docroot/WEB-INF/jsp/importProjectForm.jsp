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
	<meta name="helpTopic" id="helpTopic" content="ImportProject"/>
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
