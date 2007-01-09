<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://jakarta.apache.org/struts/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

	<head>
		<jsp:element name="script">
			<jsp:attribute name="type">text/javascript</jsp:attribute>
			<jsp:attribute name="src"><c:url value="/javascript/projectConfiguration.js"/></jsp:attribute>
			<jsp:body/>
		</jsp:element>
	</head>
<html:xhtml/>

<body>

<v:bubble>
<html:form action="/admin/setup/manageProjectConfig" method="post">
<table class="projectConfig">
	<caption><fmt:message key="captions.project.config"/></caption>
	<tbody>
	<tr>
		<td><fmt:message key="label.project.name"/></td>
		<td colspan="2">
			<html:text property="config.name" styleId="txtProjectName"/>
			<html:messages property="config.name" id="msg">
				<span class="error">${msg}</span>
			</html:messages>
		</td>
	</tr>
	<tr>
		<td><fmt:message key="label.working.copy.directory"/></td>
		<td colspan="2">
			<html:text property="config.workDir" styleId="txtWorkDir"/>
			<input type="hidden" name="defaultWorkDirPattern" id="defaultWorkDirPattern"
				value="${stateManager.store.workingCopyLocationPattern}"/>
			<html:messages property="config.workDir" id="msg">
				<span class="error">${msg}</span>
			</html:messages>
		</td>
	</tr>
	<tr>
		<jsp:element name="td">
			<jsp:attribute name="title"><fmt:message key="label.site.path.description"/></jsp:attribute>
			<jsp:body><fmt:message key="label.site.path"/></jsp:body>
		</jsp:element>
		<td colspan="2">
			<html:text property="config.sitePath"/>
			<html:messages property="config.sitePath" id="msg">
				<span class="error">${msg}</span>
			</html:messages>
		</td>
	</tr>
	<tr>
		<jsp:element name="td">
			<jsp:attribute name="title"><fmt:message key="label.bugtraq.url.description"/></jsp:attribute>
			<jsp:body><fmt:message key="label.bugtraq.url"/></jsp:body>
		</jsp:element>
		<td colspan="2">
			<html:text property="config.bugtraqUrl"/>
			<html:messages property="config.bugtraqUrl" id="msg">
				<span class="error">${msg}</span>
			</html:messages>
		</td>
	</tr>
	<tr>
		<jsp:element name="td">
			<jsp:attribute name="title"><fmt:message key="label.bugtraq.regex1.description"/></jsp:attribute>
			<jsp:body><fmt:message key="label.bugtraq.regex1"/></jsp:body>
		</jsp:element>
		<td colspan="2">
			<html:text property="config.bugtraqLogRegex1"/>
			<html:messages property="config.bugtraqLogRegex1" id="msg">
				<span class="error">${msg}</span>
			</html:messages>
		</td>
	</tr>
	<tr>
		<jsp:element name="td">
			<jsp:attribute name="title"><fmt:message key="label.bugtraq.regex2.description"/></jsp:attribute>
			<jsp:body><fmt:message key="label.bugtraq.regex2"/></jsp:body>
		</jsp:element>
		<td colspan="2">
			<html:text property="config.bugtraqLogRegex2"/>
			<html:messages property="config.bugtraqLogRegex2" id="msg">
				<span class="error">${msg}</span>
			</html:messages>
		</td>
	</tr>
	<tr>
		<td><fmt:message key="label.repository.adaptor"/></td>
		<td>
			<html:select property="config.repositoryAdaptorPluginId">
				<html:option value=""><fmt:message key="label.none"/></html:option>
				<c:forEach items="${stateManager.pluginManager.repositoryPlugins}" var="plugin">
					<html:option value="${plugin.id}">${plugin.name}</html:option>
				</c:forEach>
			</html:select>
			<html:submit property="action" value="Configure"
				onclick="drillDown(this, 'config.repositoryAdaptorPluginId')"/>
			<html:messages property="config.repositoryAdaptorPluginId" id="msg">
				<span class="error">${msg}</span>
			</html:messages>
		</td>
	</tr>
	<tr>
		<td><fmt:message key="label.build.tool"/></td>
		<td>
			<html:select property="config.buildToolPluginId">
				<html:option value=""><fmt:message key="label.none"/></html:option>
				<c:forEach items="${stateManager.pluginManager.buildToolPlugins}" var="plugin">
					<html:option value="${plugin.id}">${plugin.name}</html:option>
				</c:forEach>
			</html:select>
			<html:submit property="action" value="Configure"
				onclick="drillDown(this, 'config.buildToolPluginId')"/>
			<html:messages property="config.buildToolPluginId" id="msg">
				<span class="error">${msg}</span>
			</html:messages>
		</td>
	</tr>
	<tr>
		<td><fmt:message key="label.dependencies"/></td>
		<td colspan="2">
			<div class="projectCheckboxes">
				<ul>
				<c:forEach items="${stateManager.projectConfigNames}" var="projectName">
					<c:if test="${projectConfigForm.config.name != projectName}">
						<li>
							<html:multibox property="config.dependencies" value="${projectName}"
								styleId="dep${projectName}"/>
							<jsp:element name="label">
								<jsp:attribute name="for">dep${projectName}</jsp:attribute>
								<jsp:body>${projectName}</jsp:body>
							</jsp:element>
						</li>
					</c:if>
				</c:forEach>
				</ul>
			</div>
		</td>
	</tr>
	<tr>
		<td><fmt:message key="label.build.options"/></td>
		<td colspan="2">
			<ul class="projectBuildOptions">
				<li>
					<html:checkbox property="config.autoIncludeDependencies" styleId="autoDeps"/>
					<label for="autoDeps"><fmt:message key="label.build.options.auto.include.dependencies"/></label>
				</li>
				<li>
					<html:checkbox property="config.buildOnDependencyFailure" styleId="buildOnDepFail"/>
					<label for="buildOnDepFail"><fmt:message key="label.build.options.build.on.dependency.failure"/></label>
				</li>
				<li>
					<html:checkbox property="config.suppressErrors" styleId="suppressErrors"/>
					<label for="suppressErrors"><fmt:message key="label.build.options.errors.suppress"/></label>
				</li>
				<li>
					<html:checkbox property="config.suppressWarnings" styleId="suppressWarnings"/>
					<label for="suppressWarnings"><fmt:message key="label.build.options.warnings.suppress"/></label>
				</li>
			</ul>
		</td>
	</tr>
	<tr>
		<td><fmt:message key="label.update.strategy"/></td>
		<td colspan="2">
			<ul class="projectBuildOptions">
				<li>
					<html:radio property="updateStrategy" value="CleanAlways" styleId="updateStrategyCleanAlways"/>
					<label for="updateStrategyCleanAlways"><fmt:message key="label.update.strategy.clean.always"/></label>
				</li>
				<li>
					<html:radio property="updateStrategy" value="CleanDaily" styleId="updateStrategyCleanDaily"/>
					<label for="updateStrategyCleanDaily"><fmt:message key="label.update.strategy.clean.daily"/></label>
				</li>
				<li>
					<html:radio property="updateStrategy" value="IncrementalAlways" styleId="updateStrategyIncrementalAlways"/>
					<label for="updateStrategyIncrementalAlways"><fmt:message key="label.update.strategy.incremental.always"/></label>
				</li>
			</ul>
		</td>
	</tr>
	<tr>
		<td><fmt:message key="label.project.build.scheduler"/></td>
		<td colspan="2">
			<div class="projectCheckboxes">
				<ul>
				<c:forEach items="${stateManager.config.schedulers}" var="scheduler">
					<li>
						<html:multibox property="config.schedulerNames" value="${scheduler.name}"
							styleId="sched${scheduler.name}"/>
						<jsp:element name="label">
							<jsp:attribute name="for">sched${scheduler.name}</jsp:attribute>
							<jsp:body>${scheduler.name}</jsp:body>
						</jsp:element>
					</li>
				</c:forEach>
				</ul>
			</div>
		</td>
	</tr>
	<tr>
		<td class="buttons" colspan="3">
			<c:choose>
				<c:when test="${projectConfigForm.createNew}">
					<html:submit property="action" value="Create"/>
				</c:when>
				<c:otherwise>
					<html:submit property="action" value="Update"/>
					<html:submit property="action" value="Copy"/>
					<html:submit property="action" value="Delete"/>
				</c:otherwise>
			</c:choose>
			<html:hidden property="createNew"/>
			<html:hidden property="commit" value="true"/>
			<html:hidden property="focus"/>
		</td>
	</tr>
	</tbody>
</table>
</html:form>
</v:bubble>

</body>
</html>
