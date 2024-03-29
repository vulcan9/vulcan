use vulcan;

create table project_names (
	id int not null auto_increment primary key,
	name varchar(64) not null,
	constraint uniq_name unique (name)
);

create table builds (
	id int not null auto_increment primary key,
	project_id integer not null,
	
	uuid varchar(38) not null,	
	
	work_dir varchar(2048),
	
	status varchar(5) not null,
	
	message_key varchar(256) not null,
	message_arg_0 varchar(256),
	message_arg_1 varchar(256),
	message_arg_2 varchar(256),
	message_arg_3 varchar(256),
	
	build_reason_key varchar(256),
	build_reason_arg_0 varchar(256),
	build_reason_arg_1 varchar(256),
	build_reason_arg_2 varchar(256),
	build_reason_arg_3 varchar(256),
	
	start_date timestamp not null default 0,
	completion_date timestamp not null default 0,
	
	revision bigint,
	revision_label varchar(32),
	
	-- if the build failed before getting a revision,
	-- the above columns will map to lastKnownRevision
	revision_unavailable boolean default 0,
		
	build_number int not null,
	last_good_build_number int,
	
	tag_name varchar(256),
	repository_url varchar(2048),
	
	status_changed boolean,
	
	scheduled_build boolean,
	
	requested_by varchar(256),
	
	update_type varchar(12),
	
	constraint uniq_project_build_number unique (project_id, build_number),
	constraint uniq_uuid unique (uuid),
	constraint fk_project_id foreign key (project_id) references project_names (id),
	constraint ck_status check (status in ('PASS','FAIL','SKIP','ERROR')),
	constraint ck_update_type check (update_type in ('Full', 'Incremental'))
);

create table build_dependencies (
	build_id integer not null,
	dependency_build_id integer not null,
	
	constraint pk_build_dependencies primary key (build_id, dependency_build_id),
	constraint fk_dependency_build_id foreign key (build_id) references builds (id),
	constraint fk_dependency_id_build_id foreign key (dependency_build_id) references builds (id)
);

create table build_messages (
	build_id integer not null,
	message_type char(1), 
	
	message varchar(1000) not null,
	code varchar(16),
	file varchar(1024),
	line_number integer,
	
	constraint fk_build_message_build_id foreign key (build_id) references builds (id),
	constraint ck_message_type check (message_type in ('E', 'W'))
);

create table metrics (
	build_id integer not null,

	message_key varchar(256) not null,
	data varchar(256),
	metric_type char(1) not null,

	constraint fk_metrics_build_id foreign key (build_id) references builds (id),
	constraint uniq_key_per_build unique (build_id, message_key),
	constraint ck_metric_type check (metric_type in ('N', 'P', 'S'))
);

create table test_failures (
	build_id integer not null,
	
	name varchar(256) not null,
	first_consecutive_build_number integer not null,
	
	constraint fk_test_failures_build_id foreign key (build_id) references builds (id)
);

create table change_sets (
	build_id integer not null,
	change_set_id integer not null,
	
	author varchar(64),
	message varchar(2048),
	revision_label varchar(32),
	commit_timestamp timestamp,	
	
	constraint fk_change_sets_build_id foreign key (build_id) references builds (id),
	constraint pk_change_sets primary key (build_id, change_set_id)
);

create table modified_paths (
	build_id integer not null,
	change_set_id integer not null,
	
	modified_path varchar(1024) not null,
	
	constraint fk_modified_paths_change_set foreign key (build_id, change_set_id) references change_sets (build_id, change_set_id)
);

create index idx_builds_uuid on builds (uuid);
create index idx_builds_project_id on builds (project_id);
create index idx_builds_completion_date on builds (completion_date);
create index idx_builds_work_dir on builds (work_dir);
create index idx_build_deps_build_id on build_dependencies(build_id);
create index idx_build_messages_msg on build_messages (message);
create index idx_build_messages_build_id on build_messages (build_id);
create index idx_metrics_build_id on metrics (build_id);
create index idx_test_failures_build_id on test_failures (build_id);
create index idx_change_sets_build_id on change_sets (build_id);
create index idx_modified_paths_build_id on modified_paths (build_id);
