/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2009 Chris Eldredge
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.MimeMessage;
import javax.xml.transform.TransformerException;

import net.sourceforge.vulcan.core.ProjectDomBuilder;
import net.sourceforge.vulcan.core.ProjectNameChangeListener;
import net.sourceforge.vulcan.dto.ChangeLogDto;
import net.sourceforge.vulcan.dto.ChangeSetDto;
import net.sourceforge.vulcan.dto.PluginConfigDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto;
import net.sourceforge.vulcan.dto.ProjectStatusDto.Status;
import net.sourceforge.vulcan.event.BuildCompletedEvent;
import net.sourceforge.vulcan.event.BuildStartingEvent;
import net.sourceforge.vulcan.event.ErrorEvent;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.NoSuchTransformFormatException;
import net.sourceforge.vulcan.integration.BuildManagerObserverPlugin;
import net.sourceforge.vulcan.integration.ConfigurablePlugin;
import net.sourceforge.vulcan.mailer.dto.ConfigDto;
import net.sourceforge.vulcan.mailer.dto.ProfileDto;
import net.sourceforge.vulcan.mailer.dto.ProfileDto.Policy;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.DocType;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.jdom.transform.JDOMResult;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.xml.sax.SAXException;

public class EmailPlugin implements BuildManagerObserverPlugin, ConfigurablePlugin, ProjectNameChangeListener, ApplicationContextAware {
    ProjectDomBuilder projectDomBuilder;
    EventHandler eventHandler;
    MessageAssembler messageAssembler;
    String cssLocation;
    String stylesheet;
    ApplicationContext ctx;

    ConfigDto config = new ConfigDto();
    Map<String, List<ProfileDto>> subscribers = new HashMap<String, List<ProfileDto>>();
    Session mailSession;
    String cssRules;

    public String getId() {
        return ConfigDto.PLUGIN_ID;
    }

    public String getName() {
        return ConfigDto.PLUGIN_NAME;
    }

    public PluginConfigDto getConfiguration() {
        return config;
    }

    public synchronized void setConfiguration(PluginConfigDto config) {
        this.config = (ConfigDto) config;
        createMailSession();
        hashProfiles();
    }

    public synchronized void init() throws Exception {
        if (!StringUtils.isBlank(cssLocation)) {
            InputStream is;

            try {
                is = ctx.getResource(cssLocation).getInputStream();
            } catch (Exception e) {
                is = ctx.getParent().getResource(cssLocation).getInputStream();
            }

            try {
                cssRules = IOUtils.toString(is);
            } finally {
                is.close();
            }
        }

    }

    public synchronized void destroy() throws Exception {
    }

    public void onBuildStarting(BuildStartingEvent event) {
    }
    
    public synchronized void onBuildCompleted(BuildCompletedEvent event) {
        final ProjectStatusDto status = event.getStatus();

        final Map<Locale, List<String>> subscribers = getSubscribedAddresses(status);

        if (mailSession == null || subscribers == null) {
            return;
        }

        final ProjectConfigDto projectConfig = event.getProjectConfig();

        final ClassLoader prev = Thread.currentThread().getContextClassLoader();

        try {
            Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
            sendMessages(event, projectConfig, subscribers);
        } finally {
            Thread.currentThread().setContextClassLoader(prev);
        }

    }

    public synchronized void projectNameChanged(String oldName, String newName) {
        final ProfileDto[] profiles = config.getProfiles();

        for (int i = 0; i < profiles.length; i++) {
            final String[] projects = profiles[i].getProjects();
            for (int j = 0; j < projects.length; j++) {
                if (oldName.equals(projects[j])) {
                    projects[j] = newName;
                }
            }
        }
        hashProfiles();
    }

    public ProjectDomBuilder getProjectDomBuilder() {
        return projectDomBuilder;
    }

    public void setProjectDomBuilder(ProjectDomBuilder projectDomBuilder) {
        this.projectDomBuilder = projectDomBuilder;
    }

    public EventHandler getEventHandler() {
        return eventHandler;
    }

