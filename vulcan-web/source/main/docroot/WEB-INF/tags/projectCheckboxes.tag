<?xml version="1.0" encoding="utf-8"?>
<jsp:root version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
    xmlns:html="http://struts.apache.org/tags-html"
    xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">
    
    <jsp:directive.tag display-name="projectCheckboxes" dynamic-attributes="false"/>
	<jsp:directive.attribute name="property" required="false" type="java.lang.String" rtexprvalue="false"/>
	<jsp:directive.attribute name="disableLockedProjects" required="false" type="java.lang.Boolean" rtexprvalue="false"/>
	
	<html:xhtml/>
	
	<c:if test="${property eq null}">
		<c:set var="property" value="projectNames"/>
	</c:if>
	
	<div class="projectCheckboxes">
		<ul>
			<c:forEach items="${stateManager.projectConfigNames}" var="projectName">
				<li>
					<html:multibox property="${property}" value="${projectName}"
						styleId="target_${v:mangle(projectName)}" disabled="${disableLockedProjects and v:isProjectLocked(projectName)}"/>
					<c:set var="labelClass" value=""/>
					<c:if test="${v:isProjectLocked(projectName)}">
						<c:set var="labelClass" value="locked"/>
					</c:if>
					<jsp:element name="label" class="${labelClass}">
						<jsp:attribute name="for">target_${v:mangle(projectName)}</jsp:attribute>
						<jsp:body>${projectName}</jsp:body>
					</jsp:element>
					<c:if test="${v:isProjectLocked(projectName)}">
						<c:set var="title">
							<c:out value="${v:getProjectLockMessage(projectName)}" escapeXml="true"/>
						</c:set>
						<html:img page="/images/lock.png" alt="Locked" title="${title}"/>
					</c:if>
				</li>
			</c:forEach>
		</ul>
	</div>
</jsp:root>
