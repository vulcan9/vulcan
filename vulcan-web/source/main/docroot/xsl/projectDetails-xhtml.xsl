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
	
	<xsl:key name="builds-by-message" match="project" use="message"/>
	
	<xsl:key name="issue-ids" match="//issue" use="@issue-id"/>
	
	<xsl:variable name="num-errors" select="count(/project/errors/error)"/>
	<xsl:variable name="num-warnings" select="count(/project/warnings/warning)"/>
	<xsl:variable name="num-change-sets" select="count(/project/change-sets/change-set)"/>
	<xsl:variable name="num-test-failures" select="count(/project/test-failures/test-failure)"/>
	<xsl:variable name="work-directory" select="/project/work-directory/text()"/>
	
	<xsl:template match="/project">
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US">
			<head>
				<title><xsl:value-of select="$title"/></title>
				<!--
				<xsl:if test="/project/status='BUILDING' and $reloadInterval &gt; 0">
				</xsl:if>
				-->
			</head>
			<body>
				<form class="hidden" action="#" method="get">
					<input name="work-directory" id="work-directory" type="hidden">
						<xsl:attribute name="value"><xsl:value-of select="$work-directory"/></xsl:attribute>
						<xsl:text> </xsl:text>
					</input>
					<input id="repository-url" type="hidden">
						<xsl:attribute name="value"><xsl:value-of select="/project/repository-url"/></xsl:attribute>
						<xsl:text> </xsl:text>
					</input>
				</form>
				
				<div class="build-nav">
					<xsl:apply-templates select="/project/prev-index"/>
					<xsl:text> </xsl:text>
					<xsl:value-of select="$buildNumberHeader"/>
					<xsl:text> </xsl:text>
					<xsl:value-of select="/project/build-number"/>
					<xsl:text> </xsl:text>
					<xsl:apply-templates select="/project/next-index"/>
				</div>
				
				<h3>
					<xsl:apply-templates select="/project/name"/><xsl:text> : </xsl:text>
					<xsl:apply-templates select="/project/status"/>
				</h3>
	
				<h4 class="build-stats">
					<xsl:value-of select="$buildNumberHeader"/>
					<xsl:text> </xsl:text>
					<xsl:value-of select="/project/build-number"/>
					<xsl:if test="/project/revision">
						<xsl:text>, </xsl:text>
						<xsl:value-of select="$revisionHeader"/>
						<xsl:text> </xsl:text>
						<xsl:value-of select="/project/revision"/>
					</xsl:if>
					<xsl:if test="/project/repository-tag-name">
						<xsl:text> (</xsl:text>
						<xsl:value-of select="/project/repository-tag-name"/>
						<xsl:text>)</xsl:text>
					</xsl:if>
				</h4>
				
				<ul class="tabs" id="build-report-tabs">
					<li class="active"><a id="summary-tab" href="#summary-anchor"><xsl:value-of select="$title"/></a></li>
					<li><a id="dependencies-tab" href="#dependencies-anchor">Dependencies</a></li>
					<xsl:if test="$num-change-sets &gt; 0">
						<li><a id="change-sets-tab" href="#change-sets-anchor">Commit Log (<xsl:value-of select="$num-change-sets"/>)</a></li>
					</xsl:if>
					<xsl:if test="$num-errors &gt; 0">
						<li><a id="errors-tab" href="#errors-anchor">Errors (<xsl:value-of select="$num-errors"/>)</a></li>
					</xsl:if>
					<xsl:if test="$num-warnings &gt; 0">
						<li><a id="warnings-tab" href="#warnings-anchor">Warnings (<xsl:value-of select="$num-warnings"/>)</a></li>
					</xsl:if>
					<xsl:if test="$num-test-failures &gt; 0">
						<li><a id="test-failures-tab" href="#test-failures-anchor"><xsl:value-of select="$testFailureLabel"/> (<xsl:value-of select="$num-test-failures"/>)</a></li>
					</xsl:if>
					<xsl:if test="/project/metrics">
						<li><a id="metrics-tab" href="#metrics-anchor"><xsl:value-of select="$metricsLabel"/></a></li>
					</xsl:if>
					<xsl:if test="$showBuildDirectory">
						<li><a id="build-directory-tab" href="#build-directory-anchor">Build Directory</a></li>
					</xsl:if>
				</ul>
				
				<xsl:call-template name="bubble">
					<xsl:with-param name="target" select="'summary'"/>
					<xsl:with-param name="styleClass" select="'build-summary'"/>
				</xsl:call-template>
				
				<xsl:call-template name="bubble">
					<xsl:with-param name="target" select="'revisions'"/>
				</xsl:call-template>
				
				<xsl:if test="/project/change-sets">
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'change-sets'"/>
						<xsl:with-param name="styleClass" select="'change-sets'"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="/project/errors">
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'errors'"/>
						<xsl:with-param name="styleClass" select="'build-errors'"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="/project/warnings">
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'warnings'"/>
						<xsl:with-param name="styleClass" select="'build-errors'"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="/project/test-failures">
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'test-failures'"/>
						<xsl:with-param name="styleClass" select="'test-failures'"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="/project/metrics">
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'metrics'"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="$showBuildDirectory">
					<a name="build-directory-anchor"/>
					<div id="build-directory-panel" class="tab-panel">
						<xsl:apply-templates select="/project/work-directory"/>
						<xsl:choose>
							<xsl:when test="/project/work-directory[@available='true']">
								<xsl:if test="$workingCopyBuildNumber &gt; /project/build-number">
									<span class="warning">
										<xsl:text>The contents of this directory were overwritten by Build </xsl:text>
										<xsl:call-template name="buildLink">
											<xsl:with-param name="buildNumber">
												<xsl:value-of select="$workingCopyBuildNumber"/>
											</xsl:with-param>
										</xsl:call-template>
										<xsl:text>.</xsl:text>
									</span>
								</xsl:if>
								<iframe id="iframe" name="iframe" frameborder="0">
									<xsl:attribute name="src">
										<xsl:value-of select="$projectSiteURL"/>
									</xsl:attribute>
									<xsl:text></xsl:text>
								</iframe>
							</xsl:when>
							<xsl:otherwise>
								<span class="warning">
									<xsl:text>This directory is no longer present.</xsl:text>
							</span>
							</xsl:otherwise>
						</xsl:choose>
					</div>
				</xsl:if>
			</body>
		</html>
	</xsl:template>
	
	<xsl:template name="buildLink">
		<xsl:param name="buildNumber" select="."/>
		<xsl:param name="byIndex" select="false()"/>
		<xsl:param name="text" select="$buildNumber"/>
		
		<a xmlns="http://www.w3.org/1999/xhtml">
			<xsl:choose>
				<xsl:when test="$byIndex">
					<xsl:attribute name="href"><xsl:value-of select="$viewProjectStatusURL"/>&amp;projectName=<xsl:value-of select="/project/name"/>&amp;index=<xsl:value-of select="$buildNumber"/></xsl:attribute>
				</xsl:when>
				<xsl:otherwise>
					<xsl:attribute name="href"><xsl:value-of select="$viewProjectStatusURL"/>&amp;projectName=<xsl:value-of select="/project/name"/>&amp;buildNumber=<xsl:value-of select="$buildNumber"/></xsl:attribute>
				</xsl:otherwise>
			</xsl:choose>
			
			<xsl:value-of select="$text"/>
		</a>
	</xsl:template>
	
	<xsl:template name="bubble">
		<xsl:param name="target"/>
		<xsl:param name="styleClass"/>
		
		<xsl:choose>
			<xsl:when test="$target='currently-building'">
				<xsl:call-template name="currently-building"/>
			</xsl:when>
			<xsl:when test="$target='summary'">
				<xsl:call-template name="summary"/>
			</xsl:when>
			<xsl:when test="$target='revisions'">
				<xsl:call-template name="revisions"/>
			</xsl:when>
			<xsl:when test="$target='change-sets'">
				<xsl:apply-templates select="/project/change-sets"/>
			</xsl:when>
			<xsl:when test="$target='errors'">
				<xsl:apply-templates select="/project/errors"/>
			</xsl:when>
			<xsl:when test="$target='warnings'">
				<xsl:apply-templates select="/project/warnings"/>
			</xsl:when>
			<xsl:when test="$target='metrics'">
				<xsl:apply-templates select="/project/metrics"/>
			</xsl:when>
			<xsl:when test="$target='reports'">
				<xsl:apply-templates select="/project/reports"/>
			</xsl:when>
			<xsl:when test="$target='test-failures'">
				<xsl:apply-templates select="/project/test-failures"/>
			</xsl:when>
			<xsl:when test="$target='buildHistoryReportSummary'">
				<xsl:call-template name="buildHistoryReportSummary"/>
			</xsl:when>
			<xsl:when test="$target='messageOccurrence'">
				<xsl:call-template name="messageOccurrence"/>
			</xsl:when>
			<xsl:when test="$target='history-outcomes'">
				<xsl:call-template name="history-outcomes"/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

	<xsl:template name="summary">
		<div xmlns="http://www.w3.org/1999/xhtml" id="summary-panel" class="tab-panel">
			<a name="summary-anchor"/>
			<xsl:if test="/project/message != ''">
				<h5 class="build-outcome-message">
					<xsl:choose>
						<xsl:when test="substring-before(/project/message, '&#10;')!=''">
							<xsl:value-of select="substring-before(/project/message, '&#10;')"/>
							<xsl:text>...</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="/project/message"/>
						</xsl:otherwise>
					</xsl:choose>
				</h5>
			</xsl:if>
			
			<div class="build-stats">
				<dl>
					<xsl:if test="/project/build-reason">
						<dt><xsl:value-of select="$buildReasonLabel"/></dt>
						<dd><xsl:value-of select="/project/build-reason"/></dd>
					</xsl:if>
					
					<dt><xsl:value-of select="$updateTypeLabel"/></dt>
					<dd><xsl:value-of select="/project/update-type"/></dd>
					
					<xsl:if test="/project/build-requested-by">
						<dt><xsl:value-of select="$buildRequestedByLabel"/></dt>
						<dd><xsl:value-of select="/project/build-requested-by"/></dd>
					</xsl:if>
								
					<xsl:if test="/project/build-scheduled-by">
						<dt><xsl:value-of select="$buildScheduledByLabel"/></dt>
						<dd><xsl:value-of select="/project/build-scheduled-by"/></dd>
					</xsl:if>
					
					<dt><xsl:value-of select="$elapsedTimeLabel"/></dt>
					<dd><xsl:value-of select="/project/elapsed-time"/></dd>
					
					<xsl:if test="/project/last-good-build-number">
						<dt><xsl:value-of select="$lastGoodBuildNumberLabel"/></dt>
						<dd>
							<xsl:call-template name="buildLink">
								<xsl:with-param name="buildNumber" select="/project/last-good-build-number"/>
							</xsl:call-template>
						</dd>
					</xsl:if>
				</dl>
				<xsl:if test="/project/build-log-available">
					<xsl:element name="a">
							<xsl:attribute name="href"><xsl:value-of select="$viewProjectStatusURL"/>&amp;projectName=<xsl:value-of select="/project/name"/>&amp;buildNumber=<xsl:value-of select="/project/build-number"/>&amp;view=log</xsl:attribute>
							<xsl:attribute name="class">external</xsl:attribute>
							<xsl:value-of select="$buildLogLabel"/>
					</xsl:element>
				</xsl:if>
			</div>
			
			<xsl:if test="/project/reports">
				<xsl:call-template name="bubble">
					<xsl:with-param name="target" select="'reports'"/>
				</xsl:call-template>
			</xsl:if>
			
			<!-- IE7 can't figure out <div/>.  Render <div> </div> instead. -->
			<div style="clear: left;"><xsl:text> </xsl:text></div>
		</div>
	</xsl:template>
	
	<xsl:template name="revisions">
		<div xmlns="http://www.w3.org/1999/xhtml" id="dependencies-panel" class="tab-panel">
			<a name="dependencies-anchor"/>
			<table>
				<caption class="panel-caption"><xsl:value-of select="$revisionCaption"/></caption>
				<thead>
					<tr>
						<th><xsl:value-of select="$projectHeader"/></th>
						<th><xsl:value-of select="$buildNumberHeader"/></th>
						<th><xsl:value-of select="$revisionHeader"/></th>
						<th><xsl:value-of select="$repositoryTagNameHeader"/></th>
						<th><xsl:value-of select="$timestampHeader"/></th>
						<th><xsl:value-of select="$statusHeader"/></th>
					</tr>
				</thead>
				<tbody>
					<xsl:apply-templates select="/project/dependencies"/>
					<tr>
						<td><xsl:apply-templates select="/project/name"/></td>
						<td><xsl:apply-templates select="/project/build-number"/></td>
						<td><xsl:apply-templates select="/project/revision"/></td>
						<td><xsl:apply-templates select="/project/repository-tag-name"/></td>
						<td><xsl:apply-templates select="/project/timestamp"/></td>
						<xsl:element name="td">
							<xsl:attribute name="class"><xsl:value-of select="/project/status"/> status</xsl:attribute>
							<xsl:value-of select="/project/status"/>
						</xsl:element>
					</tr>
				</tbody>
			</table>
		</div>
	</xsl:template>
	
	<xsl:template match="/project/reports">
		<table>
			<caption>Reports</caption>
			<tbody>
				<xsl:for-each select="./report">
					<tr>
						<td>
							<a target="iframe" class="report-link">
								<xsl:attribute name="href">
									<xsl:value-of select="$projectSiteURL"/>
									<xsl:value-of select="./path"/>
								</xsl:attribute>
								<xsl:value-of select="./name"/>
							</a>
						</td>
						<td>
							<xsl:choose>
								<xsl:when test="./description = ''">
									<xsl:text> </xsl:text>
									<!-- IE7 can't figure out <td/>.  Render <td> </td> instead. -->
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="./description"/>
								</xsl:otherwise>
							</xsl:choose>
						</td>
					</tr>
				</xsl:for-each>
			</tbody>
		</table>
	</xsl:template>
	
	<xsl:template match="work-directory">
		<div xmlns="http://www.w3.org/1999/xhtml">
			<xsl:value-of select="$buildDirectoryLabel"/>
			<xsl:text> </xsl:text>
			<span id="build-directory-root"><xsl:value-of select="."/></span>
			<span id="build-directory-bread-crumbs"><xsl:text> </xsl:text></span>
		</div>
	</xsl:template>
	
	<xsl:template name="currently-building">
		<span class="warning" xmlns="http://www.w3.org/1999/xhtml">
			<xsl:value-of select="$currentlyBuildingMessage"/>
		</span>
	</xsl:template>
	
	<xsl:template match="next-index">
		<xsl:call-template name="buildLink">
			<xsl:with-param name="buildNumber" select="."/>
			<xsl:with-param name="text" select="$nextBuildLabel"/>
			<xsl:with-param name="byIndex" select="true()"/>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="prev-index">
		<xsl:call-template name="buildLink">
			<xsl:with-param name="buildNumber" select="."/>
			<xsl:with-param name="text" select="$prevBuildLabel"/>
			<xsl:with-param name="byIndex" select="true()"/>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="change-sets">
		<div xmlns="http://www.w3.org/1999/xhtml" id="change-sets-panel" class="tab-panel">
			<a name="change-sets-anchor"/>
			<table class="sortable">
				<thead>
					<tr>
						<th><xsl:value-of select="$revisionHeader"/></th>
						<th><xsl:value-of select="$authorHeader"/></th>
						<th class="timestamp"><xsl:value-of select="$timestampHeader"/></th>
						<th><xsl:value-of select="$messageHeader"/></th>
						<th><xsl:value-of select="$pathsHeader"/></th>
					</tr>
				</thead>
				<tbody>
					<xsl:for-each select="change-set">
						<xsl:sort select="./timestamp/@millis" order="ascending" data-type="number"/>
						<tr>
							<td><xsl:apply-templates select="@revision"/></td>
							<td><xsl:apply-templates select="@author"/></td>
							<td class="timestamp"><xsl:value-of select="timestamp"/></td>
							<td class="commit-message"><xsl:apply-templates select="message"/></td>
							<td>
								<ul class="modified-paths">
								<xsl:for-each select="./modified-paths/path">
									<xsl:sort select="." order="ascending" data-type="text"/>
									<li><xsl:apply-templates select="."/></li>
								</xsl:for-each>
								</ul>
							</td>
						</tr>
					</xsl:for-each>
				</tbody>
			</table>
			<xsl:if test="/project/diff-available or /project/repository-url">
				<ul> 
					<xsl:if test="/project/diff-available">
						<li>
							<xsl:element name="a">
								<xsl:attribute name="href"><xsl:value-of select="$viewProjectStatusURL"/>&amp;projectName=<xsl:value-of select="/project/name"/>&amp;buildNumber=<xsl:value-of select="/project/build-number"/>&amp;view=diff</xsl:attribute>
								<xsl:attribute name="class">external</xsl:attribute>
								<xsl:value-of select="$diffHeader"/>
							</xsl:element>
						</li>
					</xsl:if>
					<xsl:if test="/project/repository-url">
						<li>
							<xsl:element name="a">
								<xsl:attribute name="href"><xsl:value-of select="/project/repository-url"/></xsl:attribute>
								<xsl:attribute name="class">external</xsl:attribute>
								<xsl:value-of select="$repositoryUrlLabel"/>
							</xsl:element>
						</li>
					</xsl:if>
				</ul>
			</xsl:if>
		</div>
	</xsl:template>
	
	<xsl:template match="link">
		<a xmlns="http://www.w3.org/1999/xhtml" class="external">
			<xsl:attribute name="href"><xsl:value-of select="."/></xsl:attribute>
			<xsl:value-of select="."/>
		</a>
	</xsl:template>
	
	<xsl:template match="issue">
		<xsl:call-template name="issue"/>
	</xsl:template>
	
	<xsl:template name="issue">
		<xsl:param name="content" select="."/>
		<xsl:param name="text" select="."/>
		
		<xsl:choose>
			<xsl:when test="$issueTrackerURL = ''">
				<span xmlns="http://www.w3.org/1999/xhtml" class="issue-unlinked">
					<xsl:value-of select="$text"/>
				</span>
			</xsl:when>
			<xsl:otherwise>
				<a xmlns="http://www.w3.org/1999/xhtml" class="issue external">
					<xsl:attribute name="href">
						<xsl:call-template name="replace-string">
							<xsl:with-param name="text" select="$issueTrackerURL"/>
							<xsl:with-param name="from" select="'%BUGID%'"/>
							<xsl:with-param name="to" select="$content/@issue-id"/>
						</xsl:call-template>
					</xsl:attribute>
					<xsl:value-of select="$text"/>
				</a>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="dependencies">
		<xsl:for-each select="dependency">
			<xsl:sort select="timestamp/@millis" order="ascending" data-type="number"/>
			<tr xmlns="http://www.w3.org/1999/xhtml">
				<td><xsl:apply-templates select="@name"/></td>
				<td>
					<a>
						<xsl:attribute name="href"><xsl:value-of select="$viewProjectStatusURL"/>&amp;projectName=<xsl:value-of select="@name"/>&amp;buildNumber=<xsl:value-of select="@build-number"/></xsl:attribute>
						<xsl:apply-templates select="@build-number"/>
					</a>
				</td>
				<td><xsl:apply-templates select="@revision"/></td>
				<td><xsl:apply-templates select="@repository-tag-name"/></td>
				<td><xsl:apply-templates select="timestamp"/></td>
				<xsl:element name="td">
					<xsl:attribute name="class"><xsl:value-of select="@status"/> status</xsl:attribute>
					<xsl:value-of select="@status"/>
				</xsl:element>
			</tr>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template match="/project/errors">
		<div xmlns="http://www.w3.org/1999/xhtml" id="errors-panel" class="tab-panel">
			<a name="errors-anchor"/>
			<xsl:call-template name="build-messages">
				<xsl:with-param name="caption" select="'Build Errors'"/>
			</xsl:call-template>
		</div>
	</xsl:template>
	
	<xsl:template match="/project/warnings">
		<div xmlns="http://www.w3.org/1999/xhtml" id="warnings-panel" class="tab-panel">
			<a name="warnings-anchor"/>
			<xsl:call-template name="build-messages">
				<xsl:with-param name="caption" select="'Build Warnings'"/>
			</xsl:call-template>
		</div>
	</xsl:template>
	
	<xsl:template name="build-messages">
		<xsl:param name="caption" select="'Build Messages'"/>
		
		<xsl:param name="showFiles" select="./*/@file"/>
		<xsl:param name="showLineNumbers" select="./*/@line-number"/>
		<xsl:param name="showCode" select="./*/@code"/>
		<xsl:param name="showTableHead" select="$showFiles or $showLineNumbers or $showCode"/>
		
		<table xmlns="http://www.w3.org/1999/xhtml" class="build-messages sortable">
			<caption class="panel-caption"><xsl:value-of select="$caption"/></caption>
			<xsl:if test="not($showTableHead)">
				<xsl:attribute name="class">blended build-messages</xsl:attribute>
			</xsl:if>
			<xsl:if test="$showTableHead">
				<thead>
					<tr>
						<xsl:if test="$showFiles"><th class="file">File</th></xsl:if>
						<xsl:if test="$showLineNumbers"><th>Line</th></xsl:if>
						<xsl:if test="$showCode"><th>Code</th></xsl:if>
						<th class="long">Message</th>
					</tr>
				</thead>
			</xsl:if>
			<tbody>
				<xsl:for-each select="./*">
					<tr>
						<xsl:if test="$showFiles">
							<td class="file">
								<xsl:choose>
									<xsl:when test="starts-with(@file, $work-directory)">
										<xsl:variable name="rel-path" select="substring-after(@file, $work-directory)"/>
										<xsl:choose>
											<xsl:when test="starts-with($rel-path, '\') or starts-with($rel-path, '/')">
												<xsl:value-of select="substring($rel-path, 2)"/>
											</xsl:when>
											<xsl:otherwise>
												<xsl:value-of select="$rel-path"/>
											</xsl:otherwise>
										</xsl:choose>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="@file"/>
									</xsl:otherwise>
								</xsl:choose>
							</td>
						</xsl:if>
						<xsl:if test="$showLineNumbers">
							<td><xsl:value-of select="@line-number"/></td>
						</xsl:if>
						<xsl:if test="$showCode">
							<td><xsl:value-of select="@code"/></td>
						</xsl:if>
						<td>
							<xsl:value-of select="."/>
						</td>
					</tr>
				</xsl:for-each>
			</tbody>
		</table>
	</xsl:template>
	
	<xsl:template match="/project/metrics">
		<div xmlns="http://www.w3.org/1999/xhtml" class="tab-panel" id="metrics-panel">
			<a name="metrics-anchor"/>
			<table>
				<tbody>
					<xsl:for-each select="./metric">
						<tr>
							<td><xsl:value-of select="@label"/></td>
							<td>
								<xsl:choose>
									<xsl:when test="@type='percent'">
										<xsl:value-of select="format-number(@value, '#.##%')"/>
									</xsl:when>
									<xsl:otherwise>
										<xsl:value-of select="@value"/>
									</xsl:otherwise>
								</xsl:choose>
							</td>
						</tr>
					</xsl:for-each>
				</tbody>
			</table>
			<blockquote>
				These metrics were gathered from the build directory.
				<a href="http://code.google.com/p/vulcan/wiki/XmlMetrics" class="external">Learn how to add custom metrics.</a>
			</blockquote>
		</div>
	</xsl:template>
	
	<xsl:template match="/project/test-failures">
		<div class="tab-panel" id="test-failures-panel" xmlns="http://www.w3.org/1999/xhtml">
			<a name="test-failures-anchor"/>
			<table>
				<thead>
					<tr>
						<th class="long"><xsl:value-of select="$testNameLabel"/></th>
						<th><xsl:value-of select="$testFailureBuildNumberLabel"/></th>
					</tr>
				</thead>
				<tbody>
					<xsl:for-each select="./test-failure">
						<tr>
							<td>
								<span class="test-name"><xsl:value-of select="@name"/></span>
								<h6 class="test-namespace"><xsl:value-of select="@namespace"/></h6>
							</td>
							<td class="build-number">
								<xsl:choose>
									<xsl:when test="@first-build = /project/build-number">
										<span class="new-test-failure">
											<xsl:value-of select="$newTestFailureLabel"/>
										</span>
									</xsl:when>
									<xsl:otherwise>
										<xsl:call-template name="buildLink">
											<xsl:with-param name="buildNumber" select="@first-build"/>
										</xsl:call-template>
									</xsl:otherwise>
								</xsl:choose>
							</td>
						</tr>
					</xsl:for-each>
				</tbody>
			</table>
			<div style="clear: left;"><xsl:text> </xsl:text></div>
		</div>
	</xsl:template>
	
	<xsl:template match="/build-history">
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US">
			<head>
				<title>Build History Report</title>
				<link rel="stylesheet" href="css/standard.css" type="text/css"/>
			</head>
			<body>
				<div class="buildHistoryReport">
				
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'buildHistoryReportSummary'"/>
					</xsl:call-template>
				
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'messageOccurrence'"/>
					</xsl:call-template>
			
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'history-outcomes'"/>
					</xsl:call-template>
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
	
	<xsl:template name="messageOccurrence">
		<table xmlns="http://www.w3.org/1999/xhtml">
			<caption>Message Occurrence</caption>
			<thead>
				<tr>
					<th>Count</th>
					<th>Rate</th>
					<th>Message</th>
				</tr>
			</thead>
			<tbody>
				<xsl:variable name="total" select="count(project)"/>
				<xsl:for-each select="project[generate-id() = generate-id(key('builds-by-message', message)[1])]">
					<xsl:sort select="count(key('builds-by-message', message))" data-type="number" order="descending"/>
					<xsl:variable name="count" select="count(key('builds-by-message', message))"/>
					<tr>
						<td>
							<xsl:value-of select="$count"/>
						</td>
						<td>
							<xsl:value-of select="format-number($count div $total, '##0.#%')"/>
						</td>
						<td>
							<xsl:value-of select="message"/>
						</td>
					</tr>
				</xsl:for-each>
			</tbody>
		</table>
	</xsl:template>
	
	<xsl:template name="history-outcomes">
		<table xmlns="http://www.w3.org/1999/xhtml" class="builds">
			<caption>Outcomes</caption>
			<thead>
				<tr>
					<th><xsl:value-of select="$projectHeader"/></th>
					<th><xsl:value-of select="$buildNumberHeader"/></th>
					<th><xsl:value-of select="$revisionHeader"/></th>
					<th><xsl:value-of select="$repositoryTagNameHeader"/></th>
					<th class="timestamp"><xsl:value-of select="$timestampHeader"/></th>
					<th><xsl:value-of select="$statusHeader"/></th>
					<th><xsl:value-of select="$messageHeader"/></th>
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
		<tr xmlns="http://www.w3.org/1999/xhtml">
			<td><xsl:apply-templates select="name"/></td>
			<td>
				<a>
					<xsl:attribute name="href"><xsl:value-of select="$viewProjectStatusURL"/>&amp;projectName=<xsl:value-of select="name"/>&amp;buildNumber=<xsl:value-of select="build-number"/></xsl:attribute>
					<xsl:apply-templates select="build-number"/>
				</a>
			</td>
			<td><xsl:apply-templates select="revision"/></td>
			<td><xsl:apply-templates select="repository-tag-name"/></td>
			<td class="timestamp"><xsl:apply-templates select="timestamp"/></td>
			<xsl:element name="td">
				<xsl:attribute name="class"><xsl:value-of select="status"/> status</xsl:attribute>
				<xsl:value-of select="status"/>
			</xsl:element>
			<td class="buildMessage"><xsl:apply-templates select="message"/></td>
		</tr>
	</xsl:template>
	
	<xsl:template name="replace-string">
		<xsl:param name="text"/>
		<xsl:param name="from"/>
		<xsl:param name="to"/>

		<xsl:choose>
			<xsl:when test="contains($text, $from)">

				<xsl:variable name="before" select="substring-before($text, $from)"/>
				<xsl:variable name="after" select="substring-after($text, $from)"/>
				<xsl:variable name="prefix" select="concat($before, $to)"/>
			
				<xsl:value-of select="$before"/>
				<xsl:value-of select="$to"/>
				<xsl:call-template name="replace-string">
					<xsl:with-param name="text" select="$after"/>
					<xsl:with-param name="from" select="$from"/>
					<xsl:with-param name="to" select="$to"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>