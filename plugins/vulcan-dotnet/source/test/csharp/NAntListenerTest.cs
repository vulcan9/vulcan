using NAnt.Core;
using NUnit.Framework;
using NUnit.Framework.SyntaxHelpers;

namespace SourceForge.Vulcan.DotNet
{
	[TestFixture]
	public class NAntListenerTest
	{
		private NAntListener lis;
		private BuildEventArgs args;
		private MessagePriority prio;
		private string file;
		private int lineNumber;
		private string code;
		private string message;

		[SetUp]
		public void SetUp()
		{
			lis = new NAntListener();
			args = new BuildEventArgs();
			args.MessageLevel = Level.Info;
			args.Message = "Some message about stuff";
		}

		[Test]
		public void ParseMessageDefault()
		{
			lis.ParseMessage(args, out prio, out file, out lineNumber, out code, out message);

			Assert.That(prio, Is.EqualTo(MessagePriority.INFO));
			Assert.That(message, Is.EqualTo(args.Message));
		}

		[Test]
		public void ParseError()
		{
			args.Message = @"c:\work space\vulcan\plugins\vulcan-dotnet\source\test\nant-workdir\SyntaxErrors.cs(5,15): error CS0029: Cannot implicitly convert type 'int' to 'string'";
			lis.ParseMessage(args, out prio, out file, out lineNumber, out code, out message);

			Assert.That(prio, Is.EqualTo(MessagePriority.ERROR));
			Assert.That(file, Is.EqualTo(@"c:\work space\vulcan\plugins\vulcan-dotnet\source\test\nant-workdir\SyntaxErrors.cs"));
			Assert.That(lineNumber, Is.EqualTo(5));
			Assert.That(message, Is.EqualTo("Cannot implicitly convert type 'int' to 'string'"));
			Assert.That(code, Is.EqualTo("CS0029"));
		}

		[Test]
		public void ParseWarning()
		{
			args.Message = "/work/dir/SyntaxErrors.cs(12321,15): warning XYZ123: Stuff and things";
			lis.ParseMessage(args, out prio, out file, out lineNumber, out code, out message);

			Assert.That(prio, Is.EqualTo(MessagePriority.WARNING));
			Assert.That(file, Is.EqualTo("/work/dir/SyntaxErrors.cs"));
			Assert.That(lineNumber, Is.EqualTo(12321));
			Assert.That(message, Is.EqualTo("Stuff and things"));
			Assert.That(code, Is.EqualTo("XYZ123"));
		}
		[Test]
		public void ParseWarningNoCode()
		{
			args.Message = "/work/dir/SyntaxErrors.cs(12321,15): warning: Stuff and things";
			lis.ParseMessage(args, out prio, out file, out lineNumber, out code, out message);

			Assert.That(prio, Is.EqualTo(MessagePriority.WARNING));
			Assert.That(file, Is.EqualTo("/work/dir/SyntaxErrors.cs"));
			Assert.That(lineNumber, Is.EqualTo(12321));
			Assert.That(message, Is.EqualTo("Stuff and things"));
			Assert.That(code, Is.Empty);
		}

		[Test]
		public void ParseWarningNoColumn()
		{
			args.Message = "/work/dir/SyntaxErrors.cs(12321): warning: Stuff and things";
			lis.ParseMessage(args, out prio, out file, out lineNumber, out code, out message);

			Assert.That(prio, Is.EqualTo(MessagePriority.WARNING));
			Assert.That(file, Is.EqualTo("/work/dir/SyntaxErrors.cs"));
			Assert.That(lineNumber, Is.EqualTo(12321));
			Assert.That(message, Is.EqualTo("Stuff and things"));
			Assert.That(code, Is.Empty);
		}

		[Test]
		public void ParseWarningNoLine()
		{
			args.Message = "/work/dir/SyntaxErrors.cs: warning: Stuff and things";
			lis.ParseMessage(args, out prio, out file, out lineNumber, out code, out message);

			Assert.That(prio, Is.EqualTo(MessagePriority.WARNING));
			Assert.That(file, Is.EqualTo("/work/dir/SyntaxErrors.cs"));
			Assert.That(lineNumber, Is.EqualTo(0));
			Assert.That(message, Is.EqualTo("Stuff and things"));
			Assert.That(code, Is.Empty);
		}
	}
}