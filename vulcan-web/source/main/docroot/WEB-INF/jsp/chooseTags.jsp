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

<body>

<v:bubble styleClass="manualBuild">
<html:form action="/buildmanagement/manualBuild" method="post">
<table class="manualBuild">
	<caption><fmt:message key="captions.choose.tags"/></caption>
	<tbody>
		<tr>
			<td colspan="2">
				<span class="description"><fmt:message key="label.choose.tags.description"/></span>
				<html:hidden property="chooseTags" value="true"/>
			</td>
		</tr>
		<c:forEach items="${manualBuildForm.projectNames}" var="projectName" varStatus="loopStatus">
			<c:set var="projectErrorPresent" value="false"/>
			<tr>
				<td>${projectName}</td>
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
		</c:forEach>
		<tr>
			<td class="buttons" colspan="2">
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
