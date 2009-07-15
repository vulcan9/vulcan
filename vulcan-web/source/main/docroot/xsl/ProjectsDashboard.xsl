<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet 
	version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:vulcan="xalan://net.sourceforge.vulcan.web.XslHelper"
	extension-element-prefixes="vulcan"
	exclude-result-prefixes="vulcan">
	
	<xsl:output method="xml" media-type="application/xml" omit-xml-declaration="yes"/>

	<xsl:include href="common.xsl"/>
		
	<xsl:param name="contextRoot"/>
	<xsl:param name="sortUrl"/>
	<xsl:param name="detailLink"/>
	<xsl:param name="sortSelect"/>
	<xsl:param name="sortOrder"/>

	<xsl:variable name="sortOrder1">
		<xsl:choose>
			<xsl:when test="$sortOrder='descending'">descending</xsl:when>
		<xsl:otherwise>ascending</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
	
	<xsl:variable name="opposite">
		<xsl:choose>
			<xsl:when test="$sortOrder='descending'">ascending</xsl:when>
		<xsl:otherwise>descending</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
		
	<xsl:template match="/">
		<table class="projects dashboard">
			<caption>
				<xsl:value-of select="vulcan:getMessage($messageSource, 'captions.projects.status')"/>
			</caption>
			<thead>
				<tr>
					<xsl:for-each select="projects/visible-columns/label">
						<th class="sortable">
							<a>
								<xsl:choose>
									<xsl:when test="$sortSelect=text()">
										<xsl:attribute name="class">sorted-<xsl:value-of select="$sortOrder1"/></xsl:attribute>
										<xsl:attribute name="href">
											<xsl:value-of select="$sortUrl"/>
											<xsl:text>&amp;config.sortColumn=</xsl:text>
											<xsl:value-of select="."/>
											<xsl:text>&amp;config.sortOrder=</xsl:text>
											<xsl:value-of select="$opposite"/>
										</xsl:attribute>
									</xsl:when>
									<xsl:otherwise>
										<xsl:attribute name="href">
											<xsl:value-of select="$sortUrl"/>
											<xsl:text>&amp;config.sortColumn=</xsl:text>
											<xsl:value-of select="."/>
											<xsl:text>&amp;config.sortOrder=ascending</xsl:text>
										</xsl:attribute>
									</xsl:otherwise>
								</xsl:choose>
								<xsl:value-of select="vulcan:getMessage($messageSource, text())"/>
							</a>
						</th>
					</xsl:for-each>
				</tr>
			</thead>
			<tbody>
				<xsl:choose>
					<xsl:when test="$sortSelect='dashboard.columns.age'">
						<xsl:apply-templates select="/projects/project">
							<xsl:sort select="timestamp/@millis" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="$sortSelect='dashboard.columns.failure-age'">
						<xsl:apply-templates select="/projects/project">
							<xsl:sort select="first-failure/elapsed-time/@millis" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="$sortSelect='dashboard.columns.revision'">
						<xsl:apply-templates select="/projects/project">
							<xsl:sort select="revision/@numeric" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="$sortSelect='dashboard.columns.build-number'">
						<xsl:apply-templates select="/projects/project">
							<xsl:sort select="build-number" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<xsl:variable name="metric-type" select="/projects/project/metrics/metric[@key=$sortSelect]/@type"/>
						<xsl:choose>
							<xsl:when test="$metric-type = 'PERCENT' or $metric-type = 'NUMBER'">
								<xsl:apply-templates select="/projects/project">
									<xsl:sort select="*[name()=substring-after($sortSelect, 'dashboard.columns.')] | metrics/metric[@key=$sortSelect]/@value" order="{$sortOrder1}" data-type="number"/>
								</xsl:apply-templates>
							</xsl:when>
							<xsl:otherwise>
								<xsl:apply-templates select="/projects/project">
									<xsl:sort select="*[name()=substring-after($sortSelect, 'dashboard.columns.')] | metrics/metric[@key=$sortSelect]/@value" order="{$sortOrder1}" data-type="text"/>
								</xsl:apply-templates>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</tbody>
		</table>
	</xsl:template>
	
	<xsl:template match="project">
		<xsl:variable name="project" select="."/>
		<tr>
			<xsl:for-each select="/projects/visible-columns/label">
				<xsl:variable name="col" select="."/>
				<td>
					<xsl:choose>
						<xsl:when test="$col='dashboard.columns.name'">
							<a>
								<xsl:attribute name="href">
									<xsl:call-template name="concat-link">
										<xsl:with-param name="prefix" select="$detailLink"/>
										<xsl:with-param name="suffix" select="concat($project/@name, '/LATEST/')"/>
									</xsl:call-template>
								</xsl:attribute>
								<xsl:value-of select="$project/@name"/>
							</a>
						</xsl:when>
						<xsl:when test="$col='dashboard.columns.age'">
							<xsl:attribute name="title">
								<xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.timestamp', $project/timestamp)"/>
							</xsl:attribute>
							<xsl:value-of select="$project/timestamp/@age"/>
						</xsl:when>
						<xsl:when test="$col='dashboard.columns.failure-age'">
							<xsl:value-of select="$project/first-failure/elapsed-time/@age"/>
						</xsl:when>
						<xsl:when test="$col='dashboard.columns.status'">
							<xsl:attribute name="title"><xsl:value-of select="$project/message"/></xsl:attribute>
							<xsl:attribute name="class">
								<xsl:value-of select="$project/status"/>
								<xsl:text> status</xsl:text>
							</xsl:attribute>
							<xsl:value-of select="$project/status"/>
							<xsl:if test="$project/locked/text() = 'true'">
								<img alt="Locked">
									<xsl:attribute name="src">
										<xsl:value-of select="$contextRoot"/>
										<xsl:text>images/lock.png</xsl:text>
									</xsl:attribute>
									<xsl:attribute name="title">
										<xsl:value-of select="$project/locked/@message"/>
									</xsl:attribute>
								</img>
							</xsl:if>
						</xsl:when>
						<xsl:otherwise>
							<xsl:value-of select="$project/*[name()=substring-after($col, 'dashboard.columns.')] | $project/metrics/metric[@key=$col]/@value"/>
						</xsl:otherwise>
					</xsl:choose>
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>
	
	<xsl:template name="concat-link">
		<xsl:param name="prefix"/>
		<xsl:param name="suffix"/>
		
		<xsl:choose>
			<xsl:when test="substring-before($prefix, ';jsessionid=')">
				<xsl:value-of select="substring-before($prefix, ';jsessionid=')"/>
				<xsl:value-of select="$suffix"/>
				<xsl:text>;jsessionid=</xsl:text>
				<xsl:value-of select="substring-after($prefix, ';jsessionid=')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat($prefix, $suffix)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
