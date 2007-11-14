<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
	<xsl:output method="xml" media-type="text/html" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>
	
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
	
	<xsl:key name="builds-by-message" match="project" use="message"/>
	
	<xsl:key name="issue-ids" match="//issue" use="@issue-id"/>
	
	<xsl:template match="/project">
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US">
			<head>
				<title><xsl:value-of select="$title"/></title>
			</head>
			<body>
				<div class="content">
					<div class="build-nav">
						<xsl:apply-templates select="/project/prev-index"/>
						<xsl:text> </xsl:text>
						<xsl:value-of select="$buildNumberHeader"/>
						<xsl:text> </xsl:text>
						<xsl:value-of select="/project/build-number"/>
						<xsl:text> </xsl:text>
						<xsl:apply-templates select="/project/next-index"/>
						<br/><br/>
					</div>
					
					<xsl:if test="/project/currently-building">
						<xsl:call-template name="bubble">
							<xsl:with-param name="target" select="'currently-building'"/>
							<xsl:with-param name="styleClass" select="'warning'"/>
						</xsl:call-template>
					</xsl:if>
					
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'summary'"/>
						<xsl:with-param name="styleClass" select="'build-summary'"/>
					</xsl:call-template>
					
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'revisions'"/>
					</xsl:call-template>
					
					<xsl:if test="/project/metrics">
						<xsl:call-template name="bubble">
							<xsl:with-param name="target" select="'metrics'"/>
						</xsl:call-template>
					</xsl:if>
						
					<xsl:if test="/project/change-sets">
						<xsl:call-template name="bubble">
							<xsl:with-param name="target" select="'change-sets'"/>
							<xsl:with-param name="styleClass" select="'change-sets'"/>
						</xsl:call-template>
					</xsl:if>
					
					<xsl:if test="/project/test-failures">
						<xsl:call-template name="bubble">
							<xsl:with-param name="target" select="'test-failures'"/>
							<xsl:with-param name="styleClass" select="'test-failures'"/>
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
				</div>
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
		
		<table class="wrapper" xmlns="http://www.w3.org/1999/xhtml">
			<xsl:if test="$styleClass">
				<xsl:attribute name="class">wrapper <xsl:value-of select="$styleClass"/></xsl:attribute>
			</xsl:if>
			<tbody>
				<tr class="wrapper">
					<td class="wrapper">    
					    <div class="bubble">
							<xsl:if test="$styleClass">
								<xsl:attribute name="class">bubble <xsl:value-of select="$styleClass"/></xsl:attribute>
							</xsl:if>
					    
							<div class="upper-left"><xsl:value-of disable-output-escaping="yes" select="'&amp;nbsp;'"/></div>
							<div class="upper-right"><xsl:value-of disable-output-escaping="yes" select="'&amp;nbsp;'"/></div>
		
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
							
							<div class="lower-left"><xsl:value-of disable-output-escaping="yes" select="'&amp;nbsp;'"/></div>
							<div class="lower-right"><xsl:value-of disable-output-escaping="yes" select="'&amp;nbsp;'"/></div>
						</div>
					</td>
				</tr>
			</tbody>
		</table>
	</xsl:template>

	<xsl:template name="summary">
		<div xmlns="http://www.w3.org/1999/xhtml">
			<span class="caption"><xsl:value-of select="$title"/></span>
			
			<h1>
				<xsl:apply-templates select="/project/name"/><xsl:text> : </xsl:text>
				<xsl:apply-templates select="/project/status"/>
			</h1>
	
			<h3 class="build-outcome-message"><xsl:apply-templates select="/project/message"/></h3>
	
			<xsl:apply-templates select="/project/build-reason"/>
			
			<xsl:apply-templates select="/project/update-type"/>
			
			<xsl:apply-templates select="/project/build-requested-by"/>
			<xsl:apply-templates select="/project/build-scheduled-by"/>
			
			<xsl:apply-templates select="/project/elapsed-time"/>
			
			<xsl:apply-templates select="/project/last-good-build-number"/>
			
			<ul>
				<li>
					<xsl:element name="a">
							<xsl:attribute name="href"><xsl:value-of select="$projectSiteURL"/></xsl:attribute>
							<xsl:attribute name="class">external</xsl:attribute>
							<xsl:value-of select="$sandboxLabel"/>
					</xsl:element>
				</li>
				
				<xsl:apply-templates select="/project/repository-url"/>
				
				<xsl:if test="/project/build-log-available">
					<li>
						<xsl:element name="a">
								<xsl:attribute name="href"><xsl:value-of select="$viewProjectStatusURL"/>&amp;projectName=<xsl:value-of select="/project/name"/>&amp;buildNumber=<xsl:value-of select="/project/build-number"/>&amp;view=log</xsl:attribute>
								<xsl:attribute name="class">external</xsl:attribute>
								<xsl:value-of select="$buildLogLabel"/>
						</xsl:element>
					</li>
				</xsl:if>
				
				<xsl:if test="/project/diff-available">
					<li>
						<xsl:element name="a">
								<xsl:attribute name="href"><xsl:value-of select="$viewProjectStatusURL"/>&amp;projectName=<xsl:value-of select="/project/name"/>&amp;buildNumber=<xsl:value-of select="/project/build-number"/>&amp;view=diff</xsl:attribute>
								<xsl:attribute name="class">external</xsl:attribute>
								<xsl:value-of select="$diffHeader"/>
						</xsl:element>
					</li>
				</xsl:if>
				
				<xsl:if test="/project/metrics">
					<li>
						<a href="#metrics">
							<xsl:value-of select="$metricsLabel"/>
						</a>
					</li>
				</xsl:if>
				<xsl:if test="/project/test-failures">
					<li>
						<a href="#testfailures">
							<xsl:value-of select="$testFailureLabel"/>
						</a>
					</li>
				</xsl:if>
				<xsl:if test="/project/errors">
					<li>
						<a href="#errors">
							<xsl:value-of select="$errorsLabel"/>
						</a>
					</li>
				</xsl:if>
				<xsl:if test="/project/warnings">
					<li>
						<a href="#warnings">
							<xsl:value-of select="$warningsLabel"/>
						</a>
					</li>
				</xsl:if>
				<xsl:call-template name="issue-list"/>
			</ul>
		</div>
	</xsl:template>
	
	<xsl:template name="revisions">
		<table xmlns="http://www.w3.org/1999/xhtml">
			<caption><xsl:value-of select="$revisionCaption" disable-output-escaping="yes"/></caption>
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
	</xsl:template>
		
	<xsl:template match="build-reason">
		<div xmlns="http://www.w3.org/1999/xhtml" class="requestUser">
			<xsl:value-of select="$buildReasonLabel"/>
			<xsl:text> </xsl:text>
			<xsl:value-of select="."/>
		</div>
	</xsl:template>
	
	<xsl:template match="update-type">
		<div xmlns="http://www.w3.org/1999/xhtml" class="requestUser">
			<xsl:value-of select="$updateTypeLabel"/>
			<xsl:text> </xsl:text>
			<xsl:value-of select="."/>
		</div>
	</xsl:template>
	
	<xsl:template match="build-requested-by">
		<div xmlns="http://www.w3.org/1999/xhtml" class="requestUser">
			<xsl:value-of select="$buildRequestedByLabel"/>
			<xsl:text> </xsl:text>
			<xsl:value-of select="."/>
		</div>
	</xsl:template>
	
	<xsl:template match="build-scheduled-by">
		<div xmlns="http://www.w3.org/1999/xhtml" class="requestUser">
			<xsl:value-of select="$buildScheduledByLabel"/>
			<xsl:text> </xsl:text>
			<xsl:value-of select="."/>
		</div>
	</xsl:template>
	
	<xsl:template match="elapsed-time">
		<div xmlns="http://www.w3.org/1999/xhtml" class="requestUser">
			<xsl:value-of select="$elapsedTimeLabel"/>
			<xsl:text> </xsl:text>
			<xsl:value-of select="."/>
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
	
	<xsl:template match="repository-url">
		<li xmlns="http://www.w3.org/1999/xhtml" class="link">
			<xsl:element name="a">
				<xsl:attribute name="href"><xsl:value-of select="."/></xsl:attribute>
				<xsl:attribute name="class">external</xsl:attribute>
				<xsl:value-of select="$repositoryUrlLabel"/>
			</xsl:element>
		</li>
	</xsl:template>
	
	<xsl:template match="last-good-build-number">
		<div xmlns="http://www.w3.org/1999/xhtml">
			<xsl:value-of select="$lastGoodBuildNumberLabel"/>
			<xsl:call-template name="buildLink"/>
		</div>
	</xsl:template>
	
	<xsl:template match="change-sets">
		<table xmlns="http://www.w3.org/1999/xhtml">
			<caption><xsl:value-of select="$changeSetCaption" disable-output-escaping="yes"/></caption>
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
	
	<xsl:template name="issue-list">
		<xsl:param name="issues" select="//issue"/>
		<xsl:if test="$issues">
			<li xmlns="http://www.w3.org/1999/xhtml" class="issue-list">
				<xsl:value-of select="$issueListHeader"/>
				<ul xmlns="http://www.w3.org/1999/xhtml" class="issue-list">
					<xsl:for-each select="$issues[generate-id() = generate-id(key('issue-ids', @issue-id)[1])]">
						<xsl:sort select="@issue-id" order="ascending" data-type="number"/>
						<li>
							<xsl:call-template name="issue">
								<xsl:with-param name="text" select="@issue-id"/>
							</xsl:call-template>
						</li>
						<xsl:text> </xsl:text>
					</xsl:for-each>
				</ul>
			</li>
		</xsl:if>
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
		<a xmlns="http://www.w3.org/1999/xhtml" id="errors"/>
		<xsl:call-template name="build-messages">
			<xsl:with-param name="caption" select="'Build Errors'"/>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="/project/warnings">
		<a xmlns="http://www.w3.org/1999/xhtml" id="warnings"/>
		<xsl:call-template name="build-messages">
			<xsl:with-param name="caption" select="'Build Warnings'"/>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template name="build-messages">
		<xsl:param name="caption" select="'Build Messages'"/>
		
		<xsl:param name="showFiles" select="./*/@file"/>
		<xsl:param name="showLineNumbers" select="./*/@line-number"/>
		<xsl:param name="showCode" select="./*/@code"/>
		<xsl:param name="showTableHead" select="$showFiles or $showLineNumbers or $showCode"/>
		
		<table xmlns="http://www.w3.org/1999/xhtml" class="build-messages">
			<xsl:if test="not($showTableHead)">
				<xsl:attribute name="class">blended build-messages</xsl:attribute>
			</xsl:if>
			<caption><xsl:value-of select="$caption"/></caption>
			<xsl:if test="$showTableHead">
				<thead>
					<tr>
						<xsl:if test="$showFiles"><th class="long">File</th></xsl:if>
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
							<td><xsl:value-of select="@file"/></td>
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
		<a xmlns="http://www.w3.org/1999/xhtml" id="metrics"/>
		<table xmlns="http://www.w3.org/1999/xhtml">
			<caption><xsl:value-of select="$metricsLabel"/></caption>
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
	</xsl:template>
	
	<xsl:template match="/project/test-failures">
		<a xmlns="http://www.w3.org/1999/xhtml" id="testfailures"/>
		<table xmlns="http://www.w3.org/1999/xhtml">
			<caption><xsl:value-of select="$testFailureLabel"/></caption>
			<thead>
				<tr>
					<th class="long"><xsl:value-of select="$testNameLabel"/></th>
					<th><xsl:value-of select="$testFailureBuildNumberLabel"/></th>
				</tr>
			</thead>
			<tbody>
				<xsl:for-each select="./test-failure">
					<tr>
						<td><xsl:value-of select="@name"/></td>
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
