<?xml version="1.0" encoding="utf-8"?>
<jsp:root version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:c="http://java.sun.com/jsp/jstl/core">
    
    <jsp:directive.tag display-name="openFlashChart" dynamic-attributes="false"/>

	<jsp:directive.attribute name="name" required="true" type="java.lang.String" rtexprvalue="false"/>
	<jsp:directive.attribute name="dataUrl" required="true" type="java.lang.String" rtexprvalue="true"/>
	<jsp:directive.attribute name="width" required="true" type="java.lang.String" rtexprvalue="false"/>
	<jsp:directive.attribute name="height" required="true" type="java.lang.String" rtexprvalue="false"/>
	
	<c:url var="contextDataUrl" value="${dataUrl}"/>
	<c:url var="flashUrl" value="/open-flash-chart.swf"/>
	
	<object
		classid="clsid:d27cdb6e-ae6d-11cf-96b8-444553540000"
		codebase="http://fpdownload.macromedia.com/pub/shockwave/cabs/flash/swflash.cab#version=8,0,0,0" 
		width="${width}" height="${height}" id="${name}object">
		
		<param name="allowScriptAccess" value="sameDomain" />
		<param name="movie" value="${flashUrl}?data=${contextDataUrl}" />
		<param name="quality" value="high" />
		<param name="bgcolor" value="#FFFFFF" />
		
		<embed src="${flashUrl}?data=${contextDataUrl}"
			quality="high" bgcolor="#FFFFFF" width="${width}" height="${height}"
			id="${name}" allowScriptAccess="sameDomain"
			type="application/x-shockwave-flash"
			pluginspage="http://www.macromedia.com/go/getflashplayer" />
	</object>
</jsp:root>
