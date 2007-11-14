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

<v:bubble styleClass="manualBuild">
<html:form action="/buildmanagement/manualBuild" method="post">
<table class="manualBuild">
	<caption><spring:message code="captions.choose.tags"/></caption>
	<tbody>
		<tr>
			<td colspan="3">
				<span class="description"><spring:message code="label.choose.tags.description"/></span>
				<html:hidden property="chooseTags" value="true"/>
			</td>
		</tr>
		<c:forEach items="${manualBuildForm.projectNames}" var="projectName" varStatus="loopStatus">
			<c:set var="projectErrorPresent" value="false"/>
			<tr>
				<td rowspan="2"><strong>${projectName}</strong></td>
				<td>Tag</td>
				<td>
					<html:messages property="${projectName}" id="msg">
						<c:set var="projectErrorPresent" value="true"/>
						<c:set var="errorPresent" value="true"/>
						<span class="error">${msg}</span>
					</html:messages>
					
					<c:if test="${not projectErrorPresent}">
						<html:hidden property="targets" value="${projectName}"/>
						<html:select property="selectedTags">
							<c:forEach items="${manualBuildForm.availableTags[loopStatus.index]}" var="tag">
								<c:choose>
									<c:when test="${manualBuildForm.selectedTags[loopStatus.index] eq tag.name}">
										<option selected="selected" value="${tag.name}">${tag.description}</option>
									</c:when>
									<c:otherwise>
										<option value="${tag.name}">${tag.description}</option>
									</c:otherwise>
								</c:choose>
							</c:forEach>
						</html:select>
					</c:if>
				</td>
			</tr>
			<tr>
				<td><spring:message code="label.working.copy.directory"/></td>
				<td>
					<html:text property="workDirOverrides" value="${manualBuildForm.workDirOverrides[loopStatus.index]}"/>
				</td>
			</tr>
		</c:forEach>
		<tr>
			<td class="buttons" colspan="3">
				<c:if test="${not errorPresent}">
					<html:submit property="action" value="Build"/>
				</c:if>
			</td>
		</tr>
	</tbody>
</table>
</html:form>
</v:bubble>
</body>
</html>
