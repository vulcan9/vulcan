namespace SourceForge.Vulcan.Tray
{
	partial class ConfigForm
	{
		/// <summary>
		/// Required designer variable.
		/// </summary>
		private System.ComponentModel.IContainer components = null;

		/// <summary>
		/// Clean up any resources being used.
		/// </summary>
		/// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
		protected override void Dispose(bool disposing)
		{
			if (disposing && (components != null))
			{
				components.Dispose();
			}
			base.Dispose(disposing);
		}

		#region Windows Form Designer generated code

		/// <summary>
		/// Required method for Designer support - do not modify
		/// the contents of this method with the code editor.
		/// </summary>
		private void InitializeComponent()
		{
			System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(ConfigForm));
			this.btnOk = new System.Windows.Forms.Button();
			this.btnCancel = new System.Windows.Forms.Button();
			this.label1 = new System.Windows.Forms.Label();
			this.url = new System.Windows.Forms.TextBox();
			this.pollingInterval = new System.Windows.Forms.NumericUpDown();
			this.label2 = new System.Windows.Forms.Label();
			this.label3 = new System.Windows.Forms.Label();
			this.chkBubbleFailures = new System.Windows.Forms.CheckBox();
			this.chkBubbleSuccess = new System.Windows.Forms.CheckBox();
			((System.ComponentModel.ISupportInitialize)(this.pollingInterval)).BeginInit();
			this.SuspendLayout();
			// 
			// btnOk
			// 
			this.btnOk.DialogResult = System.Windows.Forms.DialogResult.OK;
			this.btnOk.Location = new System.Drawing.Point(351, 240);
			this.btnOk.Name = "btnOk";
			this.btnOk.Size = new System.Drawing.Size(60, 25);
			this.btnOk.TabIndex = 0;
			this.btnOk.Text = "OK";
			this.btnOk.UseVisualStyleBackColor = true;
			// 
			// btnCancel
			// 
			this.btnCancel.DialogResult = System.Windows.Forms.DialogResult.Cancel;
			this.btnCancel.Location = new System.Drawing.Point(417, 240);
			this.btnCancel.Name = "btnCancel";
			this.btnCancel.Size = new System.Drawing.Size(60, 25);
			this.btnCancel.TabIndex = 1;
			this.btnCancel.Text = "Cancel";
			this.btnCancel.UseVisualStyleBackColor = true;
			// 
			// label1
			// 
			this.label1.AutoSize = true;
			this.label1.Location = new System.Drawing.Point(17, 21);
			this.label1.Name = "label1";
			this.label1.Size = new System.Drawing.Size(65, 13);
			this.label1.TabIndex = 2;
			this.label1.Text = "Vulcan URL";
			// 
			// url
			// 
			this.url.Location = new System.Drawing.Point(93, 18);
			this.url.Name = "url";
			this.url.Size = new System.Drawing.Size(384, 20);
			this.url.TabIndex = 3;
			// 
			// pollingInterval
			// 
			this.pollingInterval.Location = new System.Drawing.Point(93, 46);
			this.pollingInterval.Name = "pollingInterval";
			this.pollingInterval.Size = new System.Drawing.Size(55, 20);
			this.pollingInterval.TabIndex = 4;
			// 
			// label2
			// 
			this.label2.AutoSize = true;
			this.label2.Location = new System.Drawing.Point(17, 48);
			this.label2.Name = "label2";
			this.label2.Size = new System.Drawing.Size(71, 13);
			this.label2.TabIndex = 5;
			this.label2.Text = "Update every";
			// 
			// label3
			// 
			this.label3.AutoSize = true;
			this.label3.Location = new System.Drawing.Point(154, 48);
			this.label3.Name = "label3";
			this.label3.Size = new System.Drawing.Size(47, 13);
			this.label3.TabIndex = 6;
			this.label3.Text = "seconds";
			// 
			// chkBubbleFailures
			// 
			this.chkBubbleFailures.AutoSize = true;
			this.chkBubbleFailures.Checked = true;
			this.chkBubbleFailures.CheckState = System.Windows.Forms.CheckState.Checked;
			this.chkBubbleFailures.Location = new System.Drawing.Point(20, 83);
			this.chkBubbleFailures.Name = "chkBubbleFailures";
			this.chkBubbleFailures.Size = new System.Drawing.Size(144, 17);
			this.chkBubbleFailures.TabIndex = 7;
			this.chkBubbleFailures.Text = "Show bubbles on failures";
			this.chkBubbleFailures.UseVisualStyleBackColor = true;
			// 
			// chkBubbleSuccess
			// 
			this.chkBubbleSuccess.AutoSize = true;
			this.chkBubbleSuccess.Checked = true;
			this.chkBubbleSuccess.CheckState = System.Windows.Forms.CheckState.Checked;
			this.chkBubbleSuccess.Location = new System.Drawing.Point(20, 106);
			this.chkBubbleSuccess.Name = "chkBubbleSuccess";
			this.chkBubbleSuccess.Size = new System.Drawing.Size(150, 17);
			this.chkBubbleSuccess.TabIndex = 8;
			this.chkBubbleSuccess.Text = "Show bubbles on success";
			this.chkBubbleSuccess.UseVisualStyleBackColor = true;
			// 
			// ConfigForm
			// 
			this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
			this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
			this.ClientSize = new System.Drawing.Size(489, 277);
			this.Controls.Add(this.chkBubbleSuccess);
			this.Controls.Add(this.chkBubbleFailures);
			this.Controls.Add(this.label3);
			this.Controls.Add(this.label2);
			this.Controls.Add(this.pollingInterval);
			this.Controls.Add(this.url);
			this.Controls.Add(this.label1);
			this.Controls.Add(this.btnCancel);
			this.Controls.Add(this.btnOk);
			this.Icon = ((System.Drawing.Icon)(resources.GetObject("$this.Icon")));
			this.MaximizeBox = false;
			this.MinimizeBox = false;
			this.Name = "ConfigForm";
			this.ShowInTaskbar = false;
			this.Text = "VulcanTray Settings";
			this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.onClosing);
			((System.ComponentModel.ISupportInitialize)(this.pollingInterval)).EndInit();
			this.ResumeLayout(false);
			this.PerformLayout();

		}

		#endregion

		private System.Windows.Forms.Button btnOk;
		private System.Windows.Forms.Button btnCancel;
		private System.Windows.Forms.Label label1;
		private System.Windows.Forms.TextBox url;
		private System.Windows.Forms.NumericUpDown pollingInterval;
		private System.Windows.Forms.Label label2;
		private System.Windows.Forms.Label label3;
		private System.Windows.Forms.CheckBox chkBubbleFailures;
		private System.Windows.Forms.CheckBox chkBubbleSuccess;
	}
}