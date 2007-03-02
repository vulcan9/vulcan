namespace SourceForge.Vulcan.Tray
{
	public class Preferences
	{
		private string url;
		private int interval;

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
		
		public Preferences Clone()
		{
			return (Preferences) MemberwiseClone();
		}
	}
}