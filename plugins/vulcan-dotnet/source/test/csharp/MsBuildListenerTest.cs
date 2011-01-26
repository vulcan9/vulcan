using System.Collections.Generic;
using System.IO;
using NUnit.Framework;
using NUnit.Framework.SyntaxHelpers;

namespace SourceForge.Vulcan.DotNet
{
	[TestFixture]
	public class MsBuildListenerTest : MsBuildLoggingTestBase
	{
		private MsBuildListener listener;

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
			listener.BuildErrorRaised(this, MakeBuildErrorEventArgs());

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(files[0], Is.EqualTo(@"bar\baz.cs"));
		}

		[Test]
		public void TestSendsAbsoluteFileNameWhenProjectFileSpecifiedInTarget()
		{
			listener.TargetStarted(this, MakeTargetStartedEventArgs("/home/samantha/work/module1/module1.csproj"));
			listener.BuildErrorRaised(this, MakeBuildErrorEventArgs());

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
		}

		[Test]
		public void UsesMostRecentProjectFile()
		{
			listener.TargetStarted(this, MakeTargetStartedEventArgs("/home/samantha/work/module1/module1.csproj"));
			listener.TargetStarted(this, MakeTargetStartedEventArgs("/home/samantha/work/module2/module2.csproj"));
			listener.BuildErrorRaised(this, MakeBuildErrorEventArgs());

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
			Assert.That(files[0], Text.Contains("module2"));
		}

		[Test]
		public void TestSendsAbsoluteFileNameWhenProjectFileSpecifiedInTask()
		{
			listener.TaskStarted(this, MakeTaskStartedEventArgs());
			listener.BuildErrorRaised(this, MakeBuildErrorEventArgs());

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
		}

		[Test]
		public void TestAvoidNREWheFileIsNull()
		{
			listener.TaskStarted(this, MakeTaskStartedEventArgs());
			listener.BuildErrorRaised(this, MakeBuildErrorEventArgs(null));

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(files[0], Is.Null);
		}
	}
}