<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	version="1.0">
	
	<xsl:output method="xml" media-type="application/xml" omit-xml-declaration="yes"/>
	
	<xsl:param name="caption"/>
	<xsl:param name="detailLink"/>
	<xsl:param name="nameHeader"/>
	<xsl:param name="ageHeader"/>
	<xsl:param name="tagHeader"/>
	<xsl:param name="buildNumberHeader"/>
	<xsl:param name="revisionHeader"/>
	<xsl:param name="statusHeader"/>
	<xsl:param name="timestampLabel"/>
	<xsl:param name="sortSelect"/>
	<xsl:param name="sortOrder"/>

	<xsl:variable name="sortOrder1">
		<xsl:choose>
			<xsl:when test="$sortOrder='descending'">descending</xsl:when>
			<xsl:otherwise>ascending</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
		
	<xsl:template match="/">
		<div>
			<div class="caption"><xsl:value-of select="$caption" disable-output-escaping="yes"/></div>
			<xsl:choose>
				<xsl:when test="$sortSelect='name'">
					<xsl:apply-templates select="/projects/*">
						<xsl:sort select="@name" order="{$sortOrder1}"/>
					</xsl:apply-templates>
				</xsl:when>
				<xsl:when test="$sortSelect='age'">
					<xsl:apply-templates select="/projects/*">
						<xsl:sort select="timestamp/@millis" order="{$sortOrder1}" data-type="number"/>
					</xsl:apply-templates>
				</xsl:when>
				<xsl:when test="$sortSelect='revision'">
					<xsl:apply-templates select="/projects/*">
						<xsl:sort select="revision/@numeric" order="{$sortOrder1}" data-type="number"/>
					</xsl:apply-templates>
				</xsl:when>
				<xsl:when test="$sortSelect='build-number'">
					<xsl:apply-templates select="/projects/*">
						<xsl:sort select="build-number" order="{$sortOrder1}" data-type="number"/>
					</xsl:apply-templates>
				</xsl:when>
				<xsl:otherwise>
					<xsl:apply-templates select="/projects/*">
						<xsl:sort select="*[name()=$sortSelect]" order="{$sortOrder1}"/>
					</xsl:apply-templates>
				</xsl:otherwise>
			</xsl:choose>
		</div>
	</xsl:template>
	
	<xsl:template match="project">
		<div class="project-status-summary">
			<span>
				<xsl:attribute name="class">
					<xsl:value-of select="status"/>
					<xsl:text> status</xsl:text>
				</xsl:attribute>
				<xsl:value-of select="status"/>
			</span>
			<xsl:element name="a">
				<xsl:attribute name="href"><xsl:value-of select="$detailLink"/><xsl:value-of select="@name"/></xsl:attribute>
				<xsl:value-of select="@name"/>
			</xsl:element>
			<xsl:if test="build-number">
				<xsl:text> build </xsl:text>
				<xsl:value-of select="build-number"/>
			</xsl:if>
			<xsl:if test="revision and timestamp/@age">
				<br/>
				<span class="">
					<xsl:text>Revision </xsl:text>
					<xsl:value-of select="revision"/>
					<xsl:text> was built </xsl:text>
					<xsl:value-of select="timestamp/@age"/>
					<xsl:text> ago from </xsl:text>
					<xsl:value-of select="repository-tag-name"/>
				</span>
			</xsl:if>
			<xsl:if test="first-failure">
				<br/>
				<span class="">
					<xsl:text>Broken for </xsl:text>
					<xsl:value-of select="first-failure/elapsed-time/@age"/>
				</span>
			</xsl:if>
			<!-- 
			<xsl:attribute name="title"><xsl:value-of select="message"/></xsl:attribute>
			 -->
		</div>
	</xsl:template>
</xsl:stylesheet>
