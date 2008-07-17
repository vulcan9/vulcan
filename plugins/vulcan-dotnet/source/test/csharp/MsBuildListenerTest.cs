using System.Collections.Generic;
using System.IO;
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
		public void TestSendsAbsoluteFileNameWhenProjectFileSpecifiedInTask()
		{
			listener.TaskStarted(this, new TaskStartedEventArgs("foo", "help", "DoStuff", "/home/samantha/work/module1/module1.csproj", "/library/useful-stuff.targets"));
			listener.BuildErrorRaised(this, new BuildErrorEventArgs("x", "y", @"bar\baz.cs", 22, 0, 0, 0, "error", "", "csc"));

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
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