namespace SourceForge.Vulcan.Tray
{
	public class Preferences
	{
		private string url;
		private int interval;
		private bool bubbleFailures;
		private bool bubbleSuccess;
		
		public string Url
		{
			get { return url; }
			set { url = value; }
		}

		public int Interval
		{
			get { return interval; }
			set { interval = value; }
		}

		public bool BubbleFailures
		{
			get { return bubbleFailures; }
			set { bubbleFailures = value; }
		}

		public bool BubbleSuccess
		{
			get { return bubbleSuccess; }
			set { bubbleSuccess = value; }
		}

		public Preferences Clone()
		{
			return (Preferences) MemberwiseClone();
		}
	}
}