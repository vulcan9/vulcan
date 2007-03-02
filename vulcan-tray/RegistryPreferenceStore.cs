using Microsoft.Win32;

namespace SourceForge.Vulcan.Tray
{
	public class RegistryPreferenceStore : PreferenceStore
	{
		RegistryKey key;
		
		public RegistryPreferenceStore()
		{
			key = Registry.CurrentUser.OpenSubKey(@"Software\VulcanTray", true);
			
			if (key == null)
			{
				key = Registry.CurrentUser.CreateSubKey(@"Software\VulcanTray");
			}
		}
		
		public Preferences Load()
		{
			Preferences preferences = new Preferences();

			preferences.Url = (string) key.GetValue("Url", "http://vulcan.example.com");
			preferences.Interval = (int) key.GetValue("Interval", 60000);

			return preferences;
		}

		public void Save(Preferences preferences)
		{
			key.SetValue("Url", preferences.Url, RegistryValueKind.String);
			key.SetValue("Interval", preferences.Interval, RegistryValueKind.DWord);
		}
	}
}