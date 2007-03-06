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
namespace SourceForge.Vulcan.DotNet {
	using System;
	using NUnit.Framework;
		
	[TestFixture]
	public class PacketBuilderTest
	{
		[Test]
		public void Simple() {
			PacketBuilder pb = new PacketBuilder();
			
			pb.Write("hello");
			
			byte[] data = pb.ToArray();
			
			Assert.AreEqual(6, data.Length);
			Assert.AreEqual((byte) 'h', data[0]);
			Assert.AreEqual((byte) 'e', data[1]);
			Assert.AreEqual((byte) 'l', data[2]);
			Assert.AreEqual((byte) 'l', data[3]);
			Assert.AreEqual((byte) 'o', data[4]);
			Assert.AreEqual((byte) 0, data[5]);
		}
		
		[Test]
		public void Longer() {
			PacketBuilder pb = new PacketBuilder();
			
			pb.Write("hello123hello123hello123hello123hello123hello123hello123hello123" +
				"hello123hello123hello123hello123hello123hello123hello123hello123" +
				"hello123hello123hello123hello123hello123hello123hello123hello123" +
				"hello123hello123hello123hello123hello123hello123hello123hello123" +
				"hello123hello123hello123hello123hello123hello123hello123hello12");
			
			byte[] data = pb.ToArray();
			
			Assert.AreEqual(320, data.Length);
			Assert.AreEqual(0, data[319]);
		}
		
		[Test]
		public void Null() {
			PacketBuilder pb = new PacketBuilder();
			
			pb.Write(null);
			
			byte[] data = pb.ToArray();
			
			Assert.AreEqual(1, data.Length);
			Assert.AreEqual(0, data[0]);
		}
		
		[Test]
		public void Reset() {
			PacketBuilder pb = new PacketBuilder();
			
			pb.Write("hello");
			
			Assert.AreEqual(6, pb.ToArray().Length);
			
			pb.Reset();
			
			Assert.AreEqual(0, pb.ToArray().Length);
		}
	}	
}