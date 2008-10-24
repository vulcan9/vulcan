<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<html:xhtml/>

<head>
	<meta name="helpTopic" content="ManualBuild"/>
</head>

<body>

<v:messages/>

<v:bubble styleClass="manualBuild">
<html:form action="/buildmanagement/manualBuild" method="post">
<table class="manualBuild">
	<caption><spring:message code="captions.build.manual"/></caption>
	<tbody>
		<tr>
			<td><spring:message code="label.tag.options"/></td>
			<td>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="chooseTags" value="false" styleId="tagDefault"/>
						<label for="tagDefault"><spring:message code="label.tag.default"/></label>
					</li>
					<li>
						<html:radio property="chooseTags" value="true" styleId="tagChoose"/>
						<label for="tagChoose"><spring:message code="label.tag.choose"/></label>
					</li>
				</ul>
			</td>
		</tr>
		
		<tr>
			<td><spring:message code="label.update.strategy"/></td>
			<td>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="updateStrategy" value="Default" styleId="updateStrategyDefault"/>
						<label for="updateStrategyDefault"><spring:message code="label.update.strategy.default"/></label>
					</li>
					<li>
						<html:radio property="updateStrategy" value="Full" styleId="updateStrategyFull"/>
						<label for="updateStrategyFull"><spring:message code="label.update.strategy.manual.full"/></label>
					</li>
					<li>
						<html:radio property="updateStrategy" value="Incremental" styleId="updateStrategyIncremental"/>
						<label for="updateStrategyIncremental"><spring:message code="label.update.strategy.manual.incremental"/></label>
					</li>
				</ul>
			</td>
		</tr>
		
		<tr>
			<td><spring:message code="label.projects"/></td>
			<td>
				<v:projectCheckboxes property="targets" disableLockedProjects="true"/>
				<html:messages property="targets" id="msg">
					<span class="error">${msg}</span>
				</html:messages>
			</td>
		</tr>
		<tr>
			<td><spring:message code="label.build.manual.options"/></td>
			<td>
				<ul class="metaDataOptions">
					<li>
						<html:checkbox property="buildOnNoUpdates" styleId="noUpdates"/>
						<label for="noUpdates"><spring:message code="label.build.when.no.updates"/></label>
					</li>
					<li>
						<html:checkbox property="buildOnDependencyFailure" styleId="failedDeps"/>
						<label for="failedDeps"><spring:message code="label.build.when.deps.fail"/></label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td><spring:message code="label.build.manual.dependency.options"/></td>
			<td>
				<span class="description"><spring:message code="label.build.manual.dependency.options.description"/></span>
				<ul class="metaDataOptions">
					<li>
						<html:radio property="dependencies" value="NONE" styleId="depNONE"/>
						<label for="depNONE"><spring:message code="label.build.deps.none"/></label>
					</li>
					<li>
						<html:radio property="dependencies" value="AS_NEEDED" styleId="depAS_NEEDED"/>
						<label for="depAS_NEEDED"><spring:message code="label.build.deps.as.needed"/></label>
					</li>
					<li>
						<html:radio property="dependencies" value="FORCE" styleId="depFORCE"/>
						<label for="depFORCE" title="not implemented"><spring:message code="label.build.deps.force"/></label>
					</li>
				</ul>
			</td>
		</tr>
		<tr>
			<td class="buttons" colspan="2">
				<v:button name="action" value="build"><spring:message code="button.build"/></v:button>
			</td>
		</tr>
	</tbody>
</table>
</html:form>
</v:bubble>

</body>
</html>
