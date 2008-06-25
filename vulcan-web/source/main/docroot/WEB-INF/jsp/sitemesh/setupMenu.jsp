<?xml version="1.0" encoding="UTF-8" ?>

<div class="detail-menu"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">
	
<jsp:directive.page session="false"/>
<jsp:output omit-xml-declaration="true"/>

<html:xhtml/>

<v:bubble styleClass="setup">
<div>
	<span class="caption"><spring:message code="label.setup.menu"/></span>
	
	<ul class="detail-menu">
	<c:if test="${stateManager.running}">
		<li>
			<spring:message code="link.setup.project"/>
			<ul>
				<li><html:link forward="importProjectConfig"><spring:message code="link.import.project"/></html:link></li>
				<li><html:link forward="createProjectConfig"><spring:message code="label.project.new"/></html:link></li>
				<li><html:link forward="deleteProjects"><spring:message code="label.delete.projects"/></html:link></li>
				<li><html:link forward="editBuildReports"><spring:message code="label.setup.build.reports"/></html:link></li>
				<li><spring:message code="label.projects"/>
					<ul>
						<c:forEach items="${stateManager.config.projects}" var="project">
							<li>
								<html:link forward="configureProject" paramId="config.name"
										paramName="project" paramProperty="name">${project.name}</html:link>
							</li>
						</c:forEach>
					</ul>
				</li>
			</ul>
		</li>
		<li>
			<spring:message code="label.project.labels"/>
			<ul>
				<li><html:link forward="createProjectLabel"><spring:message code="label.project.label.new"/></html:link></li>
				<li><spring:message code="label.project.labels"/>
					<ul>
						<c:forEach items="${stateManager.projectLabels}" var="label">
							<li>
								<html:link forward="createProjectLabel" paramId="name" paramName="label"><c:out value="${label}" escapeXml="true"/></html:link>
							</li>
						</c:forEach>
					</ul>
				</li>
			</ul>
		</li>
		<li>
			<spring:message code="link.setup.scheduler"/>
			<ul>
				<li><html:link forward="createSchedulerConfig"><spring:message code="label.scheduler.new"/></html:link></li>
				<li><html:link forward="createBuildDaemonConfig"><spring:message code="label.build.daemon.new"/></html:link></li>
				<li>
					<spring:message code="label.schedulers"/>
					<ul>
						<c:forEach items="${stateManager.config.schedulers}" var="scheduler">
							<li>
								<html:link forward="configureScheduler" paramId="config.name"
									paramName="scheduler" paramProperty="name">${scheduler.name}</html:link>
							</li>
						</c:forEach>
					</ul>
				</li>
				<li>
					<spring:message code="label.build.daemons"/>
					<ul>
						<c:forEach items="${stateManager.config.buildDaemons}" var="daemon">
							<li>
								<html:link forward="configureBuildDaemon" paramId="config.name"
									paramName="daemon" paramProperty="name">${daemon.name}</html:link>
							</li>
						</c:forEach>
					</ul>
				</li>
			</ul>
		</li>
		
		<li>
			<spring:message code="label.setup.plugins.build"/>
			<ul>
				<c:forEach items="${stateManager.pluginManager.buildToolPlugins}" var="plugin">
					<li><html:link forward="configurePlugin" paramId="pluginId" paramName="plugin" paramProperty="id">${plugin.name}</html:link></li>
				</c:forEach>
			</ul>
		</li>
		<li>
			<spring:message code="label.setup.plugins.repository"/>
			<ul>
				<c:forEach items="${stateManager.pluginManager.repositoryPlugins}" var="plugin">
					<li><html:link forward="configurePlugin" paramId="pluginId" paramName="plugin" paramProperty="id">${plugin.name}</html:link></li>
				</c:forEach>
			</ul>
		</li>
		<li><spring:message code="label.setup.plugins.publisher"/>
			<ul>
				<c:forEach items="${stateManager.pluginManager.observerPlugins}" var="plugin">
					<li><html:link forward="configurePlugin" paramId="pluginId" paramName="plugin" paramProperty="id">${plugin.name}</html:link></li>
				</c:forEach>
			</ul>
		</li>
		
		<li><html:link forward="importPlugin"><spring:message code="link.import.plugin"/></html:link></li>
	</c:if>
		<li><html:link forward="importConfig"><spring:message code="link.import.config"/></html:link></li>
		<li><html:link forward="viewConfig"><spring:message code="link.view.config"/></html:link></li>
	</ul>
</div>
</v:bubble>
</div>