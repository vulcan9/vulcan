/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2006 Chris Eldredge
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class JavaSecurityDigester implements Digester {
	final MessageDigest digest;
	
	public JavaSecurityDigester(String algorithm) {
		try {
			digest = MessageDigest.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
	
	public String digest(byte[] data) {
		digest.reset();
		
		final byte[] result = digest.digest(data);
		
		final StringBuilder sb = new StringBuilder();
		
		for (byte b : result) {
			final String hex = Integer.toHexString(0xff & b);
			if (hex.length() == 1) {
				sb.append("0");
			}
			sb.append(hex);
		}
		
		return sb.toString();
	}
}
