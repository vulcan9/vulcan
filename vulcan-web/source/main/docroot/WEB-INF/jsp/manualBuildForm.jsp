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

<head>
	<meta name="helpTopic" id="helpTopic" content="ManualBuild"/>
</head>

<body>

<v:bubble styleClass="manualBuild">
<html:form action="/buildmanagement/manualBuild" method="post">
<table class="manualBuild">
	<caption><fmt:message key="captions.build.manual"/></caption>
	<tbody>
		<tr>
			<td><fmt:message key="label.tag.options"/></td>
			<td>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="chooseTags" value="false" styleId="tagDefault"/>
						<label for="tagDefault"><fmt:message key="label.tag.default"/></label>
					</li>
					<li>
						<html:radio property="chooseTags" value="true" styleId="tagChoose"/>
						<label for="tagChoose"><fmt:message key="label.tag.choose"/></label>
					</li>
				</ul>
			</td>
		</tr>
		
		<tr>
			<td><fmt:message key="label.update.strategy"/></td>
			<td>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="updateStrategy" value="Default" styleId="updateStrategyDefault"/>
						<label for="updateStrategyDefault"><fmt:message key="label.update.strategy.default"/></label>
					</li>
					<li>
						<html:radio property="updateStrategy" value="Full" styleId="updateStrategyFull"/>
						<label for="updateStrategyFull"><fmt:message key="label.update.strategy.manual.full"/></label>
					</li>
					<li>
						<html:radio property="updateStrategy" value="Incremental" styleId="updateStrategyIncremental"/>
						<label for="updateStrategyIncremental"><fmt:message key="label.update.strategy.manual.incremental"/></label>
					</li>
				</ul>
			</td>
		</tr>
		
		<tr>
			<td><fmt:message key="label.projects"/></td>
			<td>
				<div class="projectCheckboxes">
					<ul>
					<c:forEach items="${stateManager.projectConfigNames}" var="projectName">
						<li>
							<html:multibox property="targets" value="${projectName}"
								styleId="target${projectName}"/>
							<jsp:element name="label">
								<jsp:attribute name="for">target${projectName}</jsp:attribute>
								<jsp:body>${projectName}</jsp:body>
							</jsp:element>
						</li>
					</c:forEach>
					</ul>
				</div>
				<html:messages property="targets" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<tr>
			<td><fmt:message key="label.build.manual.options"/></td>
			<td>
				<ul class="metaDataOptions">
					<li>
						<html:checkbox property="buildOnNoUpdates" styleId="noUpdates"/>
						<label for="noUpdates"><fmt:message key="label.build.when.no.updates"/></label>
					</li>
					<li>
						<html:checkbox property="buildOnDependencyFailure" styleId="failedDeps"/>
						<label for="failedDeps"><fmt:message key="label.build.when.deps.fail"/></label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><fmt:message key="label.build.manual.dependency.options"/></td>
			<td>
				<span class="description"><fmt:message key="label.build.manual.dependency.options.description"/></span>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="dependencies" value="NONE" styleId="depNONE"/>
						<label for="depNONE"><fmt:message key="label.build.deps.none"/></label>
					</li>
					<li>
						<html:radio property="dependencies" value="AS_NEEDED" styleId="depAS_NEEDED"/>
						<label for="depAS_NEEDED"><fmt:message key="label.build.deps.as.needed"/></label>
					</li>
					<li>
						<html:radio property="dependencies" value="FORCE" styleId="depFORCE"/>
						<label for="depFORCE" title="not implemented"><fmt:message key="label.build.deps.force"/></label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td class="buttons" colspan="2">
				<html:submit property="action" value="Build"/>
			</td>
		</tr>
	</tbody>
</table>
</html:form>
</v:bubble>

</body>
</html>
