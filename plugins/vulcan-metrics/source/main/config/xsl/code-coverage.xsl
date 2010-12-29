<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
	<xsl:output method="xml" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>

	<xsl:key name="cobertura-filenames" match="/*/coverage/packages/package/classes/class" use="@filename"/>
	
	<xsl:template match="/">
		<metrics>
			<xsl:apply-templates select="/*/report/stats"/>
			<xsl:apply-templates select="/*/report/data/all"/>
			<xsl:apply-templates select="/*/coverageReport2/project"/>
			
			<xsl:if test="/*/coverage/packages">
				<xsl:call-template name="cobertura"/>
			</xsl:if>
		</metrics>
	</xsl:template>
	
	<!-- NCover (http://ncover.org) -->
	<xsl:template match="/*/coverageReport2/project">
		<metric key="vulcan.metrics.source.classes" type="number">
			<xsl:attribute name="value"><xsl:value-of select="@classes"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.methods" type="number">
			<xsl:attribute name="value"><xsl:value-of select="@totalFunctions"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.files" type="number">
			<xsl:attribute name="value"><xsl:value-of select="@files"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.lines" type="number">
			<xsl:attribute name="value"><xsl:value-of select="@nonCommentLines"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.coverage.block" type="percent">
			<xsl:attribute name="value"><xsl:value-of select="@coverage div 100"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.coverage.method" type="percent">
			<xsl:attribute name="value"><xsl:value-of select="@functionCoverage div 100"/></xsl:attribute>
		</metric>
	</xsl:template>
	
	<!--  Cobertura (http://cobertura.sourceforge.net) -->
	<xsl:template name="cobertura">
		<xsl:variable name="totalLines" select="count(/*/coverage/packages/package/classes/class/lines/line)"/>
		<xsl:variable name="hitLines" select="count(/*/coverage/packages/package/classes/class/lines/line[@hits &gt; 0])"/>

		<xsl:variable name="totalBranchLines" select="count(/*/coverage/packages/package/classes/class/lines/line[@branch='true'])"/>
		<xsl:variable name="hitBranchLines" select="count(/*/coverage/packages/package/classes/class/lines/line[@branch='true' and @hits &gt; 0])"/>
		
		<metric key="vulcan.metrics.source.packages" type="number">
			<xsl:attribute name="value"><xsl:value-of select="count(/*/coverage/packages/package)"/></xsl:attribute>
		</metric>
			
		<metric key="vulcan.metrics.source.files" type="number">
			<xsl:attribute name="value"><xsl:value-of select="count(/*/coverage/packages/package/classes/class[generate-id() = generate-id(key('cobertura-filenames', @filename)[1])])"/></xsl:attribute>
		</metric>
		
		<metric key="vulcan.metrics.source.classes" type="number">
			<xsl:attribute name="value"><xsl:value-of select="count(/*/coverage/packages/package/classes/class)"/></xsl:attribute>
		</metric>
		
		<metric key="vulcan.metrics.source.methods" type="number">
			<xsl:attribute name="value"><xsl:value-of select="count(/*/coverage/packages/package/classes/class/methods/method)"/></xsl:attribute>
		</metric>
		
		<xsl:if test="$totalLines &gt; 0">
			<metric key="vulcan.metrics.source.lines" type="number">
				<xsl:attribute name="value"><xsl:value-of select="$totalLines"/></xsl:attribute>
			</metric>
			<metric key="vulcan.metrics.coverage.line" type="percent">
				<xsl:attribute name="value"><xsl:value-of select="$hitLines div $totalLines"/></xsl:attribute>
			</metric>
		</xsl:if>
		
		<xsl:if test="count(/*/coverage) = 1">
			<!-- I don't know how branch coverage is calculated so it is only reported
				when a single coverage report is being merged. -->			
			<metric key="vulcan.metrics.coverage.branch" type="percent">
				<xsl:attribute name="value"><xsl:value-of select="/*/coverage/@branch-rate"/></xsl:attribute>
			</metric>
		</xsl:if>
	</xsl:template>
	
	<!-- Emma (http://emma.sourceforge.net) -->
	<xsl:template match="/*/report/stats">
		<metric key="vulcan.metrics.source.packages" type="number">
			<xsl:attribute name="value"><xsl:value-of select="packages/@value"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.classes" type="number">
			<xsl:attribute name="value"><xsl:value-of select="classes/@value"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.methods" type="number">
			<xsl:attribute name="value"><xsl:value-of select="methods/@value"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.files" type="number">
			<xsl:attribute name="value"><xsl:value-of select="srcfiles/@value"/></xsl:attribute>
		</metric>
		<metric key="vulcan.metrics.source.lines" type="number">
			<xsl:attribute name="value"><xsl:value-of select="srclines/@value"/></xsl:attribute>
		</metric>
	</xsl:template>
	
	<xsl:template match="/*/report/data/all">
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
		
		<metric type="percent">
			<xsl:attribute name="key">
				<xsl:text>vulcan.metrics.coverage.</xsl:text>
				<xsl:value-of select="$type"/>
			</xsl:attribute>
			<xsl:attribute name="value">
				<xsl:value-of select="substring-before($node/@value, '%') div 100"/>
			</xsl:attribute>
		</metric>
	</xsl:template>
</xsl:stylesheet>