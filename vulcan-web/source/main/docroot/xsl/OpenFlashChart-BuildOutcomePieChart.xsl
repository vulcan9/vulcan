<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
	<xsl:output method="text" media-type="text/plain" version="1.0"
		encoding="UTF-8" omit-xml-declaration="yes"/>
	
	<xsl:strip-space elements="*"/>
	
	<xsl:param name="viewProjectStatusURL"/>
	
	<xsl:key name="builds-by-outcome" match="/build-history/project" use="status"/>
	
	<xsl:variable name="totalSamples" select="count(/build-history/project)"/>
	
	<xsl:template match="/build-history">
		<xsl:text>&amp;title=Success Rate,{color: #7E97A6; font-size: 20; text-align:left}&amp;</xsl:text>
		<xsl:text>&amp;bg_colour=#FFFFFF&amp;</xsl:text>
		<xsl:text>&amp;pie=60,#FFFFFF,#000000&amp;</xsl:text>
		<xsl:text>&amp;colours=#43F143,#FF0000,#C0C000,#FFA500&amp;</xsl:text>
		<xsl:text>&amp;tool_tip=#val#%25&amp;</xsl:text>
		
		<xsl:text>&amp;pie_labels=</xsl:text>
		<xsl:for-each select="project[generate-id() = generate-id(key('builds-by-outcome', status)[1])]">
			<xsl:if test="position() != 1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:text><xsl:value-of select="status"/></xsl:text>
		</xsl:for-each>
		<xsl:text>&amp;</xsl:text>

		<xsl:text>&amp;values=</xsl:text>		
		<xsl:for-each select="project[generate-id() = generate-id(key('builds-by-outcome', status)[1])]">
			<xsl:if test="position() != 1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:text><xsl:value-of select="format-number(count(key('builds-by-outcome', status)) div $totalSamples * 100, '#.##')"/></xsl:text>
		</xsl:for-each>
		<xsl:text>&amp;</xsl:text>
	</xsl:template>
</xsl:stylesheet>
