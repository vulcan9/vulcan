<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:html="http://struts.apache.org/tags-html">

<html:xhtml/>

<body>
	<span class="error"><spring:message code="messages.plugin.locked"/></span>
	<br/>
	<spring:message code="messages.plugin.locked.info"/>
	<ul>
		<li><html:link href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4950148">4950148</html:link></li>
		<li><html:link href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4171239">4171239</html:link></li>
	</ul>
</body>
</html>
