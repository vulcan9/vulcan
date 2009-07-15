package net.sourceforge.vulcan.web;

import java.util.Locale;

import org.springframework.web.context.WebApplicationContext;

public class XalanFunctions {
	private static WebApplicationContext wac;

	public static String formatMessage(String code, String arg1, String locale) {
		return wac.getMessage(code, new String[] {arg1}, new Locale(locale));
	}

	public static void setWebApplicationContext(WebApplicationContext wac) {
		XalanFunctions.wac = wac;
	}
}
