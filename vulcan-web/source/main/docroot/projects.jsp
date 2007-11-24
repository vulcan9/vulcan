<projects
		xmlns:jsp="http://java.sun.com/JSP/Page"
		xmlns:c="http://java.sun.com/jsp/jstl/core"
		xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
		xmlns:spring="http://www.springframework.org/tags"
		xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

	<jsp:directive.page contentType="application/xml" session="false"/>
	<jsp:output omit-xml-declaration="false"/>
	
	<jsp:useBean id="now" class="java.util.Date" scope="page"/>
	<spring:message code="build.timestamp.format" var="timestampPattern"/>	

	<c:set var="buildingProjects" value="${stateManager.buildManager.projectsBeingBuilt}"/>
	<c:set var="projects" value="${stateManager.config.projects}"/>
	<c:set var="projectStatus" value="${stateManager.buildManager.projectStatus}"/>
	
	<c:forEach items="${projects}" var="project">
		<jsp:element name="project">
			<jsp:attribute name="name">${project.name}</jsp:attribute>
			<jsp:body>
				<c:set var="status" value="${projectStatus[project.name]}"/>
				<c:if test="${buildingProjects[project.name] != null}">
					<c:set var="status" value="${buildingProjects[project.name]}"/>
				</c:if>
				<c:choose>
					<c:when test="${status != null}">
						<status>${status.status}</status>
						<c:if test="${status.status eq 'BUILDING' and projectStatus[project.name] ne null}">
							<previous-status>${projectStatus[project.name].status}</previous-status>
						</c:if>
						<c:choose>
							<c:when test="${status.completionDate ne null}">
								<c:set var="elapsedTime" value="${now.time - status.completionDate.time}"/>
								<jsp:element name="timestamp">
									<jsp:attribute name="millis">${status.completionDate.time}</jsp:attribute>
									<jsp:attribute name="age">
										<v:formatElapsedTime value="${elapsedTime}" verbosity="1"/> 
									</jsp:attribute>
									<jsp:body>
										<fmt:formatDate value="${status.completionDate}" pattern="${timestampPattern}"/>
									</jsp:body>
								</jsp:element>
							</c:when>
							<c:otherwise>
								<timestamp millis="${now.time}"/>
							</c:otherwise>
						</c:choose>
						<c:if test="${status.lastGoodBuildNumber != null and status.lastGoodBuildNumber != status.buildNumber}">
							<c:set var="firstFailure" value="${v:getOutcomeByBuildNumber(pageContext, status.name, status.lastGoodBuildNumber + 1)}"/>
							<c:set var="elapsedTime" value="${now.time - firstFailure.completionDate.time}"/>
							<first-failure>
								<build-number><c:out value="${firstFailure.buildNumber}"/></build-number>
								<jsp:element name="elapsed-time">
									<jsp:attribute name="millis">${elapsedTime}</jsp:attribute>
									<jsp:attribute name="age">
										<v:formatElapsedTime value="${elapsedTime}" verbosity="2"/> 
									</jsp:attribute>
								</jsp:element>
							</first-failure>
						</c:if>
						<c:if test="${status.messageKey != null}">
							<message><spring:message code="${status.messageKey}" arguments="${status.messageArgs}" htmlEscape="true"/></message>
						</c:if>
						<build-number><c:out value="${status.buildNumber}"/></build-number>
						<c:if test="${status.revision != null}">
							<jsp:element name="revision">
								<jsp:attribute name="numeric"><c:out value="${status.revision.revision}"/></jsp:attribute>
								<jsp:body><c:out value="${status.revision}"/></jsp:body>
							</jsp:element>
						</c:if>
						<c:if test="${status.tagName != null}">
							<repository-tag-name><c:out value="${status.tagName}"/></repository-tag-name>
						</c:if>
					</c:when>
					<c:otherwise>
						<status>Not Built</status>
					</c:otherwise>
				</c:choose>
			</jsp:body>
		</jsp:element>
	</c:forEach>
</projects>