    public void setEventHandler(EventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public MessageAssembler getMessageAssembler() {
        return messageAssembler;
    }

    public void setMessageAssembler(MessageAssembler messageAssembler) {
        this.messageAssembler = messageAssembler;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.ctx = applicationContext;
    }

    public String getCssLocation() {
        return cssLocation;
    }

    public void setCssLocation(String cssLocation) {
        this.cssLocation = cssLocation;
    }

    public String getStylesheet() {
		return stylesheet;
	}
    
    public void setStylesheet(String stylesheet) {
		this.stylesheet = stylesheet;
	}
    
    void sendMessage(MimeMessage message) throws AddressException, MessagingException {
        Transport.send(message);
    }

    Map<Locale, List<String>> getSubscribedAddresses(ProjectStatusDto status) {
        final List<ProfileDto> profiles = this.subscribers.get(status.getName());

        if (profiles != null) {
            final Map<Locale, List<String>> map = new HashMap<Locale, List<String>>();

            for (ProfileDto profile : profiles) {
                if (matchPolicy(status.getStatus(), profile, status.isStatusChanged())) {
                    final Locale locale;

                    if (StringUtils.isBlank(profile.getLocale())) {
                        locale = null;
                    } else {
                        locale = new Locale(profile.getLocale());
                    }

                    final List<String> addresses;

                    if (map.containsKey(locale)) {
                        addresses = map.get(locale);
                    } else {
                        addresses = new ArrayList<String>();
                        map.put(locale, addresses);
                    }

                    List<String> addressList = getEmailAddresses(status, profile);

                    for (String addr : addressList) {
                        final String trimmed = addr.trim();
                        if (trimmed.length() > 0) {
                            addresses.add(trimmed);
                        }
                    }

                    if (addresses.isEmpty()) {
                        map.remove(locale);
                    }
                }
            }
            if (map.size() > 0) {
                return map;
            }
        }
        return null;
    }

    protected List<String> getEmailAddresses(ProjectStatusDto status, ProfileDto profile) {
        if (profile.isOnlyEmailChangeAuthors()) {
            ChangeLogDto changeLog = status.getChangeLog();
            if (changeLog == null) {
            	return Collections.emptyList();
            }
            
            final Set<String> addresses = new LinkedHashSet<String>();
            final List<ChangeSetDto> changeSets = changeLog.getChangeSets();

            final Map<String, String> map = getChangeAuthorEmailMap();

            final String[] profileAddresses = profile.getEmailAddresses();
            for (ChangeSetDto changeSet : changeSets) {
                final String author = changeSet.getAuthor().trim();
                if (!map.containsKey(author)) {
                	continue;
                }
                final String address = map.get(author);
				if (ArrayUtils.contains(profileAddresses, address)) {
                    addresses.add(address);
                }
            }
            return new ArrayList<String>(addresses);
        }
        return Arrays.asList(profile.getEmailAddresses());
    }

    /*
     * TODO: we need some validation for the mapping strings
     */
    protected Map<String, String> getChangeAuthorEmailMap() {
        String[] mappings = config.getRepositoryEmailMappings();
        Map<String, String> map = new HashMap<String, String>();
        for (String mapping : mappings) {
            String[] keyValue = mapping.trim().split("=");
            map.put(keyValue[0], keyValue[1]);
        }
        return map;
    }

    private void sendMessages(BuildCompletedEvent event, final ProjectConfigDto projectConfig, final Map<Locale, List<String>> subscribers) {
        for (Map.Entry<Locale, List<String>> ent : subscribers.entrySet()) {
            try {
                final URL sandboxURL = generateSandboxURL(projectConfig);
                final URL statusURL = generateStatusURL();
                URL trackerURL = null;

                if (StringUtils.isNotBlank(projectConfig.getBugtraqUrl())) {
                    trackerURL = new URL(projectConfig.getBugtraqUrl());
                }

                final Document document = projectDomBuilder.createProjectDocument(event.getStatus(), ent.getKey());

                final String content = generateXhtml(document, sandboxURL, statusURL, trackerURL, ent.getKey());

                final MimeMessage message = messageAssembler.constructMessage(StringUtils.join(ent.getValue().iterator(), ","), config, event.getStatus(), content);

                sendMessage(message);
            } catch (AddressException e) {
                eventHandler.reportEvent(new ErrorEvent(this, "errors.address.exception", new Object[] { e.getRef(), e.getMessage() }, e));
            } catch (MessagingException e) {
                eventHandler.reportEvent(new ErrorEvent(this, "errors.messaging.exception", new Object[] { e.getMessage() }, e));
            } catch (Exception e) {
                eventHandler.reportEvent(new ErrorEvent(this, "errors.exception", new Object[] { e.getMessage() }, e));
            }
        }
    }

    private boolean matchPolicy(Status status, ProfileDto profile, boolean statusChanged) {
        final List<Policy> policy = Arrays.asList(profile.getPolicy());

        if (policy.contains(Policy.ALWAYS)) {
            return true;
        }

        if (profile.isOnlyOnChange() && !statusChanged) {
            return false;
        }

        return policy.contains(Policy.valueOf(status.name()));
    }

    private void hashProfiles() {
        subscribers.clear();
        for (ProfileDto profile : config.getProfiles()) {
            for (String projectName : profile.getProjects()) {
                List<ProfileDto> profiles = this.subscribers.get(projectName);
                if (profiles == null) {
                    profiles = new ArrayList<ProfileDto>();
                    this.subscribers.put(projectName, profiles);
                }
                profiles.add(profile);
            }
        }
    }

    private void createMailSession() {
        final String smtpHost = config.getSmtpHost();

        if (smtpHost != null && smtpHost.length() > 0) {
            final Properties props = new Properties();

            props.setProperty("mail.host", smtpHost);

            mailSession = Session.getInstance(props);
        } else {
            mailSession = null;
        }
        messageAssembler.setMailSession(mailSession);
    }

    private String generateXhtml(Document projectDom, URL projectSiteURL, URL statusURL, URL trackerURL, Locale locale) throws SAXException, IOException, TransformerException,
            NoSuchTransformFormatException, MalformedURLException {
        final JDOMResult xhtmlResult = new JDOMResult();

        final Map<String, ? super Object> params = new HashMap<String, Object>();
        
        params.put("showBuildDirectory", Boolean.FALSE);
        
		if (projectSiteURL != null) {
			params.put("projectSiteURL", projectSiteURL.toExternalForm());
		}
		if (statusURL != null) {
			params.put("viewProjectStatusURL", statusURL.toExternalForm());	
		}
		if (trackerURL != null) {
			params.put("issueTrackerURL", trackerURL.toExternalForm());	
		}

		projectDomBuilder.transform(projectDom, params, locale, stylesheet, xhtmlResult);

        final Document xhtmlDom = xhtmlResult.getDocument();

        xhtmlDom.setDocType(new DocType("html", "-//W3C//DTD XHTML 1.1//EN", "http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd"));

        addStyle(xhtmlDom.getRootElement());

        final OutputStream os = new ByteArrayOutputStream();

        final XMLOutputter out = new XMLOutputter(Format.getRawFormat());
        out.output(xhtmlDom, os);

        return os.toString();
    }

    private void addStyle(Element xhtmlDom) {
        if (StringUtils.isBlank(cssRules)) {
            return;
        }

        final Element head = (Element) xhtmlDom.getContent().get(0);

        if (head == null) {
            throw new IllegalStateException("xhtml document does not have <head>");
        }

        final Element style = new Element("style");
        style.setAttribute("type", "text/css");
        style.setText(cssRules);

        head.addContent(style);
    }

    private URL generateSandboxURL(ProjectConfigDto projectConfig) throws MalformedURLException {
        final StringBuilder buf = getVulcanRootURL();

        buf.append("site/");
        buf.append(projectConfig.getName());

        return new URL(buf.toString());
    }

    private URL generateStatusURL() throws MalformedURLException {
        final StringBuilder buf = getVulcanRootURL();

        buf.append("projects/");

        return new URL(buf.toString());
    }

    private StringBuilder getVulcanRootURL() {
        final String vulcanUrl = config.getVulcanUrl();

        if (StringUtils.isBlank(vulcanUrl)) {
            return new StringBuilder("http://localhost/vulcan");
        }

        final StringBuilder buf = new StringBuilder(vulcanUrl);

        if (!vulcanUrl.endsWith("/")) {
            buf.append('/');
        }

        return buf;
    }
}
