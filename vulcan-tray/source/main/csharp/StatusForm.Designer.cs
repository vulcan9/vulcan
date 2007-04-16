namespace SourceForge.Vulcan.Tray
{
	partial class StatusForm
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
			this.components = new System.ComponentModel.Container();
			System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(StatusForm));
			this.btnValidate = new System.Windows.Forms.Button();
			this.dataGrid = new System.Windows.Forms.DataGridView();
			this.name = new System.Windows.Forms.DataGridViewTextBoxColumn();
			this.age = new System.Windows.Forms.DataGridViewTextBoxColumn();
			this.buildNumber = new System.Windows.Forms.DataGridViewTextBoxColumn();
			this.revision = new System.Windows.Forms.DataGridViewTextBoxColumn();
			this.status = new System.Windows.Forms.DataGridViewTextBoxColumn();
			this.message = new System.Windows.Forms.DataGridViewTextBoxColumn();
			this.xmlDataSet = new System.Data.DataSet();
			this.projectsDataTable = new System.Data.DataTable();
			this.dataColumn2 = new System.Data.DataColumn();
			this.dataColumn3 = new System.Data.DataColumn();
			this.dataColumn4 = new System.Data.DataColumn();
			this.dataColumn5 = new System.Data.DataColumn();
			this.dataColumn7 = new System.Data.DataColumn();
			this.dataColumn1 = new System.Data.DataColumn();
			this.timer = new System.Windows.Forms.Timer(this.components);
			((System.ComponentModel.ISupportInitialize)(this.dataGrid)).BeginInit();
			((System.ComponentModel.ISupportInitialize)(this.xmlDataSet)).BeginInit();
			((System.ComponentModel.ISupportInitialize)(this.projectsDataTable)).BeginInit();
			this.SuspendLayout();
			// 
			// btnValidate
			// 
			this.btnValidate.Location = new System.Drawing.Point(12, 12);
			this.btnValidate.Name = "btnValidate";
			this.btnValidate.Size = new System.Drawing.Size(75, 20);
			this.btnValidate.TabIndex = 2;
			this.btnValidate.Text = "Refresh";
			this.btnValidate.UseVisualStyleBackColor = true;
			this.btnValidate.Click += new System.EventHandler(this.onRefreshClick);
			// 
			// dataGrid
			// 
			this.dataGrid.AllowUserToAddRows = false;
			this.dataGrid.AllowUserToDeleteRows = false;
			this.dataGrid.AllowUserToOrderColumns = true;
			this.dataGrid.AllowUserToResizeRows = false;
			this.dataGrid.BackgroundColor = System.Drawing.SystemColors.Window;
			this.dataGrid.ColumnHeadersHeightSizeMode = System.Windows.Forms.DataGridViewColumnHeadersHeightSizeMode.AutoSize;
			this.dataGrid.Columns.AddRange(new System.Windows.Forms.DataGridViewColumn[] {
            this.name,
            this.age,
            this.buildNumber,
            this.revision,
            this.status,
            this.message});
			this.dataGrid.Location = new System.Drawing.Point(-1, 50);
			this.dataGrid.Name = "dataGrid";
			this.dataGrid.ReadOnly = true;
			this.dataGrid.RowHeadersVisible = false;
			this.dataGrid.RowTemplate.ReadOnly = true;
			this.dataGrid.Size = new System.Drawing.Size(759, 216);
			this.dataGrid.TabIndex = 3;
			this.dataGrid.CellDoubleClick += new System.Windows.Forms.DataGridViewCellEventHandler(this.onCellDoubleClick);
			// 
			// name
			// 
			this.name.DataPropertyName = "name";
			this.name.HeaderText = "Project Name";
			this.name.Name = "name";
			this.name.ReadOnly = true;
			// 
			// age
			// 
			this.age.DataPropertyName = "age";
			this.age.HeaderText = "Age";
			this.age.Name = "age";
			this.age.ReadOnly = true;
			// 
			// buildNumber
			// 
			this.buildNumber.DataPropertyName = "build-number";
			this.buildNumber.HeaderText = "Build Number";
			this.buildNumber.Name = "buildNumber";
			this.buildNumber.ReadOnly = true;
			// 
			// revision
			// 
			this.revision.DataPropertyName = "revision";
			this.revision.HeaderText = "Revision";
			this.revision.Name = "revision";
			this.revision.ReadOnly = true;
			// 
			// status
			// 
			this.status.DataPropertyName = "status";
			this.status.HeaderText = "Status";
			this.status.Name = "status";
			this.status.ReadOnly = true;
			// 
			// message
			// 
			this.message.DataPropertyName = "message";
			this.message.HeaderText = "Message";
			this.message.Name = "message";
			this.message.ReadOnly = true;
			this.message.Width = 300;
			// 
			// xmlDataSet
			// 
			this.xmlDataSet.DataSetName = "ProjectStatusDataSet";
			this.xmlDataSet.Tables.AddRange(new System.Data.DataTable[] {
            this.projectsDataTable});
			// 
			// projectsDataTable
			// 
			this.projectsDataTable.Columns.AddRange(new System.Data.DataColumn[] {
            this.dataColumn2,
            this.dataColumn3,
            this.dataColumn4,
            this.dataColumn5,
            this.dataColumn7,
            this.dataColumn1});
			this.projectsDataTable.TableName = "project";
			// 
			// dataColumn2
			// 
			this.dataColumn2.ColumnMapping = System.Data.MappingType.Attribute;
			this.dataColumn2.ColumnName = "name";
			this.dataColumn2.ReadOnly = true;
			// 
			// dataColumn3
			// 
			this.dataColumn3.Caption = "status";
			this.dataColumn3.ColumnName = "status";
			this.dataColumn3.ReadOnly = true;
			// 
			// dataColumn4
			// 
			this.dataColumn4.Caption = "build-number";
			this.dataColumn4.ColumnName = "build-number";
			this.dataColumn4.DataType = typeof(int);
			this.dataColumn4.ReadOnly = true;
			// 
			// dataColumn5
			// 
			this.dataColumn5.Caption = "message";
			this.dataColumn5.ColumnName = "message";
			this.dataColumn5.ReadOnly = true;
			// 
			// dataColumn7
			// 
			this.dataColumn7.ColumnName = "revision";
			// 
			// dataColumn1
			// 
			this.dataColumn1.Caption = "age";
			this.dataColumn1.ColumnName = "age";
			// 
			// timer
			// 
			this.timer.Interval = 6000;
			this.timer.Tick += new System.EventHandler(this.onTick);
			// 
			// StatusForm
			// 
			this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
			this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
			this.ClientSize = new System.Drawing.Size(758, 266);
			this.Controls.Add(this.dataGrid);
			this.Controls.Add(this.btnValidate);
			this.Icon = ((System.Drawing.Icon)(resources.GetObject("$this.Icon")));
			this.MaximizeBox = false;
			this.MaximumSize = new System.Drawing.Size(766, 300);
			this.MinimumSize = new System.Drawing.Size(766, 300);
			this.Name = "StatusForm";
			this.Text = "Vulcan Status";
			this.Load += new System.EventHandler(this.onLoad);
			((System.ComponentModel.ISupportInitialize)(this.dataGrid)).EndInit();
			((System.ComponentModel.ISupportInitialize)(this.xmlDataSet)).EndInit();
			((System.ComponentModel.ISupportInitialize)(this.projectsDataTable)).EndInit();
			this.ResumeLayout(false);

		}

		#endregion

		private System.Windows.Forms.Button btnValidate;
		private System.Windows.Forms.DataGridView dataGrid;
		private System.Data.DataSet xmlDataSet;
		private System.Data.DataTable projectsDataTable;
		private System.Data.DataColumn dataColumn2;
		private System.Data.DataColumn dataColumn3;
		private System.Data.DataColumn dataColumn4;
		private System.Data.DataColumn dataColumn5;
		private System.Windows.Forms.Timer timer;
		private System.Data.DataColumn dataColumn7;
		private System.Windows.Forms.DataGridViewTextBoxColumn name;
		private System.Windows.Forms.DataGridViewTextBoxColumn age;
		private System.Windows.Forms.DataGridViewTextBoxColumn buildNumber;
		private System.Windows.Forms.DataGridViewTextBoxColumn revision;
		private System.Windows.Forms.DataGridViewTextBoxColumn status;
		private System.Windows.Forms.DataGridViewTextBoxColumn message;
		private System.Data.DataColumn dataColumn1;
	}
}

