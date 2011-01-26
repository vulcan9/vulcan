/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2011 Chris Eldredge
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
package net.sourceforge.vulcan.mailer;

import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.util.Date;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.mailer.dto.ConfigDto;

public class MessageAssembler {
	private Session mailSession;
	
	public void setMailSession(Session mailSession) {
		this.mailSession = mailSession;
	}
	
	public MimeMessage constructMessage(String subscribers, ConfigDto config,
			ProjectStatusDto status, String html) throws MessagingException, AddressException {
		
		final MimeMessage message = new MimeMessage(mailSession);
		
		message.setSentDate(new Date());
		message.setFrom(new InternetAddress(config.getSenderAddress()));
		
		if (isNotBlank(config.getReplyToAddress())) {
			message.setReplyTo(InternetAddress.parse(config.getReplyToAddress()));
		}
		message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(subscribers));
		
		message.setSubject(status.getName() + ": " + status.getStatus());
		
		final Multipart multipart = new MimeMultipart();
		
		html = html.replaceAll("\\r", "");
		
		addMultipartBody(multipart, "text/html; charset=UTF-8", html);
		
		message.setContent(multipart);
		
		return message;
	}
	private void addMultipartBody(Multipart multipart, String type, String content) throws MessagingException {
		MimeBodyPart bp = new MimeBodyPart();
		
		bp.setContent(content, type);
		
		multipart.addBodyPart(bp);
	}
}
