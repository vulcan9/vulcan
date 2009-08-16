<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:vulcan="xalan://net.sourceforge.vulcan.web.XslHelper"
	extension-element-prefixes="vulcan"
	exclude-result-prefixes="vulcan">
	
	<xsl:param name="locale"/>
	
	<xsl:variable name="messageSource" select="vulcan:new(string($locale))"/>

	<xsl:template match="project">
		<xsl:param name="columns"/>
		<xsl:param name="visible-columns"/>
		<xsl:param name="detailLink"/>
		<xsl:param name="detailLinkUseLatestKeyword" select="true()"/>
				
		<xsl:variable name="project" select="."/>
		
		<tr>
			<xsl:for-each select="$columns">
				<xsl:variable name="col" select="."/>
				<xsl:variable name="colName" select="text()"/>
				<xsl:variable name="buildDetailsSuffix">
					<xsl:choose>
						<xsl:when test="$detailLinkUseLatestKeyword">
							<xsl:text>/LATEST/</xsl:text>
						</xsl:when>
						<xsl:otherwise>
							<xsl:text>/</xsl:text>
							<xsl:value-of select="$project/build-number"/>
							<xsl:text>/</xsl:text>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:variable>
				<td>
					<xsl:if test="not($visible-columns[text() = $colName])">
						<xsl:attribute name="class">hidden</xsl:attribute>
					</xsl:if>
				
					<xsl:choose>
						<xsl:when test="$col='dashboard.columns.name'">
							<a>
								<xsl:attribute name="href">
									<xsl:call-template name="concat-link">
										<xsl:with-param name="prefix" select="$detailLink"/>
										<xsl:with-param name="suffix" select="concat($project/name, $buildDetailsSuffix)"/>
									</xsl:call-template>
								</xsl:attribute>
								<xsl:value-of select="$project/name"/>
							</a>
						</xsl:when>
						<xsl:when test="$col='dashboard.columns.age'">
							<xsl:attribute name="title">
								<xsl:value-of select="vulcan:getMessage($messageSource, 'label.build.timestamp', $project/timestamp)"/>
							</xsl:attribute>
							<xsl:value-of select="$project/timestamp/@age"/>
						</xsl:when>
						<xsl:when test="$col='dashboard.columns.failure-age'">
							<xsl:value-of select="$project/first-failure/elapsed-time/@age"/>
						</xsl:when>
						<xsl:when test="$col='dashboard.columns.status'">
							<xsl:attribute name="title"><xsl:value-of select="$project/message"/></xsl:attribute>
							<xsl:attribute name="class">
								<xsl:value-of select="$project/status"/>
								<xsl:text> status</xsl:text>
								<xsl:if test="not($visible-columns[text() = $colName])">
									<xsl:text> hidden</xsl:text>
								</xsl:if>
							</xsl:attribute>
							<xsl:value-of select="$project/status"/>
							<xsl:if test="$project/locked/text() = 'true'">
								<img alt="Locked">
									<xsl:attribute name="src">
										<xsl:value-of select="$contextRoot"/>
										<xsl:text>images/lock.png</xsl:text>
									</xsl:attribute>
									<xsl:attribute name="title">
										<xsl:value-of select="$project/locked/@message"/>
									</xsl:attribute>
								</img>
							</xsl:if>
						</xsl:when>
						<xsl:otherwise>
							<xsl:if test="$project/metrics/metric[@key=$col and (@type = 'number' or @type = 'percent')] or $col = 'dashboard.columns.build-number'">
								<xsl:attribute name="class">
									<xsl:text>numeric</xsl:text>
									<xsl:if test="not($visible-columns[text() = $colName])">
										<xsl:text> hidden</xsl:text>
									</xsl:if>
								</xsl:attribute>
							</xsl:if>
							<xsl:choose>
								<xsl:when test="$project/metrics/metric[@key=$col]/@type = 'percent'">
									<xsl:value-of select="format-number($project/metrics/metric[@key=$col]/@value, '#.##%')"/>
								</xsl:when>
								<xsl:otherwise>
									<xsl:value-of select="$project/*[name()=substring-after($col, 'dashboard.columns.')] | $project/metrics/metric[@key=$col]/@value"/>
								</xsl:otherwise>
							</xsl:choose>
						</xsl:otherwise>
					</xsl:choose>
				</td>
			</xsl:for-each>
		</tr>
	</xsl:template>
	
	<xsl:template name="project-column-headers">
		<xsl:param name="columns"/>
		<xsl:param name="visible-columns"/>
		<xsl:param name="sortSelect"/>
		<xsl:param name="sortOrder"/>
		<xsl:param name="sortUrl"/>
		
		<xsl:variable name="opposite">
			<xsl:choose>
				<xsl:when test="$sortOrder='descending'">ascending</xsl:when>
				<xsl:otherwise>descending</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		
		<thead>
			<tr>
				<xsl:for-each select="$columns">
					<xsl:variable name="colName" select="text()"/>
					<th class="sortable">
						<xsl:attribute name="id">
							<xsl:text>col_</xsl:text>
							<xsl:value-of select="vulcan:mangle($colName)"/>
						</xsl:attribute>
						<xsl:if test="not($visible-columns[text() = $colName])">
							<xsl:attribute name="class">hidden</xsl:attribute>
						</xsl:if>
						<xsl:choose>
							<xsl:when test="$sortUrl != ''">
								<a>
									<xsl:choose>
										<xsl:when test="$sortSelect=text()">
											<xsl:attribute name="class">sorted-<xsl:value-of select="$sortOrder"/></xsl:attribute>
											<xsl:attribute name="href">
												<xsl:value-of select="$sortUrl"/>
												<xsl:text>&amp;config.sortColumn=</xsl:text>
												<xsl:value-of select="."/>
												<xsl:text>&amp;config.sortOrder=</xsl:text>
												<xsl:value-of select="$opposite"/>
											</xsl:attribute>
										</xsl:when>
										<xsl:otherwise>
											<xsl:attribute name="href">
												<xsl:value-of select="$sortUrl"/>
												<xsl:text>&amp;config.sortColumn=</xsl:text>
												<xsl:value-of select="."/>
												<xsl:text>&amp;config.sortOrder=ascending</xsl:text>
											</xsl:attribute>
										</xsl:otherwise>
									</xsl:choose>
									<xsl:value-of select="vulcan:getMessage($messageSource, text())"/>
								</a>
							</xsl:when>
							<xsl:otherwise>
								<xsl:value-of select="vulcan:getMessage($messageSource, text())"/>
							</xsl:otherwise>
						</xsl:choose>
					</th>
				</xsl:for-each>
			</tr>
		</thead>
	</xsl:template>

	<xsl:template name="concat-link">
		<xsl:param name="prefix"/>
		<xsl:param name="suffix"/>
		
		<xsl:choose>
			<xsl:when test="substring-before($prefix, ';jsessionid=')">
				<xsl:value-of select="substring-before($prefix, ';jsessionid=')"/>
				<xsl:value-of select="$suffix"/>
				<xsl:text>;jsessionid=</xsl:text>
				<xsl:value-of select="substring-after($prefix, ';jsessionid=')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat($prefix, $suffix)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
