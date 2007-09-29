<?xml version="1.0" encoding="UTF-8" ?>

<html
	xmlns="http://www.w3.org/1999/xhtml" xml:lang="en-US"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:x="http://java.sun.com/jsp/jstl/xml"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">

<jsp:directive.page session="false"/>

<html:xhtml/>

<body>

<div class="vulcan-logo">
	<html:img page="/images/vulcan-logo.png" alt="Vulcan Logo"/>
</div>

<v:bubble>
<table>
	<caption>About Vulcan</caption>
	<tbody>
		<tr><td><pre>Vulcan, Copyright (C) 2005-2007 Chris Eldredge
Vulcan comes with ABSOLUTELY NO WARRANTY.
This is free software, and you are welcome
to redistribute it under certain conditions;
see <html:link page="/COPYING">GPL Version 2 License</html:link>.

<a href="http://code.google.com/p/vulcan/">http://code.google.com/p/vulcan/</a>
</pre></td></tr>
	</tbody>
</table>
</v:bubble>

<v:bubble styleClass="plugin-versions">
<table class="plugin-versions">
	<caption><fmt:message key="captions.versions"/></caption>
	<thead>
		<tr>
			<th><fmt:message key="th.component.name"/></th>
			<th><fmt:message key="th.component.type"/></th>
			<th><fmt:message key="th.component.version"/></th>
		</tr>
	</thead>
	<tbody>
	<c:set var="components" value="${stateManager.componentVersions}"/>
	<c:forEach items="${components}" var="comp">
		<tr>
			<td title="${comp.id}">${comp.name}</td>
			<td>${comp.type}</td>
			<td class="component-version">${comp.version}</td>
		</tr>
	</c:forEach>
	</tbody>
</table>
</v:bubble>

