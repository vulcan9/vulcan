<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:x="http://java.sun.com/jsp/jstl/xml"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://jakarta.apache.org/struts/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<jsp:directive.page session="false"/>

<html:xhtml/>

<body>

<v:bubble styleClass="about">
<table class="about">
	<caption><fmt:message key="captions.versions"/></caption>
	<thead>
		<tr>
			<th><fmt:message key="th.component.name"/></th>
			<th><fmt:message key="th.component.type"/></th>
			<th><fmt:message key="th.component.version"/></th>
		</tr>
	</thead>
	<tbody>
	<c:set var="components" value="${stateManager.componentVersions}"/>
	<c:forEach items="${components}" var="comp">
		<tr>
			<td title="${comp.id}">${comp.name}</td>
			<td>${comp.type}</td>
			<td class="component-version">${comp.version}</td>
		</tr>
	</c:forEach>
	</tbody>
</table>
</v:bubble>
</body>
</html>
