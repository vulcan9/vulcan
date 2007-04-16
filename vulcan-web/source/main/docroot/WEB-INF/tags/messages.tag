<?xml version="1.0" encoding="utf-8"?>
<jsp:root version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:html="http://struts.apache.org/tags-html"
    xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">
    
    <jsp:directive.tag display-name="messages" dynamic-attributes="false"/>

	<html:messages message="true" id="msg">
		<v:bubble styleClass="message"><span class="message">${msg}</span></v:bubble>
	</html:messages>

	<html:messages name="warnings" id="msg">
		<v:bubble styleClass="warning"><span class="warning">${msg}</span></v:bubble>
	</html:messages>

	<html:messages property="org.apache.struts.action.GLOBAL_MESSAGE" id="msg">
		<v:bubble styleClass="error"><span class="error">${msg}</span></v:bubble>
	</html:messages>

</jsp:root>