<v:bubble>
<table>
	<caption>Third Party Software</caption>
	<thead>
		<tr>
			<th>Product</th>
			<th>License</th>
			<th>Web Site</th>
		</tr>
	</thead>
	<tbody>
		<tr>
			<td>ANTLR</td>
			<td><a href="http://www.antlr.org/license.html">ANTLR 3 License</a> [BSD Style]</td>
			<td><a href="http://www.antlr.org/">http://www.antlr.org/</a></td>
		</tr>
		<tr>
			<td>cglib</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Versoin 2.0</a></td>
			<td><a href="http://cglib.sourceforge.net/">http://cglib.sourceforge.net/</a></td>
		</tr>
		<tr>
			<td>EZMorph</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Versoin 2.0</a></td>
			<td><a href="http://ezmorph.sourceforge.net/">http://ezmorph.sourceforge.net/</a></td>
		</tr>
		<tr>
			<td>Ganymed SSH2</td>
			<td><a href="http://www.ganymed.ethz.ch/ssh2/LICENSE.txt">Ganymed Open Source License</a> [BSD Style]</td>
			<td><a href="http://www.ganymed.ethz.ch/ssh2/">http://www.ganymed.ethz.ch/ssh2/</a></td>
		</tr>
		<tr>
			<td>HSQLDB</td>
			<td><a href="http://hsqldb.org/web/hsqlLicense.html">Hypersonic Group / HSQL Development Group</a></td>
			<td><a href="http://hsqldb.org/">http://hsqldb.org/</a></td>
		</tr>
		<tr>
			<td>Jakarta Commons</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Versoin 2.0</a></td>
			<td><a href="http://commons.apache.org/">http://commons.apache.org/</a></td>
		</tr>
		<tr>
			<td>Jakarta Oro</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Versoin 2.0</a></td>
			<td><a href="http://jakarta.apache.org/oro/">http://jakarta.apache.org/oro/</a></td>
		</tr>
		<tr>
			<td>Jakarta Taglibs</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Versoin 2.0</a></td>
			<td><a href="http://jakarta.apache.org/taglibs/">http://jakarta.apache.org/taglibs/</a></td>
		</tr>
		<tr>
			<td>Java Activation Framework</td>
			<td><a href="https://glassfish.dev.java.net/public/CDDLv1.0.html">Common Development and Distribution License</a></td>
			<td><a href="http://java.sun.com/products/javabeans/jaf/downloads/index.html">http://java.sun.com/products/javabeans/jaf/downloads/index.html</a></td>
		</tr>
		<tr>
			<td>Java Mail API</td>
			<td><a href="https://glassfish.dev.java.net/public/CDDLv1.0.html">Common Development and Distribution License</a></td>
			<td><a href="http://java.sun.com/products/javamail/">http://java.sun.com/products/javamail/</a></td>
		</tr>
		<tr>
			<td>JDOM</td>
			<td>Apache Public License (without acknowledgement clause)</td>
			<td><a href="http://www.jdom.org/">http://www.jdom.org</a></td>
		</tr>
		<tr>
			<td>Json-lib</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Versoin 2.0</a></td>
			<td><a href="http://json-lib.sourceforge.net/">http://json-lib.sourceforge.net/</a></td>
		</tr>
		<tr>
			<td>JUG (Java UUID Generator)</td>
			<td><a href="http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html">LGPL 2.1</a></td>
			<td><a href="http://docs.safehaus.org/display/JUG/Home">http://docs.safehaus.org/display/JUG/Home</a></td>
		</tr>
		<tr>
			<td>log4j</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Versoin 2.0</a></td>
			<td><a href="http://logging.apache.org/log4j/docs/index.html">http://logging.apache.org/log4j/docs/index.html</a></td>
		</tr>
		<tr>
			<td>MochiKit</td>
			<td><a href="http://www.opensource.org/licenses/mit-license.php">MIT License</a></td>
			<td><a href="http://mochikit.com/">http://mochikit.com/</a></td>
		</tr>
		<tr>
			<td>Netbeans CVS Client</td>
			<td><a href="http://www.netbeans.org/cddl.html">Common Development and Distribution License</a></td>
			<td><a href="http://javacvs.netbeans.org/">http://javacvs.netbeans.org/</a></td>
		</tr>
		<tr>
			<td>Plexus Utils</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Versoin 2.0</a></td>
			<td><a href="http://plexus.codehaus.org/plexus-utils/">http://plexus.codehaus.org/plexus-utils/</a></td>
		</tr>
		<tr>
			<td>PlotKit</td>
			<td><a href="http://www.opensource.org/licenses/bsd-license.php">BSD License</a></td>
			<td><a href="http://www.liquidx.net/plotkit/">http://www.liquidx.net/plotkit/</a></td>
		</tr>
		<tr>
			<td>Quartz</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Version 2.0</a></td>
			<td><a href="http://www.opensymphony.com/quartz/">http://www.opensymphony.com/quartz/</a></td>
		</tr>
		<tr>
			<td>SiteMesh</td>
			<td><a href="http://www.opensymphony.com/sitemesh/license.action">OpenSymphony Software License, Version 1.1</a></td>
			<td><a href="http://www.opensymphony.com/sitemesh/">http://www.opensymphony.com/sitemesh/</a></td>
		</tr>
		<tr>
			<td>Spring Framework</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Version 2.0</a></td>
			<td><a href="http://springframework.org/">http://springframework.org/</a></td>
		</tr>
		<tr>
			<td>Struts</td>
			<td><a href="http://www.apache.org/licenses/LICENSE-2.0">Apache Public License, Versoin 2.0</a></td>
			<td><a href="http://struts.apache.org/">http://struts.apache.org/</a></td>
		</tr>
		<tr>
			<td>TMate SVNKit</td>
			<td><a href="http://svnkit.com/license.html">TMate Open Source License</a></td>
			<td><a href="http://svnkit.com/">http://svnkit.com/</a></td>
		</tr>
	</tbody>
</table>
</v:bubble>

</body>
</html>
