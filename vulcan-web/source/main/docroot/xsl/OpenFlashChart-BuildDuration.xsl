<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:math="http://exslt.org/math"
	xmlns:vulcan="xalan://net.sourceforge.vulcan.web.XslHelper"
	extension-element-prefixes="vulcan math"
	exclude-result-prefixes="vulcan math">
	
	<xsl:output method="text" media-type="text/plain" version="1.0"
		encoding="UTF-8" omit-xml-declaration="yes"/>

	<xsl:include href="common.xsl"/>
	
	<xsl:strip-space elements="*"/>
	
	<xsl:param name="projectSiteURL"/>
	<xsl:param name="viewProjectStatusURL"/>
	
	<xsl:key name="builds-by-project-name-and-update-type" match="/build-history/project" use="concat(name,'-',update-type)"/>
	<xsl:key name="builds-by-timestamp" match="/build-history/project" use="timestamp/@millis"/>
	
	<xsl:variable name="incrBuildCount" select="count(/build-history/project[update-type = 'Incremental'])"/>
	<xsl:variable name="fullBuildCount" select="count(/build-history/project[update-type = 'Full'])"/>
	
	<xsl:template match="/build-history">
		<xsl:variable name="maxDuration">
			<xsl:for-each select="project/elapsed-time/@millis">
				<xsl:sort data-type="number" select="." order="descending"/>
				<xsl:if test="position()=1">
					<xsl:value-of select="ceiling(. * 1.05 div 60000)"/>
				</xsl:if>
			</xsl:for-each>
		</xsl:variable>
		
		&amp;y_min=0&amp;
		<xsl:text>&amp;y_max=</xsl:text>
		<xsl:value-of select="$maxDuration"/>
		<xsl:text>&amp;</xsl:text>

<xsl:text>
&amp;bg_colour=#FFFFFF&amp;
&amp;x_axis_colour=#818D9D&amp;
&amp;x_grid_colour=#F0F0F0&amp;
&amp;y_axis_colour=#818D9D&amp;
&amp;y_grid_colour=#ADB5C7&amp;

&amp;x_label_style=10,#164166,2,1&amp;

&amp;y_legend=</xsl:text>
<xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.duration')"/>
<xsl:text>,12,#164166&amp;
&amp;y_ticks=5,10,5&amp;
</xsl:text>
		
		<xsl:text>&amp;x_min=</xsl:text>
		<xsl:value-of select="x-axis/minimum/@millis"/>
		<xsl:text>&amp;</xsl:text>
		
		<xsl:text>&amp;x_max=</xsl:text>
		<xsl:value-of select="x-axis/maximum/@millis"/>
		<xsl:text>&amp;</xsl:text>
		
		<xsl:text>&amp;x_labels=</xsl:text>
		
		<xsl:for-each select="x-axis/labels/label">
			<xsl:if test="position()!=1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:text>(</xsl:text>
			<xsl:value-of select="."/>
			<xsl:text>,</xsl:text>
			<xsl:value-of select="@millis"/>
			<xsl:text>)</xsl:text>
		</xsl:for-each>
		<xsl:text>&amp;</xsl:text>
		
		<xsl:for-each select="project[generate-id() = generate-id(key('builds-by-project-name-and-update-type', concat(name,'-',update-type))[1])]">
			<xsl:call-template name="dataset">
				<xsl:with-param name="samples" select="key('builds-by-project-name-and-update-type', concat(name,'-',update-type))"/>
				<xsl:with-param name="series-name" select="name"/>
				<xsl:with-param name="update-type" select="update-type"/>
				<xsl:with-param name="index" select="position()"/>
				<xsl:with-param name="color">
					<xsl:call-template name="choose-color"/>
				</xsl:with-param>
			</xsl:call-template>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="dataset">
		<xsl:param name="samples"/>
		<xsl:param name="index"/>
		<xsl:param name="series-name"/>
		<xsl:param name="update-type"/>
		<xsl:param name="color"/>
		
		<xsl:variable name="suffix"><xsl:if test="$index != 1">_<xsl:value-of select="$index"/></xsl:if></xsl:variable>
		
		<xsl:text>&amp;line</xsl:text>
		<xsl:value-of select="$suffix"/>
		<xsl:text>=1,</xsl:text>
		<xsl:value-of select="$color"/>
		<xsl:text>,</xsl:text>
		<xsl:value-of select="$series-name"/>
		<xsl:if test="$incrBuildCount != 0 and $fullBuildCount != 0">
			<xsl:text> (</xsl:text>
			<xsl:value-of select="$update-type"/>
			<xsl:text>)</xsl:text>
		</xsl:if>
		<xsl:text>,12,4&amp;</xsl:text>
		
		&amp;values<xsl:value-of select="$suffix"/>=<xsl:for-each select="$samples">
			<xsl:if test="position()!=1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:text>(</xsl:text>
			<xsl:value-of select="timestamp/@millis"/>
			<xsl:text>,</xsl:text>
			<xsl:value-of select="elapsed-time/@millis div 60000"/>
			<xsl:text>)</xsl:text>
		</xsl:for-each>&amp;
		
		&amp;tool_tips<xsl:value-of select="$suffix"/>=<xsl:for-each select="$samples">
			<xsl:if test="position()!=1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:value-of select="name"/>
			<xsl:text>&lt;br&gt;</xsl:text>
			<xsl:value-of select="vulcan:getMessage($messageSource, 'th.build.number')"/>
			<xsl:text> </xsl:text>
			<xsl:value-of select="build-number"/>
			<xsl:text>&lt;br&gt;</xsl:text>
			<xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.completed')"/>
			<xsl:text> </xsl:text>
			<xsl:value-of select="timestamp"/>
			<xsl:text>&lt;br&gt;</xsl:text>
			<xsl:value-of select="vulcan:getMessage($messageSource, 'label.update.type')"/>
			<xsl:text> </xsl:text>
			<xsl:value-of select="update-type"/>
			<xsl:text>&lt;br&gt;</xsl:text>
			<xsl:value-of select="vulcan:getMessage($messageSource, 'th.build.elapsed.time')"/>
			<xsl:text> </xsl:text>
			<xsl:value-of select="elapsed-time"/>
		</xsl:for-each>&amp;

		&amp;links<xsl:value-of select="$suffix"/>=<xsl:for-each select="$samples">
			<xsl:if test="position()!=1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:text>javascript:showBuildDetails('</xsl:text>
			<xsl:value-of select="$viewProjectStatusURL"/>
			<xsl:value-of select="name"/>
			<xsl:text>/</xsl:text>
			<xsl:value-of select="build-number"/>
			<xsl:text>/')</xsl:text>
		</xsl:for-each>&amp;
	</xsl:template>
	
	<xsl:template name="choose-color">
		<xsl:choose>
			<xsl:when test="position() = 1">
				<xsl:text>#0066DD</xsl:text>
			</xsl:when>
			<xsl:when test="position() = 2">
				<xsl:text>#DC3912</xsl:text>
			</xsl:when>
			<xsl:when test="position() = 3">
				<xsl:text>#FF9900</xsl:text>
			</xsl:when>
			<xsl:when test="position() = 4">
				<xsl:text>#008000</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>#000000</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
