<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	version="1.0">
	
	<xsl:output method="xml" media-type="application/xml" omit-xml-declaration="yes"/>
	
	<xsl:param name="contextRoot"/>
	<xsl:param name="caption"/>
	<xsl:param name="sortUrl"/>
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
	
	<xsl:variable name="opposite">
		<xsl:choose>
			<xsl:when test="$sortOrder='descending'">ascending</xsl:when>
		<xsl:otherwise>descending</xsl:otherwise>
		</xsl:choose>
	</xsl:variable>
		
	<xsl:template match="/">
		<table class="projects dashboard">
			<caption><xsl:value-of select="$caption" disable-output-escaping="yes"/></caption>
			<thead>
				<tr>
					<th class="sortable">
						<xsl:element name="a">
							<xsl:choose>
								<xsl:when test="$sortSelect='name'">
									<xsl:attribute name="class">sorted-<xsl:value-of select="$sortOrder1"/></xsl:attribute>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=name&amp;config.sortOrder=<xsl:value-of select="$opposite"/></xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=name&amp;config.sortOrder=ascending</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="$nameHeader"/>
						</xsl:element>
					</th>
					<th class="sortable age">
						<xsl:element name="a">
							<xsl:choose>
								<xsl:when test="$sortSelect='age'">
									<xsl:attribute name="class">sorted-<xsl:value-of select="$sortOrder1"/></xsl:attribute>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=age&amp;config.sortOrder=<xsl:value-of select="$opposite"/></xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=age&amp;config.sortOrder=descending</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="$ageHeader"/>
						</xsl:element>
					</th>
					<th class="sortable">
						<xsl:element name="a">
							<xsl:choose>
								<xsl:when test="$sortSelect='build-number'">
									<xsl:attribute name="class">sorted-<xsl:value-of select="$sortOrder1"/></xsl:attribute>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=build-number&amp;config.sortOrder=<xsl:value-of select="$opposite"/></xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=build-number&amp;config.sortOrder=ascending</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="$buildNumberHeader"/>
						</xsl:element>
					</th>
					<th class="sortable">
						<xsl:element name="a">
							<xsl:choose>
								<xsl:when test="$sortSelect='revision'">
									<xsl:attribute name="class">sorted-<xsl:value-of select="$sortOrder1"/></xsl:attribute>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=revision&amp;config.sortOrder=<xsl:value-of select="$opposite"/></xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=revision&amp;config.sortOrder=ascending</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="$revisionHeader"/>
						</xsl:element>
					</th>
					<th class="sortable">
						<xsl:element name="a">
							<xsl:choose>
								<xsl:when test="$sortSelect='repository-tag-name'">
									<xsl:attribute name="class">sorted-<xsl:value-of select="$sortOrder1"/></xsl:attribute>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=repository-tag-name&amp;config.sortOrder=<xsl:value-of select="$opposite"/></xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=repository-tag-name&amp;config.sortOrder=ascending</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="$tagHeader"/>
						</xsl:element>
					</th>
					<th class="sortable">
						<xsl:element name="a">
							<xsl:choose>
								<xsl:when test="$sortSelect='status'">
									<xsl:attribute name="class">sorted-<xsl:value-of select="$sortOrder1"/></xsl:attribute>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=status&amp;config.sortOrder=<xsl:value-of select="$opposite"/></xsl:attribute>
								</xsl:when>
								<xsl:otherwise>
									<xsl:attribute name="href"><xsl:value-of select="$sortUrl"/>&amp;config.sortColumn=status&amp;config.sortOrder=ascending</xsl:attribute>
								</xsl:otherwise>
							</xsl:choose>
							<xsl:value-of select="$statusHeader"/>
						</xsl:element>
					</th>
				</tr>
			</thead>
					
			<tbody>
				<xsl:choose>
					<xsl:when test="$sortSelect='name'">
						<xsl:apply-templates select="/projects/project">
							<xsl:sort select="@name" order="{$sortOrder1}"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="$sortSelect='age'">
						<xsl:apply-templates select="/projects/project">
							<xsl:sort select="timestamp/@millis" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="$sortSelect='revision'">
						<xsl:apply-templates select="/projects/project">
							<xsl:sort select="revision/@numeric" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:when test="$sortSelect='build-number'">
						<xsl:apply-templates select="/projects/project">
							<xsl:sort select="build-number" order="{$sortOrder1}" data-type="number"/>
						</xsl:apply-templates>
					</xsl:when>
					<xsl:otherwise>
						<xsl:apply-templates select="/projects/project">
							<xsl:sort select="*[name()=$sortSelect]" order="{$sortOrder1}"/>
						</xsl:apply-templates>
					</xsl:otherwise>
				</xsl:choose>
			</tbody>
		</table>
	</xsl:template>
	
	<xsl:template match="project">
		<tr>
			<td>
				<xsl:element name="a">
					<xsl:attribute name="href">
						<xsl:value-of select="$detailLink"/>
						<xsl:value-of select="@name"/>
						<xsl:text>/LATEST/</xsl:text>
					</xsl:attribute>
					<xsl:value-of select="@name"/>
				</xsl:element>
			</td>
			<xsl:element name="td">
				<xsl:attribute name="title"><xsl:value-of select="$timestampLabel"/> <xsl:value-of select="timestamp"/></xsl:attribute>
				<xsl:value-of select="timestamp/@age"/>
			</xsl:element>
			<td><xsl:value-of select="build-number"/></td>
			<td><xsl:value-of select="revision"/></td>
			<td><xsl:value-of select="repository-tag-name"/></td>
			<xsl:element name="td">
				<xsl:attribute name="title"><xsl:value-of select="message"/></xsl:attribute>
				<xsl:attribute name="class">
					<xsl:value-of select="status"/>
					<xsl:text> status</xsl:text>
				</xsl:attribute>
				<xsl:value-of select="status"/>
				<xsl:if test="locked/text() = 'true'">
					<img alt="Locked">
						<xsl:attribute name="src">
							<xsl:value-of select="$contextRoot"/>
							<xsl:text>images/lock.png</xsl:text>
						</xsl:attribute>
						<xsl:attribute name="title">
							<xsl:value-of select="locked/@message"/>
						</xsl:attribute>
					</img>
				</xsl:if>
			</xsl:element>
		</tr>
	</xsl:template>
</xsl:stylesheet>
