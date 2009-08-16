<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:vulcan="xalan://net.sourceforge.vulcan.web.XslHelper"
	extension-element-prefixes="vulcan"
	exclude-result-prefixes="vulcan">
	
	<xsl:output method="xml" media-type="text/html" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>
	
	<xsl:include href="common.xsl"/>
	
	<xsl:param name="preferences"/>
	<xsl:param name="contextRoot"/>
	<xsl:param name="viewProjectStatusURL"/>
	
	<xsl:key name="metrics-labels" match="/build-history/project/metrics/metric" use="@label"/>
	
	<xsl:variable name="allColumns" select="vulcan:getBuildHistoryAvailableColumns()/label"/>
	<xsl:variable name="visibleColumns" select="vulcan:getBuildHistoryVisibleColumns($preferences)/label"/>
	
	<xsl:template match="/build-history">
		<xsl:variable name="foo" select="vulcan:getBuildHistoryVisibleColumns($preferences)"/>
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US">
			<head>
				<title>Build History Report</title>
			</head>
			<body>
				<xsl:call-template name="buildHistoryReportSummary"/>

				<xsl:call-template name="columnVisibilitySelectors"/>
				
				<xsl:call-template name="buildHistoryTable"/>

				<xsl:call-template name="javascript"/>
			</body>
		</html>
	</xsl:template>
	
	<xsl:template name="javascript">
		<script xmlns="http://www.w3.org/1999/xhtml" language="javascript">
		function selectAll(checked) {
			$("#metrics-checkboxes input").each(function (item) {
				var wasChecked = $(this).is(':checked') ? true : false;
				
				if (wasChecked != checked) {
					$(this).attr("checked", checked);
					$(this).trigger("toggle");
				}
			});
		}
		
		$(document).ready(function() {
			$("#selectAll").click(function() { selectAll(true) });
			$("#selectNone").click(function() { selectAll(false) });
		});
		</script>
	</xsl:template>
	
	<xsl:template name="buildHistoryReportSummary">
		<table class="buildHistoryReportSummary" xmlns="http://www.w3.org/1999/xhtml">
			<caption>Report Summary</caption>
			<tbody>
				<tr>
					<th>From</th>
					<td><xsl:value-of select="@from"/></td>
				</tr>
				<tr>
					<th>To</th>
					<td><xsl:value-of select="@to"/></td>
				</tr>
				<tr>
					<th>Success Rate</th>
					<td>
						<xsl:value-of select="format-number(count(/build-history/project/status[text()='PASS']) div count(/build-history/project), '##0.#%')"/>
						(<xsl:value-of select="count(/build-history/project/status[text()='PASS'])"/>
						/
						<xsl:value-of select="count(/build-history/project)"/>)
					</td>
				</tr>
			</tbody>
		</table>
	</xsl:template>

	<xsl:template name="columnVisibilitySelectors">
		<form xmlns="http://www.w3.org/1999/xhtml" id="column-selectors" method="get" action="">
			<xsl:attribute name="action">
				<xsl:value-of select="$contextRoot"/>
				<xsl:text>/managePreferences.do</xsl:text>
			</xsl:attribute>
			<div>
				<h3 class="caption">Columns</h3>
				<div class="options">
					<ul id="metrics-checkboxes" class="metaDataOptions">
						<xsl:for-each select="$allColumns">
							<xsl:variable name="col" select="text()"/>
							<li>
								<input type="checkbox">
									<xsl:attribute name="id">
										<xsl:text>chk_</xsl:text>
										<xsl:value-of select="vulcan:mangle(.)"/>
									</xsl:attribute>
									<xsl:attribute name="value">
										<xsl:value-of select="."/>
									</xsl:attribute>
									<xsl:if test="$visibleColumns[text() = $col]">
										<xsl:attribute name="checked">checked</xsl:attribute>
									</xsl:if>
								</input>
								<label>
									<xsl:attribute name="for">
										<xsl:text>chk_</xsl:text>
										<xsl:value-of select="vulcan:mangle(.)"/>
									</xsl:attribute>
									<xsl:value-of select="vulcan:getMessage($messageSource, text())"/>
								</label>
							</li>
						</xsl:for-each>
					</ul>
				</div>
				<ul class="buttons">
					<li>
						<a id="selectAll" href="#">
							<xsl:value-of select="vulcan:getMessage($messageSource, 'button.select.all')"/>
						</a>
					</li>
					<li>
						<a id="selectNone" href="#">
							<xsl:value-of select="vulcan:getMessage($messageSource, 'button.select.none')"/>
						</a>
					</li>
				</ul>
			</div>
		</form>
	</xsl:template>
	
	<xsl:template name="buildHistoryTable">
		<table xmlns="http://www.w3.org/1999/xhtml" class="builds sortable">
			<caption>Outcomes</caption>
			<xsl:call-template name="project-column-headers">
				<xsl:with-param name="columns" select="$allColumns"/>
				<xsl:with-param name="visible-columns" select="$visibleColumns"/>
				<xsl:with-param name="sortSelect" select="'dashboard.columns.timestamp'"/>
				<xsl:with-param name="sortOrder" select="'ascending'"/>
				<xsl:with-param name="sortUrl" select="''"/>
			</xsl:call-template>
			<tbody>
				<xsl:apply-templates select="/build-history/project">
					<xsl:with-param name="columns" select="$allColumns"/>
					<xsl:with-param name="visible-columns" select="$visibleColumns"/>
					<xsl:with-param name="detailLink">
						<xsl:value-of select="$contextRoot"/>
						<xsl:text>/projects/</xsl:text>
					</xsl:with-param>
					<xsl:with-param name="detailLinkUseLatestKeyword" select="false()"/>
					<xsl:sort select="timestamp/@millis" order="ascending" data-type="number"/>
				</xsl:apply-templates>
			</tbody>
		</table>
	</xsl:template>
</xsl:stylesheet>