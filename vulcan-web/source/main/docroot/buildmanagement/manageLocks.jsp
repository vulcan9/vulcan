<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<html:xhtml/>

<body>
	<v:messages/>
	
	<html:form action="/buildmanagement/manageLocks" method="post">
		<div>
			<table>
				<caption><spring:message code="captions.manage.project.locks"/></caption>
				<tbody>
					<tr>
						<td>
							<spring:message code="label.projects"/>
						</td>
						<td>
							<v:projectCheckboxes disableLockedProjects="false"/>
							<html:messages property="projectNames" id="msg">
								<span class="error">${msg}</span>
							</html:messages>
						</td>
					</tr>
					<tr>
						<td>
							<spring:message code="label.lock.message"/>
						</td>
						<td>
							<html:text property="message"/>
						</td>
					</tr>
					<tr>
						<td class="buttons" colspan="2">
							<v:button name="action" value="lock"><spring:message code="button.lock.projects"/></v:button>
							<v:button name="action" value="clear"><spring:message code="button.lock.projects.remove"/></v:button>
						</td>
					</tr>
				</tbody>
			</table>
			
			<c:if test="${not empty stateManager.projectLocks}">
				<spring:message code="build.timestamp.format" var="timestampPattern"/>
				<table>
					<caption><spring:message code="captions.remove.project.locks"/></caption>
					<thead>
						<tr>
							<th>Date</th>
							<th>Comment</th>
							<th></th>
						</tr>
					</thead>
					<tbody>
						<c:forEach items="${stateManager.projectLocks}" var="lock">
							<tr>
								<td><fmt:formatDate value="${lock.date}" pattern="${timestampPattern}"/></td>
								<td><c:out value="${lock.message}" escapeXml="true"/></td>
								<td><input type="checkbox" name="lockId" value="${lock.id}"/></td>
							</tr>
						</c:forEach>
						<tr>
							<td colspan="3">
								<v:button name="action" value="unlock"><spring:message code="button.unlock.projects"/></v:button>
							</td>
						</tr>
					</tbody>
				</table>
			</c:if>
		</div>
	</html:form>
</body>
</html>
