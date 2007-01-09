<?xml version="1.0" encoding="utf-8"?>
<jsp:root version="2.0"
	xmlns="http://www.w3.org/1999/xhtml"
    xmlns:jsp="http://java.sun.com/JSP/Page">
    
    <jsp:directive.tag display-name="bubble" dynamic-attributes="false"/>

	<jsp:directive.attribute name="styleClass" required="false" type="java.lang.String" rtexprvalue="false"/>
	
	<table class="wrapper ${styleClass}">
		<tbody>
			<tr class="wrapper">
				<td class="wrapper">    
				    <div class="bubble ${styleClass}">
						<div class="upper-left">&amp;nbsp;</div>
						<div class="upper-right">&amp;nbsp;</div>
				
						<jsp:doBody/>
						
						<div class="lower-left">&amp;nbsp;</div>
						<div class="lower-right">&amp;nbsp;</div>
					</div>
				</td>
			</tr>
		</tbody>
	</table>
</jsp:root>
