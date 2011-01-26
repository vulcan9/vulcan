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
package net.sourceforge.vulcan.spring;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.MockApplicationContext;
import net.sourceforge.vulcan.PluginManager;
import net.sourceforge.vulcan.TestUtils;
import net.sourceforge.vulcan.core.BeanEncoder;
import net.sourceforge.vulcan.dto.PluginMetaDataDto;
import net.sourceforge.vulcan.dto.ProjectConfigDto;
import net.sourceforge.vulcan.dto.RevisionTokenDto;
import net.sourceforge.vulcan.dto.StateManagerConfigDto;
import net.sourceforge.vulcan.event.Event;
import net.sourceforge.vulcan.event.EventHandler;
import net.sourceforge.vulcan.exception.DuplicatePluginIdException;
import net.sourceforge.vulcan.exception.InvalidPluginLayoutException;
import net.sourceforge.vulcan.exception.StoreException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;

public class SpringFileStoreTest extends EasyMockTestCase {
	class SpringFileStoreExt extends SpringFileStore {
		PluginMetaDataDto createPluginConfigWrapper(File f) throws IOException {
			return super.createPluginConfig(f);
		}
	}
	
	SpringFileStoreExt store;
	File configFile = new File(System.getProperty("java.io.tmpdir"), "config.xml");
	File mockPluginDir;
	File pluginsDir;
	File projectsDir;
	String osName;
	
	MockApplicationContext ctx = new MockApplicationContext();
	PluginManager pluginMgr;
	BeanEncoder beanEncoder;

	final ProjectConfigDto config = new ProjectConfigDto();
	
	final RevisionTokenDto rev = new RevisionTokenDto();

	@Override
	public void setUp() throws Exception {
		store = new SpringFileStoreExt();
		store.setConfigRoot(System.getProperty("java.io.tmpdir"));
		osName = System.getProperty("os.name");
		pluginsDir = new File(store.getConfigRoot(), "plugins");
		pluginsDir.mkdirs();
		mockPluginDir = new File(pluginsDir, "mock");
		projectsDir = new File(store.getConfigRoot(), "projects");
		
		store.setEventHandler(new EventHandler() {
			public void reportEvent(Event event) {
			}
		});
		
		pluginMgr = createStrictMock(PluginManager.class);
		ctx.registerSingleton("pluginManager", pluginMgr);

		beanEncoder = createStrictMock(BeanEncoder.class);
		
		store.setBeanEncoder(beanEncoder);
		store.setBeanFactory(ctx);
		
		config.setName("a name");
	}
	
	@Override
	public void tearDown() throws Exception {
		System.setProperty("os.name", osName);
		configFile.delete();
		FileUtils.deleteDirectory(pluginsDir);
		FileUtils.deleteDirectory(projectsDir);
		FileUtils.deleteDirectory(new File(System.getProperty("user.home") + File.separator + "Application Data" + File.separator + "vulcan-junit"));
		FileUtils.deleteDirectory(new File(System.getProperty("user.home") + File.separator + "vulcan-junit"));
		FileUtils.deleteDirectory(new File(store.getConfigRoot(), "buildLogs"));
	}
	
	public void testGetExportMimeType() throws Exception {
		assertEquals("application/xml", store.getExportMimeType());
	}
	
	public void testExport() throws Exception {
		OutputStream os = new ByteArrayOutputStream();
		
		store.exportConfiguration(os);
		os.close();
		
		assertTrue(os.toString().length() > 0);
	}
	
	public void testCreatesFile() throws Exception {
		assertFalse(configFile.exists());
		
		store.storeConfiguration(new StateManagerConfigDto());
		
		assertTrue(configFile.exists());
	}
	
	public void testDoesNotOverwriteIfNotChanged() throws Exception {
		store.storeConfiguration(new StateManagerConfigDto());

		final long firstModification = 242143000l;
		
		assertTrue("configFile.setLastModified() failed.", configFile.setLastModified(firstModification));

		int beforeCount = FileUtils.listFiles(configFile.getParentFile(), new PrefixFileFilter("config"), FalseFileFilter.INSTANCE).size();
		
		store.storeConfiguration(new StateManagerConfigDto());
		
		assertEquals(firstModification, configFile.lastModified());
		
		// should delete temp file
		assertEquals(beforeCount, FileUtils.listFiles(configFile.getParentFile(), new PrefixFileFilter("config"), FalseFileFilter.INSTANCE).size());
	}
	
