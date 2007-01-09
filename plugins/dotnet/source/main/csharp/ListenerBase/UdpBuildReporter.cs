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
using System;
using System.Net;
using System.Net.Sockets;
using System.Reflection;

namespace SourceForge.Vulcan.DotNet {
	public class UdpBuildReporter	{
		const int MAX_MESSAGE_LENGTH = 4096;
		const string MESSAGE_TRUNCATED_SUFFIX = "...";

		private Socket sendSocket;
		private EndPoint endPoint;
		private PacketBuilder packetBuilder;

		public UdpBuildReporter(string host, int destinationPort)
		{
			sendSocket = new Socket(AddressFamily.InterNetwork,
				SocketType.Dgram, ProtocolType.Udp);
			
			IPAddress dstAddr = System.Net.Dns.GetHostByName(host).AddressList[0];

			endPoint = new IPEndPoint(dstAddr, destinationPort);

			packetBuilder = new PacketBuilder();
		}

		public void SendMessage(string eventType, string projectName, string targetName,
				string taskName, string message) {

			packetBuilder.Reset();
			packetBuilder.Write(eventType);
			packetBuilder.Write(projectName);
			packetBuilder.Write(targetName);
			packetBuilder.Write(taskName);
			packetBuilder.Write(((int)MessagePriority.INFO).ToString());
			packetBuilder.Write(null);	// file
			packetBuilder.Write(null);	// lineNumber
			packetBuilder.Write(null);	// code
			packetBuilder.Write(TruncateIfNeeded(message));

			SendData(packetBuilder.ToArray());
		}

		public void SendMessage(string eventType, MessagePriority priority, string message, string file, 
			int lineNumber, string code)
		{
			packetBuilder.Reset();
			packetBuilder.Write(eventType);
			packetBuilder.Write(null);	// projectName
			packetBuilder.Write(null);	// targetName
			packetBuilder.Write(null);	// taskName
			packetBuilder.Write(((int)priority).ToString());
			packetBuilder.Write(file);
			packetBuilder.Write(lineNumber.ToString());
			packetBuilder.Write(code);
			packetBuilder.Write(TruncateIfNeeded(message));

			SendData(packetBuilder.ToArray());			
		}
		
		private void SendData(byte[] data) {
			sendSocket.SendTo(data, data.Length, SocketFlags.None, endPoint);
		}

		private string TruncateIfNeeded(string s) {
			if (s != null && s.Length > MAX_MESSAGE_LENGTH) {
				System.Text.StringBuilder sb = new System.Text.StringBuilder(
					s.Substring(0,
							MAX_MESSAGE_LENGTH - MESSAGE_TRUNCATED_SUFFIX.Length));
							
				sb.Append(MESSAGE_TRUNCATED_SUFFIX);
			
				return sb.ToString();
			}
		
			return s;
		}
	}
}
