<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
	<xsl:output method="xml" media-type="text/html" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>
	
	<xsl:param name="contextRoot"/>
	<xsl:param name="title"/>
	<xsl:param name="projectSiteURL"/>
	<xsl:param name="viewProjectStatusURL"/>
	<xsl:param name="issueTrackerURL"/>
	<xsl:param name="issueListHeader"/>
	<xsl:param name="nextBuildLabel"/>
	<xsl:param name="prevBuildLabel"/>
	<xsl:param name="sandboxLabel"/>
	<xsl:param name="buildLogLabel"/>
	<xsl:param name="revisionCaption"/>
	<xsl:param name="changeSetCaption"/>
	<xsl:param name="projectHeader"/>
	<xsl:param name="revisionHeader"/>
	<xsl:param name="buildNumberHeader"/>
	<xsl:param name="authorHeader"/>
	<xsl:param name="timestampHeader"/>
	<xsl:param name="messageHeader"/>
	<xsl:param name="pathsHeader"/>
	<xsl:param name="diffHeader"/>
	<xsl:param name="statusHeader"/>
	<xsl:param name="lastGoodBuildNumberLabel"/>
	<xsl:param name="repositoryUrlLabel"/>
	<xsl:param name="repositoryTagNameHeader"/>
	<xsl:param name="currentlyBuildingMessage"/>
	<xsl:param name="buildRequestedByLabel"/>
	<xsl:param name="buildScheduledByLabel"/>
	<xsl:param name="elapsedTimeLabel"/>
	<xsl:param name="buildReasonLabel"/>
	<xsl:param name="updateTypeLabel"/>
	<xsl:param name="warningsLabel"/>
	<xsl:param name="errorsLabel"/>
	<xsl:param name="metricsLabel"/>
	<xsl:param name="testFailureLabel"/>
	<xsl:param name="testNameLabel"/>
	<xsl:param name="testFailureBuildNumberLabel"/>
	<xsl:param name="newTestFailureLabel"/>
	<xsl:param name="reloadInterval"/>
	<xsl:param name="buildDirectoryLabel"/>
	<xsl:param name="showBuildDirectory" select="'true'"/>
	<xsl:param name="workingCopyBuildNumber"/>
	
	<xsl:key name="metrics-labels" match="/build-history/project/metrics/metric" use="@label"/>
	
	<xsl:template match="/build-history">
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US">
			<head>
				<title>Build History Report</title>
			</head>
			<body>
				<div class="buildHistoryReport">
				
					<xsl:call-template name="buildHistoryReportSummary"/>
				
					<xsl:call-template name="columnVisibilitySelectors"/>
					
					<xsl:call-template name="buildHistoryTable"/>
				</div>
			</body>
		</html>
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
		<form xmlns="http://www.w3.org/1999/xhtml" id="column-selectors" method="get" action="#">
			<div>
				<h3 class="caption">Columns</h3>
				<div class="options">
					<ul id="metrics-checkboxes" class="metaDataOptions">
						<li>
							<input type="checkbox" checked="checked" id="build_number"/>
							<label for="build_number"><xsl:value-of select="$buildNumberHeader"/></label>
						</li>
						<li>
							<input type="checkbox" checked="checked" id="revision"/>
							<label for="revision"><xsl:value-of select="$revisionHeader"/></label>
						</li>
						<li>
							<input type="checkbox" checked="checked" id="tag"/>
							<label for="tag"><xsl:value-of select="$repositoryTagNameHeader"/></label>
						</li>
						<li>
							<input type="checkbox" checked="checked" id="tstamp"/>
							<label for="tstamp"><xsl:value-of select="$timestampHeader"/></label>
						</li>
						<li>
							<input type="checkbox" checked="checked" id="elapsed_time"/>
							<label for="elapsed_time"><xsl:value-of select="'Elapsed Time'"/></label>
						</li>
						<li>
							<input type="checkbox" checked="checked" id="status"/>
							<label for="status"><xsl:value-of select="$statusHeader"/></label>
						</li>
						<li>
							<input type="checkbox" checked="checked" id="message"/>
							<label for="message"><xsl:value-of select="$messageHeader"/></label>
						</li>
						<li>
							<input type="checkbox" checked="checked" id="update_type"/>
							<label for="update_type"><xsl:value-of select="'Build Type'"/></label>
						</li>
						<li>
							<input type="checkbox" checked="checked" id="requested_by"/>
							<label for="requested_by"><xsl:value-of select="'Build Scheduler / User'"/></label>
						</li>
						<li>
							<input type="checkbox" checked="checked" id="work_dir"/>
							<label for="work_dir"><xsl:value-of select="'Build Directory'"/></label>
						</li>
						
						<xsl:for-each select="/build-history/project/metrics/metric[generate-id() = generate-id(key('metrics-labels', @label)[1])]">
							<li>
								<input type="checkbox" checked="checked">
									<xsl:attribute name="id">
										<xsl:text>metric</xsl:text>
										<xsl:value-of select="position()"/>
									</xsl:attribute>
								</input>
								<label>
									<xsl:attribute name="for">
										<xsl:text>metric</xsl:text>
										<xsl:value-of select="position()"/>
									</xsl:attribute>
									<xsl:value-of select="@label"/>
								</label>
							</li>
						</xsl:for-each>
					</ul>
				</div>
			</div>
		</form>
	</xsl:template>
	
	<xsl:template name="buildHistoryTable">
		<table xmlns="http://www.w3.org/1999/xhtml" class="builds">
			<caption>Outcomes</caption>
			<thead>
				<tr id="build-data-headers">
					<th><xsl:value-of select="$projectHeader"/></th>
					<th id="col_build_number"><xsl:value-of select="$buildNumberHeader"/></th>
					<th id="col_revision"><xsl:value-of select="$revisionHeader"/></th>
					<th id="col_tag"><xsl:value-of select="$repositoryTagNameHeader"/></th>
					<th id="col_tstamp" class="timestamp"><xsl:value-of select="$timestampHeader"/></th>
					<th id="col_elapsed_time"><xsl:value-of select="'Elapsed Time'"/></th>
					<th id="col_status"><xsl:value-of select="$statusHeader"/></th>
					<th id="col_message"><xsl:value-of select="$messageHeader"/></th>
					<th id="col_update_type">Build Type</th>
					<th id="col_requested_by">Scheduler / User</th>
					<th id="col_work_dir">Build Directory</th>
					<xsl:for-each select="/build-history/project/metrics/metric[generate-id() = generate-id(key('metrics-labels', @label)[1])]">
						<th>
							<xsl:attribute name="id">
								<xsl:text>col_metric</xsl:text>
								<xsl:value-of select="position()"/>
							</xsl:attribute>
							<xsl:value-of select="@label"/>
						</th>
					</xsl:for-each>
				</tr>
			</thead>
			<tbody>
				<xsl:apply-templates select="/build-history/project">
					<xsl:sort select="timestamp/@millis" order="ascending" data-type="number"/>
				</xsl:apply-templates>
			</tbody>
		</table>
	</xsl:template>
	
	<xsl:template match="/build-history/project">
		<xsl:variable name="project" select="."/>
		<tr xmlns="http://www.w3.org/1999/xhtml">
			<td><xsl:apply-templates select="name"/></td>
			<td class="numeric">
				<a>
					<xsl:attribute name="href">
						<xsl:value-of select="$viewProjectStatusURL"/>
						<xsl:value-of select="name"/>
						<xsl:text>/</xsl:text>
						<xsl:value-of select="build-number"/>
						<xsl:text>/</xsl:text>
					</xsl:attribute>
					<xsl:apply-templates select="build-number"/>
				</a>
			</td>
			<td><xsl:apply-templates select="revision"/></td>
			<td><xsl:apply-templates select="repository-tag-name"/></td>
			<td class="timestamp"><xsl:apply-templates select="timestamp"/></td>
			<td><xsl:apply-templates select="elapsed-time"/></td>
			<xsl:element name="td">
				<xsl:attribute name="class"><xsl:value-of select="status"/> status</xsl:attribute>
				<xsl:value-of select="status"/>
			</xsl:element>
			<td class="buildMessage"><xsl:apply-templates select="message"/></td>
			<td><xsl:apply-templates select="update-type"/></td>
			<td>
				<xsl:apply-templates select="build-scheduled-by"/>
				<xsl:apply-templates select="build-requested-by"/>
			</td>
			<td><xsl:apply-templates select="work-directory"/></td>
			
			<xsl:for-each select="/build-history/project/metrics/metric[generate-id() = generate-id(key('metrics-labels', @label)[1])]">
				<xsl:variable name="label" select="@label"/>
				<xsl:variable name="metric-type" select="$project/metrics/metric[@label=$label]/@type"/>
				<td>
					<xsl:choose>
						<xsl:when test="$metric-type = 'percent'">
							<xsl:attribute name="class">numeric</xsl:attribute>
							<xsl:value-of select="format-number($project/metrics/metric[@label=$label]/@value, '#.##%')"/>
						</xsl:when>
						<xsl:when test="$metric-type = 'number'">
							<xsl:attribute name="class">numeric</xsl:attribute>
							<xsl:attribute name="title">
								<xsl:value-of select="$project/metrics/metric[@label=$label]/@value"/>
							</xsl:attribute>
							<xsl:value-of select="format-number($project/metrics/metric[@label=$label]/@value, '###,###.##')"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$project/metrics/metric[@label=$label]/@value"/>
						</xsl:otherwise>
					</xsl:choose>
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>
	
</xsl:stylesheet>