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
package net.sourceforge.vulcan.web.struts;

import junit.framework.TestCase;
import net.sourceforge.vulcan.EasyMockTestCase;
import net.sourceforge.vulcan.metadata.SvnRevision;

import org.easymock.EasyMock;
import org.easymock.IExpectationSetters;

import servletunit.struts.MockStrutsTestCase;

@SvnRevision(id="$Id$", url="$HeadURL$")
public abstract class EasyMockStrutsTestCase extends MockStrutsTestCase {
	private EasyMockTestCase easyMockDelegate = new EasyMockTestCase() {};

	private EasyMockTestCase.TestRunner testRunnerDelegate = new EasyMockTestCase.TestRunner() {
		public void run() throws Throwable {
			EasyMockStrutsTestCase.super.runTest();	
		}
		public TestCase getInstance() {
			return EasyMockStrutsTestCase.this;
		}
	};
	
	public <T> T createMock(Class<T> toMock) {
		return easyMockDelegate.createMock(toMock);
	}

	public <T> T createNiceMock(Class<T> toMock) {
		return easyMockDelegate.createNiceMock(toMock);
	}

	public <T> T createStrictMock(Class<T> toMock) {
		return easyMockDelegate.createStrictMock(toMock);
	}
	
	public void replay() {
		easyMockDelegate.replay();
	}

	public void reset() {
		easyMockDelegate.reset();
	}

	public void verify() {
		easyMockDelegate.verify();
	}

	public void trainNoCalls() {
		easyMockDelegate.trainNoCalls();
	}

	@Override
	protected void runTest() throws Throwable {
		easyMockDelegate.runMockTest(testRunnerDelegate);
	}
	
	/* static convenience methods follow */
    public static <T> IExpectationSetters<T> expect(T value) {
        return EasyMock.expect(value);
    }
    
    public static IExpectationSetters<Object> expectLastCall() {
        return EasyMock.expectLastCall();
    }

    public static Object anyObject() {
    	return EasyMock.anyObject();
    }
    
    public static int anyInt() {
    	return EasyMock.anyInt();
    }
    
    public static Object notNull() {
    	return EasyMock.notNull();
    }
    
    @SuppressWarnings("unchecked")
	public static <T> T isNull() {
    	return (T)EasyMock.isNull();
    }

    public static <T> T[] aryEq(T[] arr) {
    	return EasyMock.aryEq(arr);
    }
    
	public static boolean eq(boolean b) {
		return EasyMock.eq(b);
	}

	public static <T> T eq(T obj) {
		return EasyMock.eq(obj);
	}

}
