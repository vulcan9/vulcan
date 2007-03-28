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
<form action="/vulcan/admin/setup/createProjectFromUrl.do" method="post">
<table>
	<caption><fmt:message key="captions.import.project"/></caption>
	<tbody>
		<tr>
			<td>
				<fmt:message key="label.project.url"/>
			</td>
			<td>
				<input type="text" name="url"/>
			</td>
		</tr>
		<tr>
			<td class="buttons" colspan="2">
				<input type="submit" value="Import"/>
			</td>
		</tr>
	</tbody>
</table>
</form>
</v:bubble>

<v:messages/>

</body>
</html>
