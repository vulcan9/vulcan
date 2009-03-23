/*
 * Vulcan Build Manager
 * Copyright (C) 2005-2007 Chris Eldredge
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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;
import net.sourceforge.vulcan.TestUtils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.WildcardFilter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

public class JdbcSchemaMigratorTest extends TestCase {
	SingleConnectionDataSource dataSource = new SingleConnectionDataSource();
	JdbcSchemaMigrator migrator = new JdbcSchemaMigrator();
	
	protected void setUp() throws Exception {
		super.setUp();
		final Properties props = new Properties();
		props.setProperty("autocommit", "false");

		dataSource.setDriverClassName("org.hsqldb.jdbcDriver");
		dataSource.setUrl("jdbc:hsqldb:mem:JdbcSchemaMigratorTest");
		dataSource.setUsername("sa");
		dataSource.setPassword("");
		dataSource.setConnectionProperties(props);
		dataSource.setAutoCommit(false);
		
		migrator.setDataSource(dataSource);
		final FileSystemResource resource = resolveSqlScript("create_tables.sql");
		migrator.setCreateTablesScript(resource);
	}

	protected void tearDown() throws Exception {
		new JdbcTemplate(dataSource).execute("shutdown;");
		super.tearDown();
	}

	public void testCreateInitialTables() throws Exception {
		migrator.createJdbcTemplate();
		migrator.createInitialTables();
		
		int count = new JdbcTemplate(dataSource).queryForInt("select count(1) from builds");
		assertEquals(0, count);
	}
	
	public void testUpdateSchemaCreatesTables() throws Exception {
		migrator.setMigrationScripts(new Resource[0]);
		migrator.updateSchema();
		
		int count = new JdbcTemplate(dataSource).queryForInt("select count(1) from builds");
		assertEquals(0, count);
	}
	
	public void testUpdateSchemaDoesNotRecreateTablesWhenPresent() throws Exception {
		migrator.createJdbcTemplate();
		migrator.createInitialTables();
		migrator.setMigrationScripts(new Resource[0]);
		
		migrator.updateSchema();
	}
	
	public void testUpdateSchemaInitializesToVersionOne() throws Exception {
		migrator.setMigrationScripts(getMigrationScripts());
		
		migrator.createJdbcTemplate();
		migrator.createInitialTables();
		
		migrator.updateSchema();
		
		assertTrue("expected version table to exist", migrator.isTablePresent(JdbcSchemaMigrator.VERSION_TABLE_NAME));
	}

	public void testSortsMigrationScripts() throws Exception {
		Resource r1 = new FileSystemResource("S001_foo");
		Resource r2 = new FileSystemResource("S002_foo");
		migrator.setMigrationScripts(new Resource[] {r2, r1});
		
		assertSame(r1, migrator.getMigrationScripts()[0]);
		assertSame(r2, migrator.getMigrationScripts()[1]);
	}
	
	public void testUpdateSchemaDoesNotRerunMigrationScripts() throws Exception {
		migrator.setMigrationScripts(getMigrationScripts());
		migrator.createJdbcTemplate();
		migrator.createInitialTables();
		
		migrator.updateSchema();
		
		assertTrue("expected version table to exist", migrator.isTablePresent(JdbcSchemaMigrator.VERSION_TABLE_NAME));
		
		// should not cause error
		migrator.updateSchema();
	}

	static FileSystemResource resolveSqlScript(String scriptName) {
		final File file = TestUtils.resolveRelativeFile("source/main/sql/hsql/" + scriptName);
		return new FileSystemResource(file);
	}
	
	@SuppressWarnings("unchecked")
	static Resource[] getMigrationScripts() {
		File dir = TestUtils.resolveRelativeFile("source/main/sql/hsql");
		List<Resource> scripts = new ArrayList<Resource>();
		for (File file : (Collection<File>)FileUtils.listFiles(dir, new WildcardFilter("S*.sql"), FalseFileFilter.INSTANCE)) {
			scripts.add(new FileSystemResource(file));
		}
		return scripts.toArray(new Resource[scripts.size()]);
	}

}
