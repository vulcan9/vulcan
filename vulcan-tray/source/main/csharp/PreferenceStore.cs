using System;

namespace SourceForge.Vulcan.Tray
{
	public interface PreferenceStore
	{
		Preferences Load();
		void Save(Preferences preferences);
	}
}