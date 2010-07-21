<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:vulcan="xalan://net.sourceforge.vulcan.web.XslHelper"
	xmlns:datetime="http://exslt.org/dates-and-times"
	extension-element-prefixes="vulcan datetime"
	exclude-result-prefixes="vulcan datetime">
	
	<xsl:output method="xml" media-type="application/rss+xml;charset=UTF-8" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>
	
	<xsl:include href="common.xsl"/>
	
	<xsl:param name="preferences"/>
	<xsl:param name="contextRoot"/>
	<xsl:param name="viewProjectStatusURL"/>
	
	<xsl:key name="metrics-labels" match="/build-history/project/metrics/metric" use="@label"/>
	
	<xsl:variable name="rss-date-format" select="'EEE, dd MMM yyyy HH:mm:ss Z'"/>
	
	<xsl:template match="/build-history">
		<rss version="2.0">
			<channel>
				<title>RSS</title>
				<description>Build results for projects named X</description>
				<link><xsl:value-of select="$viewProjectStatusURL"/></link>
	
				<xsl:apply-templates select="project">
					<xsl:sort select="timestamp/@millis" data-type="number" order="descending"/>
				</xsl:apply-templates>
			</channel>
		</rss>
	</xsl:template>
	
	<xsl:template match="project">
		<item>
			<title>
				<xsl:value-of select="name"/>
				<xsl:text> - </xsl:text>
				<xsl:value-of select="status"/>
				<xsl:text> - Build </xsl:text>
				<xsl:value-of select="build-number"/>
			</title>
			<pubDate><xsl:value-of select="datetime:formatDate(timestamp, $rss-date-format)"/></pubDate>
			<link>
				<xsl:value-of select="$viewProjectStatusURL"/>
				<xsl:value-of select="name"/>
				<xsl:text>/</xsl:text>
				<xsl:value-of select="build-number"/>
				<xsl:text>/</xsl:text>
			</link>
			<description>
				<xsl:text>&lt;p&gt;</xsl:text>
				<xsl:value-of select="message"/>
				<xsl:text>&lt;/p&gt;</xsl:text>
				<xsl:text>&lt;p&gt;</xsl:text>
				<xsl:value-of select="build-reason"/>
				<xsl:text>&lt;/p&gt;</xsl:text>
			</description>
		</item>
	</xsl:template>
</xsl:stylesheet>