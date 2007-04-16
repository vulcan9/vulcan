<?xml version="1.0" encoding="UTF-8" ?>

<div class="detail-menu"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">
	
<jsp:directive.page session="false"/>
<jsp:output omit-xml-declaration="true"/>

<html:xhtml/>

<v:bubble styleClass="setup">
<div>
	<span class="caption"><fmt:message key="label.setup.menu"/></span>
	
	<ul class="detail-menu">
	<c:if test="${stateManager.running}">
		<li>
			<fmt:message key="link.setup.project"/>
			<ul>
				<li><html:link forward="importProjectConfig"><fmt:message key="link.import.project"/></html:link></li>
				<li><html:link forward="createProjectConfig"><fmt:message key="label.project.new"/></html:link></li>
				<li><html:link forward="deleteProjects"><fmt:message key="label.delete.projects"/></html:link></li>
				<li><fmt:message key="label.projects"/>
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
			<fmt:message key="link.setup.scheduler"/>
			<ul>
				<li><html:link forward="createSchedulerConfig"><fmt:message key="label.scheduler.new"/></html:link></li>
				<li><html:link forward="createBuildDaemonConfig"><fmt:message key="label.build.daemon.new"/></html:link></li>
				<li>
					<fmt:message key="label.schedulers"/>
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
					<fmt:message key="label.build.daemons"/>
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
			<fmt:message key="label.setup.plugins.build"/>
			<ul>
				<c:forEach items="${stateManager.pluginManager.buildToolPlugins}" var="plugin">
					<li><html:link forward="configurePlugin" paramId="pluginId" paramName="plugin" paramProperty="id">${plugin.name}</html:link></li>
				</c:forEach>
			</ul>
		</li>
		<li>
			<fmt:message key="label.setup.plugins.repository"/>
			<ul>
				<c:forEach items="${stateManager.pluginManager.repositoryPlugins}" var="plugin">
					<li><html:link forward="configurePlugin" paramId="pluginId" paramName="plugin" paramProperty="id">${plugin.name}</html:link></li>
				</c:forEach>
			</ul>
		</li>
		<li><fmt:message key="label.setup.plugins.publisher"/>
			<ul>
				<c:forEach items="${stateManager.pluginManager.observerPlugins}" var="plugin">
					<li><html:link forward="configurePlugin" paramId="pluginId" paramName="plugin" paramProperty="id">${plugin.name}</html:link></li>
				</c:forEach>
			</ul>
		</li>
		
		<li><html:link forward="importPlugin"><fmt:message key="link.import.plugin"/></html:link></li>
	</c:if>
		<li><html:link forward="importConfig"><fmt:message key="link.import.config"/></html:link></li>
		<li><html:link forward="viewConfig"><fmt:message key="link.view.config"/></html:link></li>
		<li><html:link forward="about"><fmt:message key="link.about"/></html:link></li>
	</ul>
</div>
</v:bubble>
</div>