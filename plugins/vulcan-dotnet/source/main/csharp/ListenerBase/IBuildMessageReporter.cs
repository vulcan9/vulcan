namespace SourceForge.Vulcan.DotNet
{
	public interface IBuildMessageReporter
	{
		void SendMessage(string eventType, string projectName, string targetName,
		                 string taskName, string message);

		void SendMessage(string eventType, MessagePriority priority, string message, string file, 
		                 int lineNumber, string code);
	}
}