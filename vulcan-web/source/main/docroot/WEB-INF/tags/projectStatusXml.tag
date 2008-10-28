<jsp:root version="2.0"
		xmlns:jsp="http://java.sun.com/JSP/Page"
		xmlns:c="http://java.sun.com/jsp/jstl/core"
		xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
		xmlns:fn="http://java.sun.com/jsp/jstl/functions"
		xmlns:spring="http://www.springframework.org/tags"
		xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags"
		xmlns:x="http://java.sun.com/jsp/jstl/xml">

	<jsp:directive.tag display-name="projectStatusXml" dynamic-attributes="false"/>
	<jsp:directive.attribute name="transform" required="false" type="java.lang.Boolean" rtexprvalue="true"/>
	<jsp:directive.attribute name="caption" required="false" type="java.lang.Object" rtexprvalue="true"/>
	<jsp:directive.attribute name="labels" required="false" type="java.lang.Object" rtexprvalue="true"/>
	
	<jsp:useBean id="now" class="java.util.Date" scope="page"/>
	<spring:message code="build.timestamp.format" var="timestampPattern"/>	

	<c:set var="buildingProjects" value="${stateManager.buildManager.projectsBeingBuilt}"/>
	<c:set var="projects" value="${v:getProjectNamesByLabels(labels)}"/>
	<c:set var="projectStatus" value="${stateManager.buildManager.projectStatus}"/>
	
	<c:set var="statusXml">
		<projects>
			<available-labels>
				<c:forEach items="${stateManager.projectLabels}" var="label">
					<label><c:out value="${label}"/></label>
				</c:forEach>
			</available-labels>
			<selected-labels>
				<c:forEach items="${labels}" var="label">
					<label><c:out value="${label}"/></label>
				</c:forEach>
			</selected-labels>
						
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
									<c:set var="remainingTime" value="${status.estimatedBuildTimeMillis - (now.time - status.startDate.time)}"/>
									<c:if test="${remainingTime lt 0}">
										<c:set var="remainingTime" value="0"/>
									</c:if>
									<jsp:element name="estimated-build-time">
										<jsp:attribute name="millis">
											${status.estimatedBuildTimeMillis}
										</jsp:attribute>
										<jsp:attribute name="remaining-millis">
											${remainingTime}
										</jsp:attribute>
										<jsp:body>
											<v:formatElapsedTime value="${status.estimatedBuildTimeMillis}" verbosity="2"/>
										</jsp:body>
									</jsp:element>
									<c:if test="${! empty status.errors}">
										<errors-present/>
									</c:if>
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
								<locked message="${v:getProjectLockMessage(projectName)}">
									<c:out value="${v:isProjectLocked(projectName)}"/>
								</locked>
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
	</c:set>
	
	<c:choose>
		<c:when test="${transform}">
			<c:import url="/xsl/projects.xsl" var="xslt"/>
			<x:transform xslt="${xslt}" doc="${statusXml}">
				<x:param name="contextRoot">
					<c:url value="/" var="contextRoot"/>
					${fn:substringBefore(contextRoot, ';')}
				</x:param>
				<x:param name="caption">
					<spring:message code="captions.projects.status"/>
					<c:if test="${caption ne null}">
						<c:out value=" - ${caption}"/>
					</c:if>
				</x:param>
				<x:param name="sortUrl">
					<c:url value="/managePreferences.do?action=save"/>
				</x:param>
				<x:param name="detailLink">
					<c:url value="/viewProjectStatus.do?transform=xhtml&amp;projectName="/>
				</x:param>
				<x:param name="nameHeader">
					<spring:message code="th.project.name"/>
				</x:param>
				<x:param name="buildNumberHeader">
					<spring:message code="th.build.number"/>
				</x:param>
				<x:param name="ageHeader">
					<spring:message code="th.age"/>
				</x:param>
				<x:param name="tagHeader">
					<spring:message code="th.tagName"/>
				</x:param>
				<x:param name="revisionHeader">
					<spring:message code="th.revision"/>
				</x:param>
				<x:param name="statusHeader">
					<spring:message code="th.project.status"/>
				</x:param>
				<x:param name="timestampLabel">
					<spring:message code="label.build.timestamp"/>
				</x:param>
				<x:param name="sortSelect">${preferences.sortColumn}</x:param>
				<x:param name="sortOrder">${preferences.sortOrder}</x:param>
			</x:transform>
		</c:when>
		<c:otherwise>
			<c:out value="${statusXml}" escapeXml="false"/>
		</c:otherwise>
	</c:choose>
</jsp:root>