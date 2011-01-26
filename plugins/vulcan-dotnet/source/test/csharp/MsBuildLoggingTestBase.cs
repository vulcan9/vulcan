using System.Collections.Generic;
using Microsoft.Build.Framework;

namespace SourceForge.Vulcan.DotNet
{
	public class MsBuildLoggingTestBase : IBuildMessageReporter
	{
		protected IList<string> files;

		public void SendMessage(string eventType, string projectName, string targetName, string taskName, string message)
		{
		}

		public void SendMessage(string eventType, MessagePriority priority, string message, string file, int lineNumber, string code)
		{
			files.Add(file);
		}

		protected BuildErrorEventArgs MakeBuildErrorEventArgs()
		{
			return MakeBuildErrorEventArgs(@"bar\baz.cs");
		}

		protected BuildErrorEventArgs MakeBuildErrorEventArgs(string file)
		{
			BuildErrorEventArgs args = new BuildErrorEventArgs("x", "y", file, 22, 0, 0, 0, "error", "", "csc");
			args.BuildEventContext = new BuildEventContext(0, 1, 2, 3);
			return args;
		}

		protected TargetStartedEventArgs MakeTargetStartedEventArgs(string projectFile)
		{
			TargetStartedEventArgs args = new TargetStartedEventArgs("foo", "help", "DoStuff", projectFile, "/library/useful-stuff.targets");
			args.BuildEventContext = new BuildEventContext(0, 1, 2, 3);
			return args;
		}

		protected TaskStartedEventArgs MakeTaskStartedEventArgs()
		{
			TaskStartedEventArgs args = new TaskStartedEventArgs("foo", "help", "DoStuff", "/home/samantha/work/module1/module1.csproj", "/library/useful-stuff.targets");
			args.BuildEventContext = new BuildEventContext(0, 1, 2, 3);
			return args;
		}
	}
}