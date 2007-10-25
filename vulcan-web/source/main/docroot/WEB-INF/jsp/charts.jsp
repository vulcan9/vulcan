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
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/plotkit/excanvas.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/mochikit/MochiKit.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/plotkit/Base.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/plotkit/Layout.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/plotkit/Canvas.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/plotkit/SweetCanvas.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/plotkit/Legend.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>
	<jsp:element name="script">
		<jsp:attribute name="type">text/javascript</jsp:attribute>
		<jsp:attribute name="src"><c:url value="/javascript/chart.js"/></jsp:attribute>
		<jsp:body/>
	</jsp:element>

<script type="text/javascript">
window.buildData = ${jsonBuildHistory};
</script>
</head>
<body>
	<form id="chartForm" method="post" action="#">
	<div style="float: left;">
		Chart Type:
		<select id="chartSelector">
			<option value="SuccessRateGraph">Success Rate</option>
			<option value="MetricsGraph">Build Metrics</option>
			<option value="ElapsedBuildTimeGraph">Elapsed Build Time</option>
			<option value="TotalBuildsGraph">Total Builds</option>
		</select>
	
		<div id="ElapsedBuildTimeGraphForm" style="display: none">	
			<ul class="metaDataOptions">
				<li>
					<input type="checkbox" id="chkFullBuilds" checked="checked"/>
					<label for="chkFullBuilds">Full Builds</label>
				</li>
				<li>
					<input type="checkbox" id="chkIncrementalBuilds" checked="checked"/>
					<label for="chkIncrementalBuilds">Incremental Builds</label>
				</li>
			</ul>
		</div>
		
		<div id="MetricsGraphForm" style="display: none">
		</div>
		
		<div id="SuccessRateGraphForm" style="display: none">
			Interval:
			<select id="intervalSelector">
				<option value="86400000">1 Day</option>
				<option value="604800000">1 Week</option>
				<option value="1209600000">2 Weeks</option>
				<option value="2419200000">4 Weeks</option>
			</select>
		</div>
		
		<div>
			<button id="btnRedraw">Generate</button>
		</div>
	</div>
	</form>
	<div style="float: left"><canvas id="myCanvas" height="600" width="800"></canvas></div>
	<div class="chart-legend">
		<span class="legend-header">Legend</span>
		<div id="myLegend"/>
	</div>
</body>
</html>