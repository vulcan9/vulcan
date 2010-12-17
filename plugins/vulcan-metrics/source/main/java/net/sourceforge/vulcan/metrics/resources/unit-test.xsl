<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
	<xsl:output method="xml" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>

	<xsl:variable name="junitCount" select="count(//testcase)"/>
	<xsl:variable name="nunitCount" select="count(//test-case[@executed='True'])"/>
	<xsl:variable name="ignored" select="count(//test-case[@executed='False'])"/>
	<xsl:variable name="total" select="$junitCount + $nunitCount"/>
	
	<xsl:template match="/">
		<metrics>
			<xsl:if test="$total != 0 or $ignored != 0">
				<metric key="vulcan.metrics.tests.executed">
					<xsl:attribute name="value"><xsl:value-of select="$total"/></xsl:attribute>
				</metric>
				<metric key="vulcan.metrics.tests.failed">
					<xsl:attribute name="value">
						<xsl:value-of select="
							count(//testcase/failure) +
							count(//testcase/error) +
							count(//test-case[@success='False'])"/>
					</xsl:attribute>
				</metric>
				
				<xsl:call-template name="list-failures">
					<xsl:with-param name="nodes" select="//testcase[failure]"/>
					<xsl:with-param name="appendSuiteName" select="true()"/>
				</xsl:call-template>
				<xsl:call-template name="list-failures">
					<xsl:with-param name="nodes" select="//testcase[error]"/>
					<xsl:with-param name="appendSuiteName" select="true()"/>
				</xsl:call-template>
				<xsl:call-template name="list-failures">
					<xsl:with-param name="nodes" select="//test-case[@success='False']"/>
				</xsl:call-template>
			</xsl:if>
			<xsl:if test="$ignored != 0">
				<metric key="vulcan.metrics.tests.ignored">
					<xsl:attribute name="value">
						<xsl:value-of select="$ignored"/>
					</xsl:attribute>
				</metric>
			</xsl:if>
		</metrics>
	</xsl:template>
	
	<xsl:template name="list-failures">
		<xsl:param name="nodes"/>
		<xsl:param name="appendSuiteName" select="false()"/>
		
		<xsl:for-each select="$nodes">
			<test-failure>
				<xsl:if test="$appendSuiteName">
					<xsl:value-of select="../@name"/>
					<xsl:text>.</xsl:text>
				</xsl:if>
				<xsl:value-of select="@name"/>
			</test-failure>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>