	public void testCreatesNestedPath() throws Exception {
		final File parent = new File(System.getProperty("user.home"), "vulcan-junit");
		final File root = new File(parent, "config-dir");

		tryToDelete(root);
		tryToDelete(parent);

		try {
			assertFalse(parent.isDirectory());
			assertFalse(root.isDirectory());
			
			store.setConfigRoot(System.getProperty("user.home") + "/vulcan-junit/config-dir");
			store.init();
			
			assertTrue(parent.isDirectory());
			assertTrue(root.isDirectory());
		} finally {
			tryToDelete(root);
			tryToDelete(parent);
		}
	}
	
	public void testLoadDefaultIfConfigFileNotPresent() throws Exception {
		store.setConfigRoot("${user.home}/vulcan-junit");
		
		assertFalse(new File(store.getConfigRoot(), "config.xml").exists());
		
		assertNotNull(store.loadConfiguration());
		
		assertFalse(new File(store.getConfigRoot(), "config.xml").exists());
	}
	
	public void testExtractCorruptPlugin() throws Exception {
		final File zip = TestUtils.resolveRelativeFile("source/test/pluginTests/corrupt.zip");
		assertTrue(zip.exists());
		
		try {
			store.extractPlugin(new FileInputStream(zip));
			fail("expected exception");
		} catch (StoreException e) {
		}
	}
	
	public void testExtractPlugin() throws Exception {
		try {
			final File zip = TestUtils.resolveRelativeFile("source/test/pluginTests/mockPlugin.zip");
			assertTrue(zip.exists());
			
			final InputStream is = new FileInputStream(zip);
			
			assertFalse(mockPluginDir.isDirectory());
			
			final PluginMetaDataDto plugin = store.extractPlugin(is);
			assertEquals(1, plugin.getClassPath().length);
			assertTrue(plugin.getClassPath()[0].toString().endsWith("mock/token.jar"));
			assertEquals(mockPluginDir, plugin.getDirectory());
			
			assertTrue(mockPluginDir.isDirectory());
		} finally {
			FileUtils.deleteDirectory(mockPluginDir);
		}
	}
	
	public void testExtractPluginVersionDescriptorNotFirstEntry() throws Exception {
		try {
			final File zip = TestUtils.resolveRelativeFile("source/test/pluginTests/mockPluginUnordered.zip");
			assertTrue(zip.exists());
			
			final InputStream is = new FileInputStream(zip);
			
			assertFalse(mockPluginDir.isDirectory());
			
			final PluginMetaDataDto plugin = store.extractPlugin(is);
			assertEquals(1, plugin.getClassPath().length);
			assertTrue(plugin.getClassPath()[0].toString().endsWith("mock/token.jar"));
			assertEquals(mockPluginDir, plugin.getDirectory());
			
			assertTrue(mockPluginDir.isDirectory());
		} finally {
			FileUtils.deleteDirectory(mockPluginDir);
		}
	}
	
	public void testExtractPluginNoTopLevel() throws Exception {
		final File zip = TestUtils.resolveRelativeFile("source/test/pluginTests/flat.zip");
		assertTrue(zip.exists());
		
		final InputStream is = new FileInputStream(zip);
		
		assertEquals(0, pluginsDir.list().length);
		
		try {
			store.extractPlugin(is);
			fail("expected exception");
		} catch (InvalidPluginLayoutException e) {
			
		}
		assertEquals(0, pluginsDir.list().length);
	}
	
	public void testExtractPluginEmpty() throws Exception {
		final File zip = TestUtils.resolveRelativeFile("source/test/pluginTests/empty.zip");
		assertTrue(zip.exists());
		
		final InputStream is = new FileInputStream(zip);
		
		assertEquals(0, pluginsDir.list().length);
		
		try {
			store.extractPlugin(is);
			fail("expected exception");
		} catch (InvalidPluginLayoutException e) {
			
		}
		assertEquals(0, pluginsDir.list().length);
	}
	
	public void testExtractPluginMoreThanOneTopLevel() throws Exception {
		final File zip = TestUtils.resolveRelativeFile("source/test/pluginTests/toomanydirs.zip");
		assertTrue(zip.exists());
		
		final InputStream is = new FileInputStream(zip);
		
		assertEquals(0, pluginsDir.list().length);
		
		try {
			store.extractPlugin(is);
			fail("expected exception");
		} catch (InvalidPluginLayoutException e) {
			
		}
		assertEquals(0, pluginsDir.list().length);
	}
	
