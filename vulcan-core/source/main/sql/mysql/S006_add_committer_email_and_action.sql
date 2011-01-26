alter table change_sets add column author_email varchar(256) default null after author;

alter table modified_paths add column modification_type char(1) default null;
alter table modified_paths add constraint ck_modification_type check (modification_type in ('A', 'R', 'M'));

update db_version set version_number=6;
