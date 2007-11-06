<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
	<xsl:output method="xml" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>

	<xsl:template match="/">
		<metrics>
			<xsl:apply-templates select="//report/stats"/>
			<xsl:apply-templates select="//report/data/all"/>
			<xsl:apply-templates select="//coverageReport2/project"/>
		</metrics>
	</xsl:template>
	
	<!-- NCover (http://ncover.org) -->
	<xsl:template match="//coverageReport2/project">
		<metric key="vulcan.metrics.source.classes">
			<xsl:attribute name="value"><xsl:value-of select="@classes"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.methods">
			<xsl:attribute name="value"><xsl:value-of select="@totalFunctions"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.files">
			<xsl:attribute name="value"><xsl:value-of select="@files"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.lines">
			<xsl:attribute name="value"><xsl:value-of select="@nonCommentLines"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.coverage.block">
			<xsl:attribute name="value"><xsl:value-of select="format-number(@coverage, '#.##')"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.coverage.method">
			<xsl:attribute name="value"><xsl:value-of select="format-number(@functionCoverage, '#.##')"/></xsl:attribute>
		</metric>
	</xsl:template>
	
	<!-- Emma (http://emma.sourceforge.net) -->
	<xsl:template match="//report/stats">
		<metric key="vulcan.metrics.source.packages">
			<xsl:attribute name="value"><xsl:value-of select="packages/@value"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.classes">
			<xsl:attribute name="value"><xsl:value-of select="classes/@value"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.methods">
			<xsl:attribute name="value"><xsl:value-of select="methods/@value"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.files">
			<xsl:attribute name="value"><xsl:value-of select="srcfiles/@value"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.lines">
			<xsl:attribute name="value"><xsl:value-of select="srclines/@value"/></xsl:attribute>
		</metric>
	</xsl:template>
	
	<xsl:template match="//report/data/all">
		<xsl:call-template name="emma-coverage">
			<xsl:with-param name="type" select="'class'"/>
			<xsl:with-param name="node" select="coverage[@type='class, %']"/>
		</xsl:call-template>
		<xsl:call-template name="emma-coverage">
			<xsl:with-param name="type" select="'method'"/>
			<xsl:with-param name="node" select="coverage[@type='method, %']"/>
		</xsl:call-template>
		<xsl:call-template name="emma-coverage">
			<xsl:with-param name="type" select="'block'"/>
			<xsl:with-param name="node" select="coverage[@type='block, %']"/>
		</xsl:call-template>
		<xsl:call-template name="emma-coverage">
			<xsl:with-param name="type" select="'line'"/>
			<xsl:with-param name="node" select="coverage[@type='line, %']"/>
		</xsl:call-template>
	</xsl:template>
	
	<xsl:template name="emma-coverage">
		<xsl:param name="type"/>
		<xsl:param name="node"/>
		
		<metric>
			<xsl:attribute name="key">
				<xsl:text>vulcan.metrics.coverage.</xsl:text>
				<xsl:value-of select="$type"/>
			</xsl:attribute>
			<xsl:attribute name="value">
				<xsl:value-of select="substring-before($node/@value, ' ')"/>
			</xsl:attribute>
		</metric>
	</xsl:template>
</xsl:stylesheet>