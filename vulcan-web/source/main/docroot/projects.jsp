<projects
		xmlns:jsp="http://java.sun.com/JSP/Page"
		xmlns:c="http://java.sun.com/jsp/jstl/core"
		xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
		xmlns:spring="http://www.springframework.org/tags">

	<jsp:directive.page contentType="application/xml" session="false"/>
	<jsp:output omit-xml-declaration="false"/>
	
	<jsp:useBean id="now" class="java.util.Date" scope="page"/>
	<fmt:message key="build.timestamp.format" var="timestampPattern"/>	

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
								<!-- WLS 91 can't handle the number 31449600000 -->
								<c:set var="yearMillis" value="${604800000 * 52}"/>
								<jsp:element name="timestamp">
									<jsp:attribute name="millis">${status.completionDate.time}</jsp:attribute>
									<jsp:attribute name="age">
										<c:choose>
											<c:when test="${elapsedTime lt 60000}">
												1 <fmt:message key="time.minutes"/>
											</c:when>
											<c:when test="${elapsedTime lt 3600000}">
												<fmt:formatNumber maxFractionDigits="0" value="${elapsedTime / 60000}"/>${' '}<fmt:message key="time.minutes"/>
											</c:when>
											<c:when test="${elapsedTime lt 86400000}">
												<fmt:formatNumber maxFractionDigits="0" value="${elapsedTime / 3600000}"/>${' '}<fmt:message key="time.hours"/>
											</c:when>
											<c:when test="${elapsedTime lt 604800000}">
												<fmt:formatNumber maxFractionDigits="0" value="${elapsedTime / 86400000}"/>${' '}<fmt:message key="time.days"/>
											</c:when>
											<c:when test="${elapsedTime lt yearMillis}">
												<fmt:formatNumber maxFractionDigits="0" value="${elapsedTime / 604800000}"/>${' '}<fmt:message key="time.weeks"/>
											</c:when>
											<c:otherwise>
												<fmt:formatNumber maxFractionDigits="0" value="${elapsedTime / yearMillis}"/>${' '}<fmt:message key="time.years"/>
											</c:otherwise>
										</c:choose>
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