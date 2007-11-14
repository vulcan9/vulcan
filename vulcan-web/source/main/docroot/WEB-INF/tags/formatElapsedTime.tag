<?xml version="1.0" encoding="utf-8"?>
<jsp:root version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:html="http://struts.apache.org/tags-html"
    xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">
    
    <jsp:directive.tag display-name="formatElapsedTime" dynamic-attributes="false"/>
    
	<jsp:directive.attribute name="value" required="true" type="java.lang.Long" rtexprvalue="true"/>
	<jsp:directive.attribute name="verbosity" required="false" type="java.lang.Long" rtexprvalue="true"/>
	
	${v:formatElapsedTime(pageContext, value, verbosity)}
</jsp:root>
