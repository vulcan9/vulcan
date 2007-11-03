<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:x="http://java.sun.com/jsp/jstl/xml"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">
<html:xhtml/>
<head>
	<script type="text/javascript">
		function showBuildDetails(url) {
			launchWindowHandler(null, url, "modePopup", "buildDetails");
		}
	</script>
</head>
<body>
	<div style="float: left; margin-right: 5em;">
		Projects:
		<c:forEach items="${reportForm.projectNames}" var="name" varStatus="loop">
			<c:if test="${loop.index eq 0}">
				<c:out value="${name}"/>
			</c:if>
			<c:if test="${loop.index gt 0}">
				<c:out value=",${name}"/>
			</c:if>
		</c:forEach>
		<br/>
		Range: ${fromLabel} to ${toLabel}
		<!-- <br/>
		<span style="font-family: Verdana; font-size: 18pt; color: #7E97A6;">Verdana</span>
		<br/>
		<span style="font-family: Calibri; font-size: 18pt; color: #7E97A6;">Calibri</span>
		<br/>
		<span style="font-family: Arial; font-size: 18pt; color: #7E97A6;">Arial</span>
		<br/>
		<span style="font-family: Bitstream Vera Sans; font-size: 18pt; color: #7E97A6;">Bitstream Vera Sans</span>
		 -->
	</div>
	<div>
		<v:openFlashChart
			name="successRateChart"
			width="300"
			height="200"
			dataUrl="/getBuildHistory.do?transform=OpenFlashChart-BuildOutcomePieChart"/>
	</div>
	<div style="float: left; margin-right: 5em;">
		<v:openFlashChart
			name="metricsChart"
			width="400"
			height="300"
			dataUrl="/getBuildHistory.do?transform=OpenFlashChart-Metrics"/>
	</div>
	<div style="float: left">
		<v:openFlashChart
			name="buildDurationChart"
			width="400"
			height="300"
			dataUrl="/getBuildHistory.do?transform=OpenFlashChart-BuildDuration"/>
	</div>
</body>
</html>