<?xml version="1.0" encoding="UTF-8" ?>

<jsp:root
	version="2.0"
	xmlns:jsp="http://java.sun.com/JSP/Page"
	xmlns:c="http://java.sun.com/jsp/jstl/core"
	xmlns:fmt="http://java.sun.com/jsp/jstl/fmt"
	xmlns:html="http://struts.apache.org/tags-html"
	xmlns:spring="http://www.springframework.org/tags"
	xmlns:v="http://vulcan.sourceforge.net/j2ee/jsp/tags">
<jsp:directive.page import="org.apache.commons.codec.binary.Hex"/>
<jsp:directive.page import="org.apache.commons.codec.digest.DigestUtils"/>
<jsp:directive.page import="org.apache.commons.io.IOUtils"/>
<jsp:directive.page import="java.security.SecureRandom"/>
<jsp:directive.page import="javax.crypto.KeyGenerator"/>
<jsp:directive.page import="javax.crypto.Mac"/>

<jsp:scriptlet>
KeyGenerator kg = KeyGenerator.getInstance("HmacMD5");
kg.init(new SecureRandom("hello".getBytes()));

Mac mac = Mac.getInstance("HmacMD5");
mac.init(kg.generateKey());
byte[] result = mac.doFinal(IOUtils.toByteArray(request.getInputStream()));

String resultStr = new String(Hex.encodeHex(result));

if (resultStr.equals(request.getHeader("Google-Code-Project-Hosting-Hook-Hmac"))) {
} else {
	response.sendError(403);
}
</jsp:scriptlet>
</jsp:root>