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
	<v:messages/>
	
	<v:bubble styleClass="">
		<html:form action="/buildmanagement/manageLocks" method="post">
			<table>
				<caption><spring:message code="captions.manage.project.locks"/></caption>
				<tbody>
					<tr>
						<td>
							<spring:message code="label.projects"/>
						</td>
						<td>
							<v:projectCheckboxes disableLockedProjects="false"/>
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
							<v:button name="action" value="unlock"><spring:message code="button.unlock.projects"/></v:button>
							<v:button name="action" value="clear"><spring:message code="button.lock.projects.remove"/></v:button>
						</td>
					</tr>
				</tbody>
			</table>
		</html:form>
	</v:bubble>
</body>
</html>
