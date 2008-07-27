<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
	
	<xsl:output method="xml" version="1.0"
		encoding="UTF-8" omit-xml-declaration="no"/>

	<xsl:variable name="junitCount" select="count(//testcase)"/>
	<xsl:variable name="nunitCount" select="count(//test-case[@executed='True'])"/>
	<xsl:variable name="cunitCount" select="count(//CUNIT_RUN_TEST_RECORD)"/>
	<xsl:variable name="seleniumCount" select="count(//html/body/table/tr/td/table/tbody/tr[@class='status_passed' or @class='status_failed'])"/>
	
	<xsl:variable name="ignored" select="count(//test-case[@executed='False'])"/>
	<xsl:variable name="total" select="$junitCount + $nunitCount + $seleniumCount + $cunitCount"/>
	
	<xsl:template match="/">
		<metrics>
			<xsl:if test="$total != 0 or $ignored != 0">
				<metric key="vulcan.metrics.tests.executed" type="number">
					<xsl:attribute name="value"><xsl:value-of select="$total"/></xsl:attribute>
				</metric>
				<metric key="vulcan.metrics.tests.failed" type="number">
					<xsl:attribute name="value">
						<xsl:value-of select="
							count(//testcase/failure) +
							count(//testcase/error) +
							count(//test-case[@success='False']) +
							count(//html/body/table/tr/td/table/tbody/tr[@class='status_failed']) +
							count(//CUNIT_RUN_TEST_FAILURE)"/>
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
				
				<xsl:call-template name="list-failures-cunit"/>
				<xsl:call-template name="list-failures-selenium"/>
			</xsl:if>
			<xsl:if test="$ignored != 0">
				<metric key="vulcan.metrics.tests.ignored" type="number">
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
	
	<xsl:template name="list-failures-cunit">
		<xsl:for-each select="//CUNIT_RUN_TEST_FAILURE/TEST_NAME">
			<xsl:variable name="suite-name" select="ancestor::CUNIT_RUN_SUITE//SUITE_NAME/text()"/>
			<test-failure>
				<xsl:value-of select="substring($suite-name, 2, string-length($suite-name) - 2)"/>
				<xsl:text>.</xsl:text>
				<xsl:value-of select="substring-before(substring(., 2), '()')"/>
			</test-failure>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="list-failures-selenium">
		<xsl:for-each select="//html/body/table/tr/td/table/tbody/tr[@class='status_failed']/td/a">
			<test-failure>
				<xsl:value-of select="ancestor::table/thead/tr[contains(@class, 'title')]/td/text()"/>
				<xsl:text>.</xsl:text>
				<xsl:value-of select="."/>
			</test-failure>
		</xsl:for-each>
	</xsl:template>
</xsl:stylesheet>