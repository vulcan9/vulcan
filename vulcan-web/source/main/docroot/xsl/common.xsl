<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0"
	xmlns:vulcan="xalan://net.sourceforge.vulcan.web.XslHelper"
	extension-element-prefixes="vulcan"
	exclude-result-prefixes="vulcan">
	
	<xsl:param name="locale"/>
	
	<xsl:variable name="messageSource" select="vulcan:new(string($locale))"/>
</xsl:stylesheet>
