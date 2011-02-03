<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:vulcan="xalan://net.sourceforge.vulcan.web.XslHelper"
	extension-element-prefixes="vulcan"
	exclude-result-prefixes="vulcan">
	
	<xsl:include href="common.xsl"/>
	
	<xsl:output method="xml" media-type="text/html" version="1.0"
		encoding="UTF-8" omit-xml-declaration="yes"/>
	
	<xsl:param name="title"/>
	<xsl:param name="viewProjectStatusURL"/>
	<xsl:param name="issueTrackerURL"/>
	
	<xsl:variable name="num-errors" select="count(/project/errors/error)"/>
	<xsl:variable name="num-warnings" select="count(/project/warnings/warning)"/>
	<xsl:variable name="num-changes" select="count(/project/change-sets/change-set)"/>
	<xsl:variable name="num-test-failures" select="count(/project/test-failures/test-failure)"/>
	<xsl:variable name="work-directory" select="/project/work-directory/text()"/>
	
	<xsl:template match="/project">
		<html xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US">
			<head>
				<style type="text/css">
					body {
						font-family:sans-serif;
					}
					p.meta {
						font-size:12px;
					}
					p.broken-by {
						background-color: yellow;
						font-weight: bold;
						padding: 1ex;
					}
					a.jump {
						font-size:12px;
					}
					td, th {
						vertical-align:top;
						text-align:left;
						padding: 10px 5px;
					}
					tr {
						border-bottom: solid 1px #ccc;
					}
					ul.modified-paths {
						margin:5px 5px 5px 20px;
						padding-left:15px;
					}
					ul.modified-paths li {
						font-family:monospace;
						font-size:11px;
						white-space:pre;
					}
					ul.modified-paths li.modify:before {
						content: "(M) ";
					}
					ul.modified-paths li.add:before {
						content: "(A) ";
					}
					ul.modified-paths li.add {
						color: green;
					}
					ul.modified-paths li.remove:before {
						content: "(D) ";
					}
					ul.modified-paths li.remove {
						color: red;
						text-decoration: line-through;
					}
					dt {
						color: #b00;
						font-weight: bold;
					}
					dd {
						font-family:monospace;
						padding-bottom:10px;
					}
				</style>
			</head>
			<body>
				<p class="meta">
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
					<xsl:if test="/project/timestamp/@text">
						<xsl:text> - </xsl:text>
						<xsl:value-of select="/project/timestamp/@text"/>
					</xsl:if>
					</p>

				<h1>
					<xsl:apply-templates select="/project/name"/>
					<xsl:text> : </xsl:text>
					<xsl:apply-templates select="/project/status"/>
				</h1>

				<p class="meta">
					<xsl:if test="$num-changes &gt; 0">
						[<a xmlns="http://www.w3.org/1999/xhtml" href="#commit-log">Recent Commits</a>]
					</xsl:if>
					
					<xsl:text>[</xsl:text>
					<a xmlns="http://www.w3.org/1999/xhtml" class="build-link">
						<xsl:attribute name="href">
							<xsl:value-of select="$viewProjectStatusURL"/>
							<xsl:value-of select="/project/name"/>
							<xsl:text>/</xsl:text>
							<xsl:value-of select="/project/build-number"/>
							<xsl:text>/</xsl:text>
						</xsl:attribute>
						<xsl:text>Full Report</xsl:text>
					</a>
					<xsl:text>]</xsl:text>
				</p>

				<xsl:if test="/project/broken-by">
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'broken-by'"/>
					</xsl:call-template>
				</xsl:if>
				
				<xsl:if test="/project/errors">
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'errors'"/>
					</xsl:call-template>
				</xsl:if>

				<xsl:if test="/project/test-failures">
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'test-failures'"/>
					</xsl:call-template>
				</xsl:if>
				<xsl:if test="/project/change-sets">
					<xsl:call-template name="bubble">
						<xsl:with-param name="target" select="'changes'"/>
					</xsl:call-template>
				</xsl:if>
			</body>
		</html>
	</xsl:template>

	<xsl:template name="bubble">
		<xsl:param name="target"/>
		<hr xmlns="http://www.w3.org/1999/xhtml" />
		<xsl:choose>
			<xsl:when test="$target='broken-by'">
				<xsl:apply-templates select="/project/broken-by"/>
			</xsl:when>
			<xsl:when test="$target='changes'">
				<xsl:apply-templates select="/project/change-sets"/>
			</xsl:when>
			<xsl:when test="$target='errors'">
				<xsl:apply-templates select="/project/errors"/>
			</xsl:when>
			<xsl:when test="$target='test-failures'">
				<xsl:apply-templates select="/project/test-failures"/>
			</xsl:when>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="broken-by">
		<p class="broken-by">
			<xsl:value-of select="vulcan:getMessage($messageSource, 'messages.broken.by', .)"/>
		</p>
	</xsl:template>
	
	<xsl:template match="change-sets">
		<h3 id="commit-log">Commit Log (<xsl:value-of select="$num-changes"/>)</h3>
		<table>
			<thead>
				<tr>
					<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.author')"/></th>
					<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.revision')"/></th>
					<th><xsl:value-of select="vulcan:getMessage($messageSource, 'th.message')"/></th>
				</tr>
			</thead>
			<tbody>
				<xsl:for-each select="change-set">
					<xsl:sort select="./timestamp/@millis" order="ascending" data-type="number"/>
					<tr>
						<td><xsl:apply-templates select="@author"/></td>
						<td><xsl:apply-templates select="@revision"/></td>
						<td class="commit-message"><xsl:apply-templates select="message"/>
							<ul class="modified-paths">
							<xsl:for-each select="./modified-paths/path">
								<xsl:sort select="." order="ascending" data-type="text"/>
								<li>
									<xsl:if test="@action">
										<xsl:attribute name="class">
											<xsl:value-of select="@action"/>
										</xsl:attribute>
									</xsl:if>

									<xsl:apply-templates select="."/>
								</li>
							</xsl:for-each>
							</ul>
						</td>
					</tr>
				</xsl:for-each>
			</tbody>
			</table>
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
	
	<xsl:template match="/project/errors">
		<div xmlns="http://www.w3.org/1999/xhtml" id="errors-panel" class="tab-panel">
			<a name="errors-panel"/>
			<xsl:call-template name="build-messages">
				<xsl:with-param name="caption" select="'Build Errors'"/>
			</xsl:call-template>
		</div>
	</xsl:template>
	
	<xsl:template name="build-messages">
		<xsl:param name="caption" select="'Build Messages'"/>
		
		<xsl:param name="showFiles" select="./*/@file"/>
		<xsl:param name="showLineNumbers" select="./*/@line-number"/>
		<xsl:param name="showCode" select="./*/@code"/>
		<xsl:param name="showTableHead" select="$showFiles or $showLineNumbers or $showCode"/>

		<h2><xsl:value-of select="$caption"/></h2>
		<dl xmlns="http://www.w3.org/1999/xhtml">
		<xsl:for-each select="./*">
			<dt><xsl:value-of select="."/></dt>
			<xsl:if test="$showFiles">
				<dd>
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
				</dd>
			</xsl:if>
		</xsl:for-each>
		</dl>
	</xsl:template>
	
	<xsl:template match="/project/test-failures">
		<h2><xsl:value-of select="vulcan:getMessage($messageSource, 'label.test.failures')"/></h2>
		<dl xmlns="http://www.w3.org/1999/xhtml">
		<xsl:for-each select="./test-failure">
				<dt>
					<xsl:if test="@namespace!=''">
						<span class="test-namespace">
							<xsl:value-of select="@namespace"/>
							<xsl:text>.</xsl:text>
						</span>
					</xsl:if>
					<span class="test-name"><xsl:value-of select="@name"/></span>
					
					<xsl:if test="@message">
						<!-- Want word wrap but preserve line breaks too.  Preformatted text does not do that. -->
						<p style="color:black;padding-left:10px;font-size:12px">
							<xsl:call-template name="replace-string">
								<xsl:with-param name="text" select="@message"/>
								<xsl:with-param name="from" select="'&#10;'"/>
								<xsl:with-param name="to" select="'&lt;br /&gt;'"/>
							</xsl:call-template>
						</p>
					</xsl:if>
				</dt>
				<xsl:if test="text()!=''">
					<dd>
						<xsl:value-of select="text()"/>
					</dd>
				</xsl:if>
		</xsl:for-each>
		</dl>
	</xsl:template>
	
</xsl:stylesheet>