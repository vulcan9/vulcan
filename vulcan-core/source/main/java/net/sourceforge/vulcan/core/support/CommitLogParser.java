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
package net.sourceforge.vulcan.core.support;

import static org.apache.commons.lang.StringUtils.*;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sourceforge.vulcan.metadata.SvnRevision;

import org.jdom.Content;
import org.jdom.Element;
import org.jdom.Text;

@SvnRevision(id="$Id: AbstractProjectDomBuilder.java 161 2006-12-05 22:19:21Z chris.eldredge $", url="$HeadURL: https://vulcan.googlecode.com/svn/main/trunk/source/main/java/net/sourceforge/vulcan/core/support/AbstractProjectDomBuilder.java $")
class CommitLogParser {
	private static String urlPattern = "\\w+://\\S+";

	private final Element messageNode = new Element("message");
	
	private String keywordPattern = "";
	private Pattern idPattern;
	
	void parse(String message) {
		if (isNotBlank(keywordPattern)) {
			keywordPattern = "(?:" + keywordPattern + "|" + urlPattern + ")"; 
		} else {
			keywordPattern = urlPattern;
		}
		
		parse(message, Pattern.compile(keywordPattern).matcher(message), true);
	}

	void setIdPattern(String idPattern) {
		if (isNotBlank(idPattern)) {
			this.idPattern = Pattern.compile(idPattern);
		} else {
			this.idPattern = null;
		}
	}
	
	void setKeywordPattern(String keywordPattern) {
		this.keywordPattern = keywordPattern;
	}
	
	Element getMessageNode() {
		return messageNode;
	}
	
	@SuppressWarnings("unchecked")
	List<Content> getContents() {
		return messageNode.getContent();
	}
	
	private void parse(String message, Matcher matcher, boolean recurse) {
		int lastEnd = 0;
		
		while (matcher.find()) {
			int issueStart = matcher.start();

			final String wholeMatch = matcher.group(0);
			Matcher idMatcher = null;
			
			if (recurse && idPattern != null && !isLink(wholeMatch)) {
				idMatcher = idPattern.matcher(wholeMatch);
				if (idMatcher.find()) {
					idMatcher.reset();
				} else {
					idMatcher = null;
				}
			}

			if (lastEnd < issueStart) {
				appendText(message.substring(lastEnd, issueStart));
			}

			if (idMatcher != null) {
				parse(wholeMatch, idMatcher, false);				
			} else {
				appendIssueOrLinkNode(wholeMatch, matcher.group(0));
			}
			
			lastEnd = matcher.end();
		}
		
		if (lastEnd < message.length()) {
			appendText(message.substring(lastEnd));
		}
	}

	private void appendText(String string) {
		final int contentSize = messageNode.getContentSize();
		if (contentSize > 0) {
			final Content content = messageNode.getContent(contentSize-1);
			if (content instanceof Text) {
				final Text text = (Text) content;
				text.setText(text.getText() + string);
				return;
			}
		}
		
		messageNode.addContent(new Text(string));
	}

	private void appendIssueOrLinkNode(String text, String issueId) {
		final Element node;
		final String trailingPunctuation;
		
		if (isLink(text)) {
			node = new Element("link");
			
			int index = indexOfLastNonPunctuation(text);
			
			if (index != text.length() - 1) {
				trailingPunctuation = text.substring(index+1);
				text = text.substring(0, index+1);
			} else {
				trailingPunctuation = null;
			}
		} else {
			node = new Element("issue");
			node.setAttribute("issue-id", issueId);
			trailingPunctuation = null;
		}
		
		node.setText(text);
		
		messageNode.addContent(node);
		
		if (trailingPunctuation != null) {
			appendText(trailingPunctuation);
		}
	}

	private boolean isLink(String text) {
		return text.indexOf("://") > 0;
	}

	private int indexOfLastNonPunctuation(String text) {
		int i = text.length();
		boolean isPunc;
		do {
			isPunc = false;
			i--;
			
			char c = text.charAt(i);
			if (c=='.' || c==')' || c==']' || c==',' || c==';') {
				isPunc = true;
			}
		} while (isPunc);
		return i;
	}
}
