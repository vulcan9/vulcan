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
package net.sourceforge.vulcan.web.struts.forms;

import net.sourceforge.vulcan.dto.SchedulerConfigDto;
import net.sourceforge.vulcan.metadata.SvnRevision;
import net.sourceforge.vulcan.web.struts.forms.SchedulerConfigForm;
import junit.framework.TestCase;


@SvnRevision(id="$Id$", url="$HeadURL$")
public class SchedulerConfigFormTest extends TestCase {
	SchedulerConfigForm form = new SchedulerConfigForm();
	
	@Override
	public void setUp() throws Exception {
		form.populate(new SchedulerConfigDto(), false);
	}
	public void testSetsMultiplierZero() throws Exception {
		doTest(0, "0", "0");
	}
	public void testSetsMultiplierSeconds() throws Exception {
		doTest(1000, "1", "1000");
		doTest(5000, "5", "1000");
	}
	public void testSetsMultiplierMinutes() throws Exception {
		doTest(60000, "1", "60000");
		doTest(300000, "5", "60000");
	}
	public void testSetsMultiplierHours() throws Exception {
		doTest(3600000, "1", "3600000");
		doTest(18000000, "5", "3600000");
	}
	public void testSetsMultiplierDays() throws Exception {
		doTest(86400000, "1", "86400000");
		doTest(432000000, "5", "86400000");
	}
	private void doTest(long interval, String scalar, String multiplier) {
		form.getSchedulerConfig().setInterval(interval);
		form.doAfterPopulate();
		assertEquals(scalar, form.getIntervalScalar());
		assertEquals(multiplier, form.getIntervalMultiplier());
	}
}
