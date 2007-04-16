<?xml version="1.0" encoding="UTF-8" ?>

<rest-response
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://struts.apache.org/tags-html">

	<jsp:directive.page contentType="application/xml" session="false"/>
	
	<html:messages message="true" id="msg">
		<message>${msg}</message>
	</html:messages>

	<html:messages name="warnings" id="msg">
		<warning>${msg}</warning>
	</html:messages>

	<html:messages name="org.apache.struts.action.ERROR" id="msg">
		<error>${msg}</error>
	</html:messages>
	
	<html:messages property="org.apache.struts.action.GLOBAL_MESSAGE" id="msg">
		<error>${msg}</error>
	</html:messages>

</rest-response>