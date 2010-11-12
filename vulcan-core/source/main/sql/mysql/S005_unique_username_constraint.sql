alter table users add constraint uniq_username unique(username);

update db_version set version_number=5;
