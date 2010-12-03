alter table builds add column work_dir_vcs_clean boolean default false after work_dir;

update db_version set version_number=4;
