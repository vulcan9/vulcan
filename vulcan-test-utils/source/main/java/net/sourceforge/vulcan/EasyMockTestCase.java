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
package net.sourceforge.vulcan;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.easymock.IExpectationSetters;
import org.easymock.IMocksControl;

public abstract class EasyMockTestCase extends TestCase {
	@Retention(RetentionPolicy.RUNTIME)
	@Target({ElementType.METHOD})
	protected @interface TrainingMethod {
		String value();
	}

	public interface TestRunner {
		void run() throws Throwable;
		TestCase getInstance();
	}
	
	private static class ReflectionMatcher implements IArgumentMatcher {
			private final Object expected;
	
			public ReflectionMatcher(final Object expected) {
				this.expected = expected;
			}
			
			public boolean matches(Object actual) {
				return EqualsBuilder.reflectionEquals(expected, actual);
			}
			public void appendTo(StringBuffer buffer) {
				buffer.append(expected);
			}
		}

	private final TestRunner internalTestRunner = new TestRunner() {
		public void run() throws Throwable{
			EasyMockTestCase.super.runTest();
		}
		public TestCase getInstance() {
			return EasyMockTestCase.this;
		}
	};
	
	private IMocksControl control = EasyMock.createControl();
	private IMocksControl niceControl = EasyMock.createNiceControl();
	private IMocksControl strictControl = EasyMock.createStrictControl();
	
	private List<Object> mocks = new ArrayList<Object>();
	
	public void trainNoCalls() {
	}
	
	public <T> T createNiceMock(Class<T> toMock) {
		T createMock = niceControl.createMock(toMock);
		mocks.add(createMock);
		return createMock;
	}
	
	public <T> T createMock(Class<T> toMock) {
		T createMock = control.createMock(toMock);
		mocks.add(createMock);
		return createMock;
	}
	
	public <T> T createStrictMock(Class<T> toMock) {
		T createMock = strictControl.createMock(toMock);
		
		mocks.add(createMock);
		return createMock;
	}
	
	public void replay() {
		control.replay();
		niceControl.replay();
		strictControl.replay();
	}
	
	public void reset() {
		control.reset();
		niceControl.reset();
		strictControl.reset();
	}	
	public void verify() {
		control.verify();
		niceControl.verify();
		strictControl.verify();
	}
	public void checkOrder(boolean checkOrder) {
		control.checkOrder(checkOrder);
		niceControl.checkOrder(checkOrder);
		strictControl.checkOrder(checkOrder);
	}
	public void runMockTest(TestRunner testRunner) throws Throwable {
		final boolean trained = train(testRunner.getInstance());
		
		if (trained) {
			replay();
		}
		
		testRunner.run();
		
		if (trained) {
			verify();
		}
	}

	public static <T> IExpectationSetters<T> expect(T value) {
		return EasyMock.expect(value);
	}
	
	public static IExpectationSetters<Object> expectLastCall() {
		return EasyMock.expectLastCall();
	}

	public static Object anyObject() {
		return EasyMock.anyObject();
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T notNull() {
		return (T) EasyMock.notNull();
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

	public static Object reflectionEq(Object argument) {
		EasyMock.reportMatcher(new ReflectionMatcher(argument));
		return null;
	}
	
	@Override
	protected void runTest() throws Throwable {
		runMockTest(internalTestRunner);
	}

	private boolean train(TestCase instance) throws Throwable {
		final Class<? extends TestCase> c = instance.getClass();
		final Method testMethod = c.getMethod(instance.getName(), (Class[])null);
	
		final TrainingMethod trainingMethodAnn = testMethod.getAnnotation(TrainingMethod.class);
		
		if (trainingMethodAnn != null) {
			try {
				final Method trainingMethod = c.getMethod(
					trainingMethodAnn.value(), (Class[])null);
				trainingMethod.invoke(instance, (Object[]) null);
				return true;
			} catch (InvocationTargetException e) {
				throw e.getCause();
			} catch (NoSuchMethodException e) {
				fail("TrainingMethod " + trainingMethodAnn.value() + " does not exist or is not accessible.");
			}
		}
		
		return false;
	}
}
