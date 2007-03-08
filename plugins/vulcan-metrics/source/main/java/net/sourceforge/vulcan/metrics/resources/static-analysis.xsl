<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
	<xsl:output method="xml" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>

	<xsl:variable name="fxCopInformationals" select="count(//FxCopReport//Issue[@Level='Informational'])"/>
	<xsl:variable name="fxCopWarnings" select="count(//FxCopReport//Issue[@Level='Warning'])"/>
	<xsl:variable name="fxCopCriticalWarnings" select="count(//FxCopReport//Issue[@Level='CriticalWarning'])"/>
	<xsl:variable name="fxCopErrors" select="count(//FxCopReport//Issue[@Level='Error'])"/>
	<xsl:variable name="fxCopCriticalErrors" select="count(//FxCopReport//Issue[@Level='CriticalError'])"/>
	
	<xsl:template match="/">
		<metrics>
			<xsl:if test="//FxCopReport">
				<metric key="vulcan.metrics.fxcop.informational">
					<xsl:attribute name="value"><xsl:value-of select="$fxCopInformationals"/></xsl:attribute>
				</metric>
				<metric key="vulcan.metrics.fxcop.warnings">
					<xsl:attribute name="value"><xsl:value-of select="$fxCopWarnings"/></xsl:attribute>
				</metric>
				<metric key="vulcan.metrics.fxcop.critical.warnings">
					<xsl:attribute name="value"><xsl:value-of select="$fxCopCriticalWarnings"/></xsl:attribute>
				</metric>
				<metric key="vulcan.metrics.fxcop.errors">
					<xsl:attribute name="value"><xsl:value-of select="$fxCopErrors"/></xsl:attribute>
				</metric>
				<metric key="vulcan.metrics.fxcop.critical.errors">
					<xsl:attribute name="value"><xsl:value-of select="$fxCopCriticalErrors"/></xsl:attribute>
				</metric>
			</xsl:if>
		</metrics>
	</xsl:template>
</xsl:stylesheet>