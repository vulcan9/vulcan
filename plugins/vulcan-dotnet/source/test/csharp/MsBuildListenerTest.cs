using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using Microsoft.Build.Framework;
using NUnit.Framework;
using NUnit.Framework.SyntaxHelpers;

namespace SourceForge.Vulcan.DotNet
{
	[TestFixture]
	public class MsBuildListenerTest : IBuildMessageReporter
	{
		private MsBuildListener listener;
		private IList<string> files;

		[SetUp]
		public void SetUp()
		{
			listener = new MsBuildListener();
			listener.Reporter = this;

			files = new List<string>();
		}

		[Test]
		public void TestDefaultBehaviorWhenNoProjectFileAvailable()
		{
			listener.BuildErrorRaised(this, new BuildErrorEventArgs("x", "y", @"bar\baz.cs", 22, 0, 0, 0, "error", "", "csc"));

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(files[0], Is.EqualTo(@"bar\baz.cs"));
		}

		[Test]
		public void TestSendsAbsoluteFileNameWhenProjectFileSpecifiedInTarget()
		{
			listener.TargetStarted(this, new TargetStartedEventArgs("foo", "help", "DoStuff", "/home/samantha/work/module1/module1.csproj", "/library/useful-stuff.targets"));
			listener.BuildErrorRaised(this, new BuildErrorEventArgs("x", "y", @"bar\baz.cs", 22, 0, 0, 0, "error", "", "csc"));

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
		}

		[Test]
		public void UsesMostRecentProjectFile()
		{
			listener.TargetStarted(this, new TargetStartedEventArgs("foo", "help", "DoStuff", "/home/samantha/work/module1/module1.csproj", "/library/useful-stuff.targets"));
			listener.TargetStarted(this, new TargetStartedEventArgs("foo", "help", "DoStuff", "/home/samantha/work/module2/module2.csproj", "/library/useful-stuff.targets"));
			listener.BuildErrorRaised(this, new BuildErrorEventArgs("x", "y", @"bar\baz.cs", 22, 0, 0, 0, "error", "", "csc"));

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
			Assert.That(files[0], Text.Contains("module2"));
		}

		[Test]
		public void TestSendsAbsoluteFileThreadSafe()
		{
			TargetStartedEventArgs e1 = CreateTaskStartedEvent("/home/samantha/work/module1/module1.csproj");
			TargetStartedEventArgs e2 = CreateTaskStartedEventWithDifferentThreadId("/home/samantha/work/module2/module2.csproj");

			listener.TargetStarted(this, e1);
			listener.TargetStarted(this, e2);
			
			listener.BuildErrorRaised(this, new BuildErrorEventArgs("x", "y", @"bar\baz.cs", 22, 0, 0, 0, "error", "", "csc"));

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
			Assert.That(files[0], Text.Contains("module1"));
		}

		private delegate TargetStartedEventArgs CreateTaskStartedEventDelegate(string projectFile);

		private TargetStartedEventArgs CreateTaskStartedEvent(string projectFile)
		{
			return new TargetStartedEventArgs("foo", "help", "DoStuff", projectFile, "/library/useful-stuff.targets");
		}

		private TargetStartedEventArgs CreateTaskStartedEventWithDifferentThreadId(string projectFile)
		{
			TargetStartedEventArgs e = null;

			CreateTaskStartedEventDelegate d = CreateTaskStartedEvent;
			d.BeginInvoke(projectFile, delegate(IAsyncResult iar)
			              	{
			              		e = d.EndInvoke(iar);
			              		lock(this)
			              		{
			              			Monitor.Pulse(this);
			              		}
			              	}, null);
			
			lock(this)
			{
				if (e == null)
				{
					Monitor.Wait(this);
				}
			}

			return e;
		}

		[Test]
		public void TestSendsAbsoluteFileNameWhenProjectFileSpecifiedInTask()
		{
			listener.TaskStarted(this, new TaskStartedEventArgs("foo", "help", "DoStuff", "/home/samantha/work/module1/module1.csproj", "/library/useful-stuff.targets"));
			listener.BuildErrorRaised(this, new BuildErrorEventArgs("x", "y", @"bar\baz.cs", 22, 0, 0, 0, "error", "", "csc"));

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
		}

		[Test]
		public void TestAvoidNREWheFileIsNull()
		{
			listener.TaskStarted(this, new TaskStartedEventArgs("foo", "help", "DoStuff", "/home/samantha/work/module1/module1.csproj", "/library/useful-stuff.targets"));
			listener.BuildErrorRaised(this, new BuildErrorEventArgs("x", "y", null, 22, 0, 0, 0, "error", "", "csc"));

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(files[0], Is.Null);
		}

		public void SendMessage(string eventType, string projectName, string targetName, string taskName, string message)
		{
		}

		public void SendMessage(string eventType, MessagePriority priority, string message, string file, int lineNumber, string code)
		{
			files.Add(file);
		}
	}
}