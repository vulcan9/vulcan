<?xml version="1.0" encoding="UTF-8" ?>

<rest-response
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

	<jsp:directive.page contentType="application/xml"/>
	<c:set var="keys" value="${v:getActionErrorPropertyList(pageContext.request)}"/>
	
	<c:if test="${not empty keys}">
		<c:if test="${restResponseCode eq null}">
			<c:set var="restResponseCode" value="400"/>
		</c:if>
		${v:setStatus(pageContext.response, restResponseCode)}
	</c:if>
	
	<c:forEach var="prop" items="${keys}">
		<html:messages id="msg" property="${prop}">
			<error request-parameter="${prop}">${msg}</error>
		</html:messages>
	</c:forEach>

	<!-- response for ManageLocksAction -->
	<c:if test="${lockId ne null}">
		<lock-id>${lockId}</lock-id>
	</c:if>

	<!-- response for ManualBuildAction -->
	<c:if test="${not empty manualBuildForm.projectNames}">
		<available-tags>
			<project>
				<c:forEach items="${manualBuildForm.projectNames}" var="projectName" varStatus="loopStatus">
					<project-name>${projectName}</project-name>
					<most-recently-used-tag>${manualBuildForm.selectedTags[loopStatus.index]}</most-recently-used-tag>
					<default-build-directory>${manualBuildForm.workDirOverrides[loopStatus.index]}</default-build-directory>
					<tags>
						<c:forEach items="${manualBuildForm.availableTags[loopStatus.index]}" var="tag">
							<tag description="${tag.description}">${tag.name}</tag>
						</c:forEach>
					</tags>
				</c:forEach>
			</project>
		</available-tags>
	</c:if>
</rest-response>