	public void testExtractPluginFileInRootAfterTopLevel() throws Exception {
		final File zip = TestUtils.resolveRelativeFile("source/test/pluginTests/fileinroot.zip");
		assertTrue(zip.exists());
		
		final InputStream is = new FileInputStream(zip);
		
		assertEquals(0, pluginsDir.list().length);
		
		try {
			store.extractPlugin(is);
			fail("expected exception");
		} catch (InvalidPluginLayoutException e) {
			
		}
		assertEquals(0, pluginsDir.list().length);
	}
	
	public void testExtractPluginTwiceThrows() throws Exception {
		final File zip = TestUtils.resolveRelativeFile("source/test/pluginTests/mockPlugin.zip");
		assertTrue(zip.exists());
		
		assertFalse(mockPluginDir.isDirectory());

		InputStream is = new FileInputStream(zip);
		store.extractPlugin(is);

		assertTrue(mockPluginDir.isDirectory());
		
		is = new FileInputStream(zip);
		try {
			store.extractPlugin(is);
			fail("expected exception");
		} catch (DuplicatePluginIdException e) {
			assertEquals("mock", e.getId());
		}
		
		assertTrue(mockPluginDir.isDirectory());
	}
	
	public void testExtractPluginOverOldVersion() throws Exception {
		final File zip = TestUtils.resolveRelativeFile("source/test/pluginTests/mockPlugin.zip");
		assertTrue(zip.exists());
		
		assertTrue(mockPluginDir.mkdirs());
		
		final File versionFile = new File(mockPluginDir, "plugin-version.xml");
		final OutputStream os = new FileOutputStream(versionFile);
		try {
			IOUtils.copy(new ByteArrayInputStream("<vulcan-version-descriptor pluginRevision=\"1\"/>".getBytes()), os);
		} finally {
			os.close();
		}

		final File shouldBeDeleted = new File(mockPluginDir, "shouldBeDeleted");
		FileUtils.touch(shouldBeDeleted);
		
		InputStream is = new FileInputStream(zip);
		store.extractPlugin(is);
		
		assertTrue(mockPluginDir.isDirectory());
		
		is = new FileInputStream(versionFile);
		try {
			assertEquals("<vulcan-version-descriptor pluginRevision=\"354\"/>",
				IOUtils.toString(is).trim());
		} finally {
			is.close();
		}
		
		assertTrue(versionFile.exists());
		assertFalse("Did not delete old file", shouldBeDeleted.exists());
	}
	
	public void testLoadsPluginVersion() throws Exception {
		final File versionFile = new File(mockPluginDir, "plugin-version.xml");
		mockPluginDir.mkdirs();
		
		final OutputStream os = new FileOutputStream(versionFile);
		try {
			IOUtils.copy(new ByteArrayInputStream("<vulcan-version-descriptor pluginRevision=\"1\" version=\"a.v.t.y\"/>".getBytes()), os);
		} finally {
			os.close();
		}

		final PluginMetaDataDto info = store.createPluginConfigWrapper(mockPluginDir);
		
		assertEquals(1, info.getRevision());
		assertEquals("a.v.t.y", info.getVersion());
	}
	
	public void testDeletePlugin() throws Exception {
		store.extractPlugin(new FileInputStream(TestUtils.resolveRelativeFile("source/test/pluginTests/mockPlugin.zip")));
		
		assertTrue(mockPluginDir.exists());
		
		store.deletePlugin("mock");
		
		assertFalse(new File(mockPluginDir, "mock").exists());
	}
	
	public void testDeletePluginFailsSchedulesForLater() throws Exception {
		store.extractPlugin(new FileInputStream(TestUtils.resolveRelativeFile("source/test/pluginTests/mockPlugin.zip")));
		
		assertTrue(mockPluginDir.exists());
		
		final File lock = new File(mockPluginDir, "lock");
		assertTrue(lock.createNewFile());
		final InputStream is = new FileInputStream(lock);

		try {
			try {
				store.deletePlugin("mock");
				
				// fail is not called here, because unix will happily delete the files.
				return;
			} catch (StoreException e) {
				assertTrue(e.getMessage().startsWith("Unable to delete file:"));
				assertTrue(mockPluginDir.exists());
				assertTrue(new File(mockPluginDir, ".delete").exists());
			}
		} finally {
			is.close();
			lock.delete();
		}
	}
	
