using System.Collections.Generic;
using System.IO;
using Microsoft.Build.Framework;
using NUnit.Framework;
using NUnit.Framework.SyntaxHelpers;

namespace SourceForge.Vulcan.DotNet
{
	[TestFixture]
	public class MsBuildNodeLoggerTest : MsBuildLoggingTestBase
	{
		private MsBuildNodeLogger logger;

		[SetUp]
		public void SetUp()
		{
			logger = new MsBuildNodeLogger();
			logger.Reporter = this;

			files = new List<string>();
		}

		[Test]
		public void TestSendsAbsoluteFileNameWhenProjectFileSpecifiedInTarget()
		{
			logger.TargetStarted(this, MakeTargetStartedEventArgs("/home/samantha/work/module1/module1.csproj"));
			logger.BuildErrorRaised(this, MakeBuildErrorEventArgs());

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
		}

		[Test]
		public void TestSendsAbsoluteFileThreadSafe()
		{
			TargetStartedEventArgs e1 = MakeTargetStartedEventArgs("/home/samantha/work/module1/module1.csproj");
			TargetStartedEventArgs e2 = MakeTargetStartedEventArgs("/home/samantha/work/module2/module2.csproj");

			e2.BuildEventContext = new BuildEventContext(2, 0, 0, 0);

			logger.TargetStarted(this, e1);
			logger.TargetStarted(this, e2);
			
			logger.BuildErrorRaised(this, MakeBuildErrorEventArgs());

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
			Assert.That(files[0], Text.Contains("module1"));
		}

		[Test]
		public void TestSendsAbsoluteFileThreadSafeUsesStack()
		{
			TargetStartedEventArgs s1 = MakeTargetStartedEventArgs("/home/samantha/work/module1/module1.csproj");
			TargetStartedEventArgs s2 = MakeTargetStartedEventArgs("/home/samantha/work/module2/module2.csproj");

			logger.TargetStarted(this, s1);
			logger.TargetStarted(this, s2);

			TargetFinishedEventArgs f1 = new TargetFinishedEventArgs("", "", "", s2.ProjectFile, s2.ProjectFile, true);
			f1.BuildEventContext = s1.BuildEventContext;
			logger.TargetFinished(this, f1);

			logger.BuildErrorRaised(this, MakeBuildErrorEventArgs());

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.True);
			Assert.That(files[0], Text.Contains("module1"));
		}

		[Test]
		public void BlankOnMissingKey()
		{
			logger.TargetStarted(this, MakeTargetStartedEventArgs("/home/samantha/work/module1/module1.csproj"));

			BuildErrorEventArgs args = MakeBuildErrorEventArgs();
			args.BuildEventContext = new BuildEventContext(33, 33, 33, 33);
			logger.BuildErrorRaised(this, args);

			Assert.That(files.Count, Is.EqualTo(1));
			Assert.That(Path.IsPathRooted(files[0]), Is.False);
		}
	}
}