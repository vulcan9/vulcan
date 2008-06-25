<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<html:xhtml/>

<body>
	<div id="lightbox">
		<div class="foo">
			Choose a project:
			<ul>
				<c:forEach items="${stateManager.config.projects}" var="project">
					<li>
						<html:link forward="configureProject" paramId="config.name"
								paramName="project" paramProperty="name">${project.name}</html:link>
					</li>
				</c:forEach>
			</ul>
		</div>
	</div>

	<v:messages/>
	
	<spring:message code="messages.choose.setup.option"/>
	
	<h2 class="setup-group">Main</h2>
	
	<ul class="setup-option-list">
		<li>
			<html:link forward="importProjectConfig" styleClass="system-settings">
				<spring:message code="link.system.settings"/>
			</html:link>
		</li>
		<li>
			<html:link forward="importProjectConfig" styleClass="java-homes">
				<spring:message code="link.java.homes"/>
			</html:link>
		</li>
		<li>
			<html:link page="/admin/setup/reports.jsp" styleClass="reports">
				<spring:message code="link.setup.reports"/>
			</html:link>
		</li>
	</ul>	
	
	<h2 class="setup-group">Projects</h2>
	
	<ul class="setup-option-list">
		<li>
			<html:link forward="importProjectConfig" styleClass="import">
				<spring:message code="link.import.project"/>
			</html:link>
		</li>
		<li>
			<html:link forward="createProjectConfig" styleClass="add">
				<spring:message code="label.project.new"/>
			</html:link>
		</li>
		<li>
			<html:link forward="createProjectConfig" styleClass="edit select-project">
				<spring:message code="label.project.edit"/>
			</html:link>
		</li>
		<li>
			<html:link forward="createProjectLabel" styleClass="group">
				<spring:message code="label.project.labels"/>
			</html:link>
		</li>
		<li>
			<html:link forward="deleteProjects" styleClass="remove">
				<spring:message code="label.delete.projects"/>
			</html:link>
		</li>
	</ul>
	
	<h2 class="setup-group">Build Schedules</h2>
	
	<ul class="setup-option-list">
		<li>
			Add schedule
		</li>
		<li>
			Edit schedule
		</li>
		<li>
			Remove schedule
		</li>
		<li>
			Add builder
		</li>
		<li>
			Edit builder
		</li>
		<li>
			Remove builder
		</li>
	</ul>

	<h2 class="setup-group">Plugin Settings</h2>
	
	<ul class="setup-option-list">
		<li>
			Add schedule
		</li>
		<li>
			Edit schedule
		</li>
		<li>
			Remove schedule
		</li>
		<li>
			Add builder
		</li>
		<li>
			Edit builder
		</li>
		<li>
			Remove builder
		</li>
	</ul>
	
	<div style="clear:both"/>
</body>
</html>
