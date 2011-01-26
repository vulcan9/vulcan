using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.Text;
using System.Windows.Forms;

namespace SourceForge.Vulcan.Tray.source.main.csharp
{
	public partial class MultipleLabelSelectForm : Form
	{
		public MultipleLabelSelectForm()
		{
			InitializeComponent();
		}

		public IList<string> Show(IList<string> availableLabels, IList<string> selectedLabels)
		{
			BindLabels(availableLabels, selectedLabels);

			this.ShowDialog();

			if(this.DialogResult == DialogResult.Cancel)
			{
				return selectedLabels;
			}

			return GetCheckedLabels();
		}

		private IList<string> GetCheckedLabels()
		{
			List<string> CheckedLabels = new List<string>();

			for(int i = 0; i < clbAvailableLabels.CheckedItems.Count; i++)
			{
				CheckedLabels.Add(clbAvailableLabels.CheckedItems[i] as string);
			}

			return CheckedLabels;
		}

		private void BindLabels(IList<string> availableLabels, IList<string> selectedLabels)
		{
			foreach (string currentLabel in availableLabels)
			{
				clbAvailableLabels.Items.Add(currentLabel);
			}

			foreach (string currentSelectedLabel in selectedLabels)
			{
				for(int i = 0; i < clbAvailableLabels.Items.Count; i++)
				{
					if(currentSelectedLabel == clbAvailableLabels.Items[i] as string)
					{
						clbAvailableLabels.SetItemChecked(i, true);
					}
				}
			}
		}

		private void btnOK_Click(object sender, EventArgs e)
		{
			this.DialogResult = DialogResult.OK;
			this.Hide();
		}

		private void btnCancel_Click(object sender, EventArgs e)
		{
			this.DialogResult = DialogResult.Cancel;
			this.Hide();
		}
	}
}