	public void testGetPluginURLsDeletesMarkedPlugin() throws Exception {
		store.extractPlugin(new FileInputStream(TestUtils.resolveRelativeFile("source/test/pluginTests/mockPlugin.zip")));
		new File(mockPluginDir, ".delete").createNewFile();
		
		assertTrue(mockPluginDir.exists());
		
		assertEquals(0, store.getPluginConfigs().length);
		
		assertFalse(mockPluginDir.exists());
	}
	
	public void testDeleteInvalidThrows() {
		try {
			store.deletePlugin("mock");
			fail("expected exception");
		} catch (StoreException e) {
			assertSame(FileNotFoundException.class, e.getCause().getClass());
		}
	}
	
	public void testGetPluginDirs() throws Exception {
		assertFalse(mockPluginDir.exists());
		
		PluginMetaDataDto[] plugins = store.getPluginConfigs();
		
		assertNotNull(plugins);
		assertEquals(0, plugins.length);
		
		store.extractPlugin(new FileInputStream(TestUtils.resolveRelativeFile("source/test/pluginTests/mockPlugin.zip")));
		
		plugins = store.getPluginConfigs();
		
		assertNotNull(plugins);
		assertEquals(1, plugins.length);
		assertEquals(1, plugins[0].getClassPath().length);
		assertEquals(new File(mockPluginDir, "token.jar").toURI().toURL(), plugins[0].getClassPath()[0]);
	}

	public void testGetOutcomeIds() throws Exception {
		final File dir1 = new File(projectsDir + File.separator + "fake", "outcomes");
		final File dir2 = new File(projectsDir + File.separator + "fakey", "outcomes");
		
		final UUID u1 = UUID.fromString("aff75fcf-7be4-11da-ad42-53827a9f1c52");
		final UUID u2 = UUID.fromString("9ff75fcf-7be4-11da-ad42-53827a9f1c52");
		final UUID u3 = UUID.fromString("9ff75fcf-7be4-11da-ad42-53827a9f1c52");
		
		dir1.mkdirs();
		dir2.mkdirs();
		
		FileUtils.touch(new File(dir1, u1.toString()));
		FileUtils.touch(new File(dir1, u2.toString()));
		FileUtils.touch(new File(dir2, u3.toString()));
		
		final Map<String, List<UUID>> ids = store.getBuildOutcomeIDs();
		
		assertEquals(2, ids.size());
		assertTrue(ids.containsKey("fakey"));
		assertTrue(ids.containsKey("fake"));
		
		assertTrue(ids.get("fake").contains(u1));
		assertTrue(ids.get("fake").contains(u2));
		assertTrue(ids.get("fakey").contains(u3));
	}
	
	public void testRenamesProjectDirOnProjectNameChanged() throws Exception {
		final File dir1 = new File(projectsDir + File.separator + "old-fake", "outcomes");
		final File dir2 = new File(projectsDir + File.separator + "new-fake", "outcomes");
		
		dir1.mkdirs();

		store.projectNameChanged("old-fake", "new-fake");
		
		assertFalse(dir1.exists());
		assertTrue(dir2.exists());
	}
	
	public void testGetChangeLogOutputStream() throws Exception {
		File f1 = store.getChangeLog("myProject", UUID.randomUUID());

		assertTrue(f1.getParentFile().exists());
		assertFalse(f1.exists());
	}
	
	public void testGetBuildLogOutputStream() throws Exception {
		File f1 = store.getBuildLog("myProject", UUID.randomUUID());
		assertTrue(f1.getParentFile().exists());
		assertFalse(f1.exists());
	}
	
	private void tryToDelete(File file) throws Exception {
		final long maxSleep = 1000;
		final long interval = 10;
		long total = 0;
		while (total < maxSleep) {
			if (!file.exists() || file.delete()) {
				return;
			} else if (file.isDirectory()) {
				try {
					FileUtils.deleteDirectory(file);
					return;
				} catch (IOException e) {
				}
			}
			System.gc();
			Thread.sleep(interval);
			total += interval;
		}
		fail("could not delete " + file);
	}
}
