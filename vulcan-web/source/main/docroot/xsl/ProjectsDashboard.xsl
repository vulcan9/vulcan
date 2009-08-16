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
	<xsl:param name="sortSelect"/>
	<xsl:param name="sortOrder"/>
	<xsl:param name="detailLink"/>
	
	<xsl:variable name="sortOrder1">
		<xsl:choose>
			<xsl:when test="$sortOrder='descending'">descending</xsl:when>
			<xsl:otherwise>ascending</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>

	<xsl:variable name="visibleColumns" select="/projects/visible-columns/label"/>
	
	
	<xsl:template match="/">
		<table class="projects dashboard">
			<caption>
				<xsl:value-of select="vulcan:getMessage($messageSource, 'captions.projects.status')"/>
			</caption>
			
			<xsl:call-template name="project-column-headers">
				<xsl:with-param name="columns" select="$visibleColumns"/>
				<xsl:with-param name="visible-columns" select="$visibleColumns"/>
				<xsl:with-param name="sortSelect" select="$sortSelect"/>
				<xsl:with-param name="sortOrder" select="$sortOrder1"/>
				<xsl:with-param name="sortUrl" select="$sortUrl"/>
			</xsl:call-template>
			
			<tbody>
				<xsl:choose>
					<xsl:when test="$sortSelect='dashboard.columns.age'">
						<xsl:apply-templates select="/projects/project">
							<xsl:with-param name="columns" select="$visibleColumns"/>
							<xsl:with-param name="visible-columns" select="$visibleColumns"/>
							<xsl:with-param name="detailLink" select="$detailLink"/>
							<xsl:sort select="timestamp/@millis" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="$sortSelect='dashboard.columns.failure-age'">
						<xsl:apply-templates select="/projects/project">
							<xsl:with-param name="columns" select="$visibleColumns"/>
							<xsl:with-param name="visible-columns" select="$visibleColumns"/>
							<xsl:with-param name="detailLink" select="$detailLink"/>
							<xsl:sort select="first-failure/elapsed-time/@millis" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="$sortSelect='dashboard.columns.revision'">
						<xsl:apply-templates select="/projects/project">
							<xsl:with-param name="columns" select="$visibleColumns"/>
							<xsl:with-param name="visible-columns" select="$visibleColumns"/>
							<xsl:with-param name="detailLink" select="$detailLink"/>
							<xsl:sort select="revision/@numeric" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="$sortSelect='dashboard.columns.build-number'">
						<xsl:apply-templates select="/projects/project">
							<xsl:with-param name="columns" select="$visibleColumns"/>
							<xsl:with-param name="visible-columns" select="$visibleColumns"/>
							<xsl:with-param name="detailLink" select="$detailLink"/>
							<xsl:sort select="build-number" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<xsl:variable name="metric-type" select="/projects/project/metrics/metric[@key=$sortSelect]/@type"/>
						<xsl:choose>
							<xsl:when test="$metric-type = 'PERCENT' or $metric-type = 'NUMBER'">
								<xsl:apply-templates select="/projects/project">
									<xsl:with-param name="columns" select="$visibleColumns"/>
									<xsl:with-param name="visible-columns" select="$visibleColumns"/>
									<xsl:with-param name="detailLink" select="$detailLink"/>
									<xsl:sort select="*[name()=substring-after($sortSelect, 'dashboard.columns.')] | metrics/metric[@key=$sortSelect]/@value" order="{$sortOrder1}" data-type="number"/>
								</xsl:apply-templates>
							</xsl:when>
							<xsl:otherwise>
								<xsl:apply-templates select="/projects/project">
									<xsl:with-param name="columns" select="$visibleColumns"/>
									<xsl:with-param name="visible-columns" select="$visibleColumns"/>
									<xsl:with-param name="detailLink" select="$detailLink"/>
									<xsl:sort select="*[name()=substring-after($sortSelect, 'dashboard.columns.')] | metrics/metric[@key=$sortSelect]/@value" order="{$sortOrder1}" data-type="text"/>
								</xsl:apply-templates>
							</xsl:otherwise>
						</xsl:choose>
					</xsl:otherwise>
				</xsl:choose>
			</tbody>
		</table>
	</xsl:template>
</xsl:stylesheet>
