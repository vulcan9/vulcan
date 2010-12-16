<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
	<xsl:output method="xml" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>

	<xsl:template match="/">
		<metrics>
			<xsl:apply-templates select="//metric[@key!='' and @value!='' and @type!='']"/>
		</metrics>
	</xsl:template>
	
	<xsl:template match="metric">
		<metric>
			<xsl:attribute name="key"><xsl:value-of select="@key"/></xsl:attribute>
			<xsl:attribute name="value"><xsl:value-of select="@value"/></xsl:attribute>
			<xsl:attribute name="type"><xsl:value-of select="@type"/></xsl:attribute>
		</metric>
	</xsl:template>
</xsl:stylesheet>