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

<body>
	<v:bubble styleClass="warning">
		<span class="warning">
			<fmt:message key="label.import.config.warning"/>
		</span>
	</v:bubble>
	
	<html:form action="/admin/importConfiguration" method="post" enctype="multipart/form-data">
		<v:bubble>
			<span class="caption"><fmt:message key="label.import.config"/></span>
			<div class="file-upload">
				<html:file property="configFile"/>
				<html:submit property="action" value="Upload"/>
				<br/>
				<html:messages property="configFile" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</div>
		</v:bubble>
	</html:form>
</body>
</html>
