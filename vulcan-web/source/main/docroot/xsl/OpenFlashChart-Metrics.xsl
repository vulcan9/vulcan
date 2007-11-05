<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0" xmlns:math="http://exslt.org/math">
	
	<xsl:output method="text" media-type="text/plain" version="1.0"
		encoding="UTF-8" omit-xml-declaration="yes"/>
	
	<xsl:strip-space elements="*"/>
	
	<xsl:param name="title"/>
	<xsl:param name="projectSiteURL"/>
	<xsl:param name="viewProjectStatusURL"/>
	<xsl:param name="issueTrackerURL"/>
	<xsl:param name="issueListHeader"/>
	<xsl:param name="nextBuildLabel"/>
	<xsl:param name="prevBuildLabel"/>
	<xsl:param name="sandboxLabel"/>
	<xsl:param name="buildLogLabel"/>
	<xsl:param name="revisionCaption"/>
	<xsl:param name="changeSetCaption"/>
	<xsl:param name="projectHeader"/>
	<xsl:param name="revisionHeader"/>
	<xsl:param name="buildNumberHeader"/>
	<xsl:param name="authorHeader"/>
	<xsl:param name="timestampHeader"/>
	<xsl:param name="messageHeader"/>
	<xsl:param name="pathsHeader"/>
	<xsl:param name="diffHeader"/>
	<xsl:param name="statusHeader"/>
	<xsl:param name="lastGoodBuildNumberLabel"/>
	<xsl:param name="repositoryUrlLabel"/>
	<xsl:param name="repositoryTagNameHeader"/>
	<xsl:param name="currentlyBuildingMessage"/>
	<xsl:param name="buildRequestedByLabel"/>
	<xsl:param name="buildScheduledByLabel"/>
	<xsl:param name="elapsedTimeLabel"/>
	<xsl:param name="buildReasonLabel"/>
	<xsl:param name="updateTypeLabel"/>
	<xsl:param name="warningsLabel"/>
	<xsl:param name="errorsLabel"/>
	<xsl:param name="metricsLabel"/>
	<xsl:param name="testFailureLabel"/>
	<xsl:param name="testNameLabel"/>
	<xsl:param name="testFailureBuildNumberLabel"/>
	<xsl:param name="newTestFailureLabel"/>
	
	<xsl:key name="builds-by-project-name" match="/build-history/project" use="name"/>
	<xsl:key name="builds-by-timestamp" match="/build-history/project" use="timestamp/@millis"/>
	
	<xsl:variable name="metricLabel1" select="'Tests executed'"/>
	<xsl:variable name="metricLabel2" select="'Test failures'"/>
	
	<xsl:template match="/build-history">
		<xsl:variable name="maxValue1">
			<xsl:choose>
				<xsl:when test="count(project/metrics/metric[@label=$metricLabel1]/@value) != 0">
					<xsl:for-each select="project/metrics/metric[@label=$metricLabel1]/@value">
						<xsl:sort data-type="number" select="." order="descending"/>
						<xsl:if test="position()=1">
							<xsl:value-of select="ceiling(. * 1.05)"/>
						</xsl:if>
					</xsl:for-each>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="1"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<xsl:variable name="maxValue2">
			<xsl:for-each select="project/metrics/metric[@label=$metricLabel2]/@value">
				<xsl:sort data-type="number" select="." order="descending"/>
				<xsl:if test="position()=1">
					<xsl:value-of select="ceiling(. * 1.05)"/>
				</xsl:if>
			</xsl:for-each>
		</xsl:variable>
		
		<xsl:text>&amp;title=Metrics,{color: #7E97A6; font-size: 20; text-align:left}&amp;</xsl:text>

		&amp;y_min=0&amp;
		<xsl:text>&amp;y_max=</xsl:text>
		<xsl:text><xsl:value-of select="$maxValue1"/></xsl:text>
		<xsl:text>&amp;</xsl:text>

		&amp;bg_colour=#FFFFFF&amp;
		&amp;x_axis_colour=#818D9D&amp;
		&amp;x_grid_colour=#F0F0F0&amp;

		&amp;x_label_style=10,#164166,2,1&amp;

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

		&amp;y_axis_colour=#818D9D&amp;
		&amp;y_grid_colour=#ADB5C7&amp;

		<xsl:text>&amp;y_legend=</xsl:text>
		<xsl:value-of select="$metricLabel1"/>
		<xsl:text>,12,#164166&amp;</xsl:text>
		&amp;y_ticks=5,10,5&amp;
		
		<xsl:if test="$maxValue2 &gt; 0">
			<xsl:text>&amp;y2_legend=</xsl:text>
			<xsl:value-of select="$metricLabel2"/>
			<xsl:text>,12,#164166&amp;</xsl:text>
			
			&amp;y2_axis_colour=#818D9D&amp;
			<xsl:text>&amp;y2_max=</xsl:text>
			<xsl:value-of select="$maxValue2"/>
			<xsl:text>&amp;</xsl:text>
			&amp;show_y2=true&amp;
			
			<xsl:text>&amp;y2_lines=</xsl:text>
			<xsl:for-each select="project[generate-id() = generate-id(key('builds-by-project-name', name)[1])]">
				<xsl:if test="position()!=1">
					<xsl:text>,</xsl:text>
				</xsl:if>
			
				<xsl:value-of select="position()*2"/>
			</xsl:for-each>
			<xsl:text>&amp;</xsl:text>
		</xsl:if>
		
		<xsl:for-each select="project[generate-id() = generate-id(key('builds-by-project-name', name)[1])]">
			<xsl:variable name="samples" select="key('builds-by-project-name', name)"/>
			<xsl:variable name="name" select="name"/>
			<xsl:variable name="index">
				<xsl:choose>
					<xsl:when test="$maxValue2 &gt; 0">
						<xsl:value-of select="position() * 2 - 1"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="position()"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:variable>
			<xsl:variable name="showMetricInLegend" select="$maxValue2 &gt; 0"/>
			
			<xsl:call-template name="dataset">
				<xsl:with-param name="samples" select="$samples[metrics/metric/@label=$metricLabel1]"/>
				<xsl:with-param name="metricLabel" select="$metricLabel1"/>
				<xsl:with-param name="seriesName" select="name"/>
				<xsl:with-param name="index" select="$index"/>
				<xsl:with-param name="showMetricInLegend" select="$showMetricInLegend"/>
				<xsl:with-param name="color">
					<xsl:call-template name="choose-color">
						<xsl:with-param name="index" select="$index"/>
					</xsl:call-template>
				</xsl:with-param>
			</xsl:call-template>
			
			<xsl:if test="$maxValue2 &gt; 0">
				<xsl:call-template name="dataset">
					<xsl:with-param name="samples" select="$samples[metrics/metric/@label=$metricLabel2]"/>
					<xsl:with-param name="metricLabel" select="$metricLabel2"/>
					<xsl:with-param name="seriesName" select="name"/>
					<xsl:with-param name="index" select="$index + 1"/>
					<xsl:with-param name="showMetricInLegend" select="$showMetricInLegend"/>
					<xsl:with-param name="color">
						<xsl:call-template name="choose-color">
							<xsl:with-param name="index" select="$index + 1"/>
						</xsl:call-template>
					</xsl:with-param>
				</xsl:call-template>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="dataset">
		<xsl:param name="samples"/>
		<xsl:param name="index"/>
		<xsl:param name="seriesName"/>
		<xsl:param name="metricLabel"/>
		<xsl:param name="color"/>
		<xsl:param name="showMetricInLegend"/>
		
		<xsl:variable name="suffix"><xsl:if test="$index != 1">_<xsl:value-of select="$index"/></xsl:if></xsl:variable>
		
		<xsl:text>&amp;line</xsl:text>
		<xsl:value-of select="$suffix"/>
		<xsl:text>=2,</xsl:text>
		<xsl:value-of select="$color"/>
		<xsl:text>,</xsl:text>
		<xsl:value-of select="$seriesName"/>
		<xsl:if test="$showMetricInLegend">
			<xsl:text> (</xsl:text>
			<xsl:value-of select="$metricLabel"/>
			<xsl:text>)</xsl:text>
		</xsl:if>
		<xsl:text>,12,4&amp;</xsl:text>
		
<xsl:text>
</xsl:text>
		<xsl:text>&amp;values</xsl:text>
		<xsl:value-of select="$suffix"/>
		<xsl:text>=</xsl:text>
		<xsl:for-each select="$samples">
			<xsl:if test="position()!=1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:text>(</xsl:text>
			<xsl:value-of select="timestamp/@millis"/>
			<xsl:text>,</xsl:text>
			<xsl:value-of select="metrics/metric[@label=$metricLabel]/@value"/>
			<xsl:text>)</xsl:text>
		</xsl:for-each>
		<xsl:text>&amp;</xsl:text>
		
		&amp;tool_tips<xsl:value-of select="$suffix"/>=<xsl:for-each select="$samples">
			<xsl:if test="position()!=1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:text><xsl:value-of select="name"/></xsl:text>
			<xsl:text>&lt;br&gt;</xsl:text>
			<xsl:text>Build </xsl:text>
			<xsl:text><xsl:value-of select="build-number"/></xsl:text>
			<xsl:text>&lt;br&gt;</xsl:text>
			<xsl:text>Completed: </xsl:text>
			<xsl:text><xsl:value-of select="timestamp"/></xsl:text>
			<xsl:text>&lt;br&gt;</xsl:text>
			<xsl:text><xsl:value-of select="$metricLabel"/></xsl:text>
			<xsl:text>:</xsl:text>
			<xsl:text><xsl:value-of select="metrics/metric[@label=$metricLabel]/@value"/></xsl:text>
		</xsl:for-each>&amp;
		
		&amp;links<xsl:value-of select="$suffix"/>=<xsl:for-each select="$samples">
			<xsl:if test="position()!=1">
				<xsl:text>,</xsl:text>
			</xsl:if>
			<xsl:text>javascript:showBuildDetails('</xsl:text>
			<xsl:text><xsl:value-of select="substring-before($viewProjectStatusURL, '?')"/></xsl:text>
			<xsl:text>?projectName=</xsl:text>
			<xsl:text><xsl:value-of select="name"/></xsl:text>
			<xsl:text>%26buildNumber=</xsl:text>
			<xsl:text><xsl:value-of select="build-number"/></xsl:text>
			<xsl:text>%26transform=xhtml')</xsl:text>
		</xsl:for-each>&amp;
		
	</xsl:template>
	
	<xsl:template name="choose-color">
		<xsl:param name="index"/>
		<xsl:choose>
			<xsl:when test="$index = 1">
				<xsl:text>#0066DD</xsl:text>
			</xsl:when>
			<xsl:when test="$index = 2">
				<xsl:text>#DC3912</xsl:text>
			</xsl:when>
			<xsl:when test="$index = 3">
				<xsl:text>#FF9900</xsl:text>
			</xsl:when>
			<xsl:when test="$index = 4">
				<xsl:text>#008000</xsl:text>
			</xsl:when>
			<xsl:otherwise>
				<xsl:text>#000000</xsl:text>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="x-axis-min-max">
		<xsl:variable name="timestamps" select="/build-history/project/timestamp/@millis"/>
		
		<xsl:for-each select="$timestamps">
			<xsl:sort data-type="number" select="." order="ascending"/>
			<xsl:if test="position()=1">
				<xsl:text>&amp;x_min=</xsl:text>
				<xsl:value-of select="."/>
				<xsl:text>&amp;</xsl:text>
			</xsl:if>
			<xsl:if test="position()=count($timestamps)">
				<xsl:text>&amp;x_max=</xsl:text>
				<xsl:value-of select="."/>
				<xsl:text>&amp;</xsl:text>
			</xsl:if>
		</xsl:for-each>
	</xsl:template>
	
</xsl:stylesheet>
