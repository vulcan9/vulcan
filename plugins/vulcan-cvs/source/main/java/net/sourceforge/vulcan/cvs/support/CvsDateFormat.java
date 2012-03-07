/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2012 Chris Eldredge
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package net.sourceforge.vulcan.cvs.support;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/* SimpleDateFormat is not thread safe, so we
 * create a new instance on each parse.
 */
public class CvsDateFormat {
	private static String PATTERN = "yyyy/MM/dd HH:mm:ss";
	
	public static Date parseDate(String dateString) {
		try {
			return new SimpleDateFormat(PATTERN).parse(dateString);
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	public static String format(Date date) {
		return new SimpleDateFormat(PATTERN).format(date);
	}
	
}
