<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
	<xsl:output method="text" media-type="text/plain" version="1.0"
		encoding="UTF-8" omit-xml-declaration="yes"/>
	
	<xsl:strip-space elements="*"/>
	
	<xsl:key name="builds-by-outcome" match="/build-history/project" use="status"/>
	
	<xsl:variable name="totalSamples" select="count(/build-history/project)"/>
	
	<xsl:template match="/build-history">
		<xsl:text>&amp;title=,{color: #7E97A6; font-size: 2; text-align:left}&amp;</xsl:text>
		<xsl:text>&amp;bg_colour=#FFFFFF&amp;</xsl:text>
		<xsl:text>&amp;pie=90,#FFFFFF,#FFFFFF,false&amp;</xsl:text>
		
		<xsl:text>&amp;colours=</xsl:text>
		<xsl:for-each select="project[generate-id() = generate-id(key('builds-by-outcome', status)[1])]">
			<xsl:if test="position() != 1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:choose>
				<xsl:when test="status='PASS'">
					<xsl:text>#43F143</xsl:text>
				</xsl:when>
				<xsl:when test="status='FAIL'">
					<xsl:text>#FF0000</xsl:text>
				</xsl:when>
				<xsl:when test="status='ERROR'">
					<xsl:text>#C0C000</xsl:text>
				</xsl:when>
				<xsl:when test="status='SKIP'">
					<xsl:text>#FFA500</xsl:text>
				</xsl:when>
			</xsl:choose>		
		</xsl:for-each>
		<xsl:text>&amp;</xsl:text>
		
		<xsl:text>&amp;tool_tip=#val#%25 (#x_label#)&amp;</xsl:text>
		
		<xsl:text>&amp;pie_labels=</xsl:text>
		<xsl:for-each select="project[generate-id() = generate-id(key('builds-by-outcome', status)[1])]">
			<xsl:if test="position() != 1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:value-of select="status"/>
		</xsl:for-each>
		<xsl:text>&amp;</xsl:text>

		<xsl:text>&amp;values=</xsl:text>		
		<xsl:for-each select="project[generate-id() = generate-id(key('builds-by-outcome', status)[1])]">
			<xsl:if test="position() != 1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:value-of select="format-number(count(key('builds-by-outcome', status)) div $totalSamples * 100, '#.##')"/>
		</xsl:for-each>
		<xsl:text>&amp;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
