<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:vulcan="xalan://net.sourceforge.vulcan.web.XslHelper"
	extension-element-prefixes="vulcan"
	exclude-result-prefixes="vulcan">
	
	<xsl:output method="xml" media-type="text/html" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>
	
	<xsl:include href="common.xsl"/>
	
	<xsl:param name="contextRoot"/>
	<xsl:param name="projectSiteURL"/>
	<xsl:param name="viewProjectStatusURL"/>
	<xsl:param name="issueTrackerURL"/>
	
	<xsl:param name="reloadInterval"/>
	<xsl:param name="showBuildDirectory" select="'true'"/>
	<xsl:param name="workingCopyBuildNumber"/>
	<xsl:param name="view"/>
	
	<xsl:key name="metrics-labels" match="/build-history/project/metrics/metric" use="@label"/>
	
	<xsl:variable name="num-errors" select="count(/project/errors/error)"/>
	<xsl:variable name="num-warnings" select="count(/project/warnings/warning)"/>
	<xsl:variable name="num-changes" select="count(/project/change-sets/change-set)"/>
	<xsl:variable name="num-test-failures" select="count(/project/test-failures/test-failure)"/>
	<xsl:variable name="work-directory" select="/project/work-directory/text()"/>
	
	<xsl:variable name="visible-div-id">
		<xsl:choose>
			<xsl:when test="$view='changes' and $num-changes &gt; 0">
				<xsl:text>changes-panel</xsl:text>
			</xsl:when>
			<xsl:when test="$view='errors' and $num-errors &gt; 0">
				<xsl:text>errors-panel</xsl:text>
			</xsl:when>
			<xsl:when test="$view='warnings' and $num-warnings &gt; 0">
				<xsl:text>warnings-panel</xsl:text>
			</xsl:when>
			<xsl:when test="$view='tests' and $num-test-failures &gt; 0">
				<xsl:text>tests-panel</xsl:text>
			</xsl:when>
			<xsl:when test="$view='metrics' and /project/metrics">
				<xsl:text>metrics-panel</xsl:text>
			</xsl:when>
			<xsl:when test="$view='browse' and $showBuildDirectory">
				<xsl:text>browse-panel</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>summary-panel</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	<xsl:template match="/project">
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US">
			<head>
				<title>
					<xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.summary')"/>
				</title>
				
				<link title="RSS" rel="alternate" type="application/rss+xml">
					<xsl:attribute name="href">
						<xsl:value-of select="$viewProjectStatusURL"/>
						<xsl:value-of select="name"/>
						<xsl:text>/rss/</xsl:text>
					</xsl:attribute>
				</link>
				
				<style type="text/css">
					<xsl:text>div#</xsl:text>
					<xsl:value-of select="$visible-div-id"/>
					<xsl:text> {display: block;}</xsl:text>
				</style>
				<!--
				<xsl:if test="/project/status='BUILDING' and $reloadInterval &gt; 0">
				</xsl:if>
				-->
				<script type="text/javascript">
					function claimBuild(event) {
						event.preventDefault();
						
						if (event.canceled) return false;
						
						var url = $(this).attr("href");
						
						$("#broken-build-claim").loadCustom(url, null, "#broken-build-claim");
						
						return false;
					}
					
					$(document).ready(function() {
						$("#claimBuild").click(claimBuild);
					});
				</script>
			</head>
			<body>
				<form class="hidden" action="#" method="get">
					<div>
						<input name="work-directory" id="work-directory" type="hidden">
							<xsl:attribute name="value"><xsl:value-of select="$work-directory"/></xsl:attribute>
							<xsl:text> </xsl:text>
						</input>
						<input id="repository-url" type="hidden">
							<xsl:attribute name="value"><xsl:value-of select="/project/repository-url"/></xsl:attribute>
							<xsl:text> </xsl:text>
						</input>
					</div>
				</form>

				<div id="build-report-header">				
					<xsl:if test="/project/previous-build-number or /project/next-build-number">
						<div class="build-nav">
							<xsl:apply-templates select="/project/previous-build-number"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="vulcan:getMessage($messageSource, 'th.build.number')"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="/project/build-number"/>
							<xsl:text> </xsl:text>
							<xsl:apply-templates select="/project/next-build-number"/>
						</div>
					</xsl:if>
					
					<h3>
						<xsl:apply-templates select="/project/name"/>
						<xsl:text> : </xsl:text>
						<xsl:apply-templates select="/project/status"/>
					</h3>
		
					<xsl:choose>
						<xsl:when test="/project/status/text() = 'FAIL' and not(/project/broken-by)">
							<p id="broken-build-claim">
								<a id="claimBuild">
									<xsl:attribute name="href">
										<xsl:value-of select="$contextRoot"/>
										<xsl:text>/wall/claimBrokenBuild.do?action=claim&amp;projectName=</xsl:text>
										<xsl:value-of select="/project/name"/>
										<xsl:text>&amp;buildNumber=</xsl:text>
										<xsl:value-of select="/project/build-number"/>
									</xsl:attribute>
									<xsl:text>Claim responsibility for this build failure</xsl:text> 
								</a>
							</p>
						</xsl:when>
						<xsl:when test="/project/broken-by">
							<p id="broken-build-claim">
								<xsl:value-of select="vulcan:getMessage($messageSource, 'messages.broken.by', concat(/project/broken-by, ' on ', /project/claim-date/@text))"/>
							</p>
						</xsl:when>
					</xsl:choose>
					
					<p class="build-stats">
						<xsl:value-of select="vulcan:getMessage($messageSource, 'th.build.number')"/>
						<xsl:text> </xsl:text>
						<xsl:value-of select="/project/build-number"/>
						<xsl:if test="/project/revision">
							<xsl:text>, </xsl:text>
							<xsl:value-of select="vulcan:getMessage($messageSource, 'th.revision')"/>
							<xsl:text> </xsl:text>
							<xsl:value-of select="/project/revision"/>
						</xsl:if>
						<xsl:if test="/project/repository-tag-name">
							<xsl:text> (</xsl:text>
							<xsl:value-of select="/project/repository-tag-name"/>
							<xsl:text>)</xsl:text>
						</xsl:if>
					</p>
					<xsl:if test="/project/timestamp/@text">
						<p class="build-stats">
							<xsl:text>Finished on </xsl:text>
							<xsl:value-of select="/project/timestamp/@text"/>
						</p>
					</xsl:if>
					
					<ul class="tabs" id="build-report-tabs">
						<li>
							<xsl:if test="$view='summary'">
								<xsl:attribute name="class">active</xsl:attribute>
							</xsl:if>
							<a id="summary-tab" href="#summary-panel">
								<xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.summary')"/>
							</a>
						</li>
						<xsl:if test="$num-changes &gt; 0">
							<li>
								<xsl:if test="$view='changes'">
									<xsl:attribute name="class">active</xsl:attribute>
								</xsl:if>
								<a id="changes-tab" href="#changes-panel">Commit Log (<xsl:value-of select="$num-changes"/>)</a>
							</li>
						</xsl:if>
						<xsl:if test="$num-errors &gt; 0">
							<li>
								<xsl:if test="$view='errors'">
									<xsl:attribute name="class">active</xsl:attribute>
								</xsl:if>
								<a id="errors-tab" href="#errors-panel">Errors (<xsl:value-of select="$num-errors"/>)</a>
							</li>
						</xsl:if>
						<xsl:if test="$num-warnings &gt; 0">
							<li>
								<xsl:if test="$view='warnings'">
									<xsl:attribute name="class">active</xsl:attribute>
								</xsl:if>
								<a id="warnings-tab" href="#warnings-panel">Warnings (<xsl:value-of select="$num-warnings"/>)</a>
							</li>
						</xsl:if>
						<xsl:if test="$num-test-failures &gt; 0">
							<li>
								<xsl:if test="$view='tests'">
									<xsl:attribute name="class">active</xsl:attribute>
								</xsl:if>
								<a id="tests-tab" href="#tests-panel"><xsl:value-of select="vulcan:getMessage($messageSource, 'label.test.failures')"/> (<xsl:value-of select="$num-test-failures"/>)</a>
							</li>
						</xsl:if>
						<xsl:if test="/project/metrics">
							<li>
								<xsl:if test="$view='metrics'">
									<xsl:attribute name="class">active</xsl:attribute>
								</xsl:if>
								<a id="metrics-tab" href="#metrics-panel"><xsl:value-of select="vulcan:getMessage($messageSource, 'label.metrics')"/></a>
							</li>
						</xsl:if>
						<xsl:if test="$showBuildDirectory">
							<li>
								<xsl:if test="$view='browse'">
									<xsl:attribute name="class">active</xsl:attribute>
								</xsl:if>
								<a id="browse-tab" href="#browse-panel">Build Directory</a>
							</li>
						</xsl:if>
					</ul>
				</div>
				
				<xsl:call-template name="bubble">
					<xsl:with-param name="target" select="'summary'"/>
					<xsl:with-param name="styleClass" select="'build-summary'"/>
				</xsl:call-template>
				
				<xsl:if test="/project/change-sets">
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'changes'"/>
						<xsl:with-param name="styleClass" select="'changes'"/>
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
					<div id="browse-panel" class="tab-panel">
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
								<iframe id="iframe" name="iframe" frameborder="0" src="site/">
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
		<xsl:param name="text" select="$buildNumber"/>
		
		<a xmlns="http://www.w3.org/1999/xhtml" class="build-link">
			<xsl:attribute name="href">
				<xsl:value-of select="$viewProjectStatusURL"/>
				<xsl:value-of select="/project/name"/>
				<xsl:text>/</xsl:text>
				<xsl:value-of select="$buildNumber"/>
				<xsl:text>/</xsl:text>
				<xsl:value-of select="$view"/>
			</xsl:attribute>
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
			<xsl:when test="$target='changes'">
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
		</xsl:choose>
	</xsl:template>

	<xsl:template name="summary">
		<div xmlns="http://www.w3.org/1999/xhtml" id="summary-panel" class="tab-panel">
			<xsl:if test="/project/message != ''">
				<h4 class="build-outcome-message">
					<xsl:choose>
						<xsl:when test="substring-before(/project/message, '&#10;')!=''">
							<xsl:value-of select="substring-before(/project/message, '&#10;')"/>
							<xsl:text>...</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="/project/message"/>
						</xsl:otherwise>
					</xsl:choose>
				</h4>
			</xsl:if>
			
			<div class="build-stats">
				<dl>
					<xsl:if test="/project/build-reason">
						<dt><xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.reason')"/></dt>
						<dd><xsl:value-of select="/project/build-reason"/></dd>
					</xsl:if>
					
					<dt><xsl:value-of select="vulcan:getMessage($messageSource, 'label.update.type')"/></dt>
					<dd><xsl:value-of select="/project/update-type"/></dd>
					
					<xsl:if test="/project/build-requested-by">
						<dt><xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.request.username')"/></dt>
						<dd><xsl:value-of select="/project/build-requested-by"/></dd>
					</xsl:if>
								
					<xsl:if test="/project/build-scheduled-by">
						<dt><xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.scheduled.by')"/></dt>
						<dd><xsl:value-of select="/project/build-scheduled-by"/></dd>
					</xsl:if>
					
					<dt><xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.elapsed.time')"/></dt>
					<dd><xsl:value-of select="/project/elapsed-time"/></dd>
					
					<xsl:if test="/project/last-good-build-number">
						<dt><xsl:value-of select="vulcan:getMessage($messageSource, 'label.last.good.build.number')"/></dt>
						<dd>
							<xsl:call-template name="buildLink">
								<xsl:with-param name="buildNumber" select="/project/last-good-build-number"/>
							</xsl:call-template>
						</dd>
					</xsl:if>
				</dl>
				
				<table id="revisions">
					<caption><xsl:value-of select="vulcan:getMessage($messageSource, 'captions.revisions')"/></caption>
					<thead>
						<tr>
							<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.project')"/></th>
							<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.build.number')"/></th>
							<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.revision')"/></th>
							<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.repository.tag.name')"/></th>
							<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.timestamp')"/></th>
							<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.status')"/></th>
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
				
				<xsl:if test="/project/build-log-available">
					<a href="log" class="external">
						<xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.log')"/>
					</a>
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
	
	<xsl:template match="/project/reports">
		<table>
			<caption>Reports</caption>
			<tbody>
				<xsl:for-each select="./report">
					<tr>
						<td>
							<a target="iframe" class="report-link">
								<xsl:attribute name="href">
									<xsl:text>site/</xsl:text>
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
			<xsl:value-of select="vulcan:getMessage($messageSource, 'label.work.directory')"/>
			<xsl:text> </xsl:text>
			<span id="build-directory-root"><xsl:value-of select="."/></span>
			<span id="build-directory-bread-crumbs"><xsl:text> </xsl:text></span>
		</div>
	</xsl:template>
	
	<xsl:template name="currently-building">
		<span class="warning" xmlns="http://www.w3.org/1999/xhtml">
			<xsl:value-of select="vulcan:getMessage($messageSource, 'messages.project.currently.building')"/>
		</span>
	</xsl:template>
	
	<xsl:template match="next-build-number">
		<xsl:call-template name="buildLink">
			<xsl:with-param name="buildNumber" select="."/>
			<xsl:with-param name="text">
				<xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.next')"/>
			</xsl:with-param>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="previous-build-number">
		<xsl:call-template name="buildLink">
			<xsl:with-param name="buildNumber" select="."/>
			<xsl:with-param name="text">
				<xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.prev')"/>
			</xsl:with-param>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template match="change-sets">
		<div xmlns="http://www.w3.org/1999/xhtml" id="changes-panel" class="tab-panel">
			<table class="sortable">
				<caption class="panel-caption">Commit Log (<xsl:value-of select="$num-changes"/>)</caption>
				<thead>
					<tr>
						<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.revision')"/></th>
						<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.author')"/></th>
						<th class="timestamp"><xsl:value-of select="vulcan:getMessage($messageSource, 'th.commit.timestamp')"/></th>
						<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.message')"/></th>
						<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.paths')"/></th>
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
							<a href="diff" class="external">
								<xsl:value-of select="vulcan:getMessage($messageSource, 'label.diff')"/>
							</a>
						</li>
					</xsl:if>
					<xsl:if test="/project/repository-url">
						<li>
							<xsl:element name="a">
								<xsl:attribute name="href"><xsl:value-of select="/project/repository-url"/></xsl:attribute>
								<xsl:attribute name="class">external</xsl:attribute>
								<xsl:value-of select="vulcan:getMessage($messageSource, 'label.repository.url')"/>
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
						<xsl:attribute name="href">
							<xsl:value-of select="$viewProjectStatusURL"/>
							<xsl:value-of select="@name"/>
							<xsl:text>/</xsl:text>
							<xsl:value-of select="@build-number"/>
							<xsl:text>/</xsl:text>
						</xsl:attribute>
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
			<xsl:call-template name="build-messages">
				<xsl:with-param name="caption" select="'Build Errors'"/>
			</xsl:call-template>
		</div>
	</xsl:template>
	
	<xsl:template match="/project/warnings">
		<div xmlns="http://www.w3.org/1999/xhtml" id="warnings-panel" class="tab-panel">
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
			<table>
				<caption class="panel-caption"><xsl:value-of select="vulcan:getMessage($messageSource, 'label.metrics')"/></caption>
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
		<div class="tab-panel" id="tests-panel" xmlns="http://www.w3.org/1999/xhtml">
			<xsl:if test="count(/project/metrics[@key='vulcan.metrics.tests.executed']) = 0">
				<span class="warning">
					<xsl:value-of select="vulcan:getMessage($messageSource, 'messages.tests.carried.over')"/>
				</span>
			</xsl:if>			
			<table>
				<caption class="panel-caption"><xsl:value-of select="vulcan:getMessage($messageSource, 'label.test.failures')"/></caption>
				<thead>
					<tr>
						<th><xsl:value-of select="vulcan:getMessage($messageSource, 'label.test.failure.build.number')"/></th>
						<th class="long"><xsl:value-of select="vulcan:getMessage($messageSource, 'label.test.failure.name')"/></th>
					</tr>
				</thead>
				<tbody>
					<xsl:for-each select="./test-failure">
						<tr>
							<td class="build-number">
								<xsl:choose>
									<xsl:when test="@first-build = /project/build-number">
										<span class="new-test-failure">
											<xsl:value-of select="vulcan:getMessage($messageSource, 'label.test.failure.new')"/>
										</span>
									</xsl:when>
									<xsl:otherwise>
										<xsl:call-template name="buildLink">
											<xsl:with-param name="buildNumber" select="@first-build"/>
										</xsl:call-template>
									</xsl:otherwise>
								</xsl:choose>
							</td>
							<td>
								<xsl:if test="@namespace!=''">
									<span class="test-namespace">
										<xsl:value-of select="@namespace"/>
										<xsl:text>.</xsl:text>
									</span>
								</xsl:if>
								<span class="test-name"><xsl:value-of select="@name"/></span>
								
								<xsl:if test="@message">
									<!-- Want word wrap but preserve line breaks too.  Preformatted text does not do that. -->
									<p class="test-failure-message">
										<xsl:call-template name="replace-string">
											<xsl:with-param name="text" select="@message"/>
											<xsl:with-param name="from" select="'&#10;'"/>
											<xsl:with-param name="to" select="'&lt;br/&gt;'"/>
										</xsl:call-template>
									</p>
								</xsl:if>
								
								<xsl:if test="text()!=''">
									<pre class="test-failure-details">
										<xsl:value-of select="text()"/>
									</pre>
								</xsl:if>
								
							</td>
						</tr>
					</xsl:for-each>
				</tbody>
			</table>
		</div>
	</xsl:template>
	
</xsl:stylesheet>