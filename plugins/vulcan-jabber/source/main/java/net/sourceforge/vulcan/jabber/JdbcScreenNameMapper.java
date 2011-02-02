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
package net.sourceforge.vulcan.jabber;

import java.util.HashMap;
import java.util.Map;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jndi.JndiObjectFactoryBean;

public class JdbcScreenNameMapper implements ScreenNameMapper {
	private final static Log LOG = LogFactory.getLog(JdbcScreenNameMapper.class);
	
	private final JdbcScreenNameMapperConfig config;
	
	public JdbcScreenNameMapper(JdbcScreenNameMapperConfig config) {
		this.config = config;
	}

	public Map<String, String> lookupByAuthor(Iterable<String> authors) {
		final Map<String, String> screenNames = new HashMap<String, String>();
		
		try {
			final JdbcTemplate template = createJdbcTemplate();
			
			for (String author : authors) {
				try {
					screenNames.put(author, (String) template.queryForObject(config.getSql(), new Object[] {author}, String.class));
				} catch (IncorrectResultSizeDataAccessException e) {
					LOG.info("No screen name found for commit author " + author);
				}
			}
		} catch (Exception e) {
			LOG.error("Exception looking up screen names in database", e);
		}
		
		return screenNames;
	}

	protected JdbcTemplate createJdbcTemplate() throws NamingException {
		return new JdbcTemplate(findDataSource());
	}

	protected DataSource findDataSource() throws IllegalArgumentException, NamingException {
		final JndiObjectFactoryBean dsFactory = new JndiObjectFactoryBean();
		dsFactory.setJndiName(config.getJndiName());
		dsFactory.afterPropertiesSet();
		
		return (DataSource)dsFactory.getObject();
	}
}
