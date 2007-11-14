<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<jsp:directive.page session="false"/>

<html:xhtml/>

<head>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/preferences.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
</head>

<body>

<v:bubble styleClass="preferences">
	<form name="preferencesForm" id="preferencesForm" action="#" method="post">
		<table>
			<caption><spring:message code="captions.preferences"/></caption>
			<tbody>
				<tr>
					<th><spring:message code="th.external.resources"/></th>
					<td>
						<ul class="metaDataOptions">
							<li>
								<input type="radio" name="windowMode"
									value="modeSame" id="modeSame"/>
								<label for="modeSame">
									<spring:message code="label.same.window"/>
								</label>
							</li>
							<li>
								<input type="radio" name="windowMode"
									value="modeNew" id="modeNew"/>
								<label for="modeNew">
									<spring:message code="label.new.window"/>
								</label>
							</li>
							<li>
								<input type="radio" name="windowMode"
									value="modePopup" id="modePopup"/>
								<label for="modePopup">
									<spring:message code="label.new.window.single"/>
								</label>
							</li>
						</ul>
					</td>
				</tr>
				<tr>
					<th>
						Dashboard
					</th>
					<td>
						<ul class="metaDataOptions">
							<li>
								<input type="checkbox" name="showDaemons" id="showDaemons" checked="checked"/>
								<label for="showDaemons">Show Build Daemons</label>
							</li>
							<li>
								<input type="checkbox" name="showQueue" id="showQueue" checked="checked"/>
								<label for="showQueue">Show Build Queue</label>
							</li>
							<li>
								<input type="checkbox" name="showSchedulers" id="showSchedulers" checked="checked"/>
								<label for="showSchedulers">Show Schedulers</label>
							</li>
						</ul>
					</td>
				</tr>
				<tr>
					<th>
						Hide Projects
					</th>
					<td>
					</td>
				</tr>
				<tr>
					<th>
						Select Columns
					</th>
					<td>
					</td>
				</tr>
				<tr>
					<td colspan="2">
						<button id="prefOkButton"><spring:message code="button.save"/></button>
						<button id="prefCancelButton"><spring:message code="button.cancel"/></button>
					</td>
				</tr>
			</tbody>
		</table>
	</form>
</v:bubble>

</body>
</html>
