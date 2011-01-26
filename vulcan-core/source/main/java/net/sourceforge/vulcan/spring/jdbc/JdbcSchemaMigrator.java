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
package net.sourceforge.vulcan.spring.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;

public class JdbcSchemaMigrator {
	static final String VERSION_TABLE_NAME = "db_version";
	static final String PROJECTS_TABLE_NAME = "project_names";
	
	private static final Log log = LogFactory.getLog(JdbcSchemaMigrator.class);
	
	private DataSource dataSource;
	private Resource createTablesScript;

	private JdbcTemplate jdbcTemplate;
	private int version;
	private Resource[] migrationScripts;
	
	public DataSource getDataSource() {
		return dataSource;
	}
	
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public Resource getCreateTablesScript() {
		return createTablesScript;
	}
	
	public void setCreateTablesScript(Resource createTablesScript) {
		this.createTablesScript = createTablesScript;
	}

	public Resource[] getMigrationScripts() {
		return migrationScripts;
	}
	
	public void setMigrationScripts(Resource[] migrationScripts) {
		this.migrationScripts = migrationScripts;
		Arrays.sort(this.migrationScripts, new Comparator<Resource>() {
			public int compare(Resource o1, Resource o2) {
				return o1.getFilename().compareTo(o2.getFilename());
			}
		});
	}
	
	public void updateSchema() throws IOException {
		createJdbcTemplate();
		
		if (isTablePresent(VERSION_TABLE_NAME)) {
			version = loadCurrentSchemaVersion();
		} else {
			version = 0;
		}
		
		if (!isTablePresent(PROJECTS_TABLE_NAME)) {
			log.info("Create initial database tables.");
			createInitialTables();
		}
		
		runMigrationScripts();
		
		log.info("Database schema version is " + version + ".");
	}
	
	void createJdbcTemplate() {
		jdbcTemplate = new JdbcTemplate(dataSource);
	}
	
	boolean isTablePresent(final String tableName) {
		final Boolean b = (Boolean) jdbcTemplate.execute(new StatementCallback() {
			public Object doInStatement(Statement stmt) throws SQLException, DataAccessException {
				ResultSet rs = stmt.getConnection().getMetaData().getTables(null, null, null, new String[]{"TABLE"});
		        while (rs.next()) {
		            if (tableName.equalsIgnoreCase(rs.getString("TABLE_NAME"))) {
		                return true;
		            }
		        }
		        return false;
			}    
	      });
		
		return b.booleanValue();
	}

	int loadCurrentSchemaVersion() {
		return jdbcTemplate.queryForInt("select version_number from db_version");
	}
	
	void createInitialTables() throws IOException {
		executeSql(createTablesScript);
	}

	void runMigrationScripts() throws IOException {
		if (migrationScripts == null) {
			return;
		}
		
		for (int i = version; i<migrationScripts.length; i++) {
			executeSql(migrationScripts[i]);
			version++;
		}
	}

	private void executeSql(final Resource resource) throws IOException {
		final String text = loadResource(resource);
		log.info("Running migration script " + resource.getFilename());
		
		final String[] commands = text.split(";");
		
		for (String command : commands) {
			final String trimmed = command.trim();
			if (StringUtils.isBlank(trimmed)) {
				continue;
			}
			
			jdbcTemplate.execute(new StatementCallback() {
				public Object doInStatement(Statement stmt)	throws SQLException, DataAccessException {
					stmt.execute(trimmed);
					final Connection conn = stmt.getConnection();
					if (!conn.getAutoCommit()) {
						conn.commit();
					}
					return null;
				}
			});
		}
	}

	private static String loadResource(Resource resource) throws IOException {
		InputStream inputStream = null;

		try {
			inputStream = resource.getInputStream();
			return IOUtils.toString(inputStream);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
		}
	}
}
