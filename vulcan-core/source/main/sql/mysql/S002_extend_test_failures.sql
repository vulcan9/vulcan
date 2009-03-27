alter table test_failures add column message varchar(1024) after name;
alter table test_failures add column details varchar(4096) after message;

update db_version set version_number=2;
