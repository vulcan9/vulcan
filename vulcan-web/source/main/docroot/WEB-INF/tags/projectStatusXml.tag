<jsp:root version="2.0"
		xmlns:jsp="http://java.sun.com/JSP/Page"
		xmlns:c="http://java.sun.com/jsp/jstl/core"
		xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
		xmlns:fn="http://java.sun.com/jsp/jstl/functions"
		xmlns:spring="http://www.springframework.org/tags"
		xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

	<jsp:directive.tag display-name="projectStatusXml" dynamic-attributes="false"/>
	<jsp:directive.attribute name="labels" required="false" type="java.lang.String[]" rtexprvalue="true"/>
	
	<jsp:useBean id="now" class="java.util.Date" scope="page"/>
	<spring:message code="build.timestamp.format" var="timestampPattern"/>	

	<c:set var="buildingProjects" value="${stateManager.buildManager.projectsBeingBuilt}"/>
	<c:set var="projects" value="${v:getProjectNamesByLabels(labels)}"/>
	<c:set var="projectStatus" value="${stateManager.buildManager.projectStatus}"/>
	
	<projects>
		<c:forEach items="${projects}" var="projectName">
			<jsp:element name="project">
				<jsp:attribute name="name">${projectName}</jsp:attribute>
				<jsp:body>
					<name>${projectName}</name>
					<c:set var="status" value="${projectStatus[projectName]}"/>
					<c:if test="${buildingProjects[projectName] != null}">
						<c:set var="status" value="${buildingProjects[projectName]}"/>
					</c:if>
					<c:choose>
						<c:when test="${status != null}">
							<status>${status.status}</status>
							<c:if test="${status.status eq 'BUILDING' and projectStatus[projectName] ne null}">
								<previous-status>${projectStatus[projectName].status}</previous-status>
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
</jsp:root>