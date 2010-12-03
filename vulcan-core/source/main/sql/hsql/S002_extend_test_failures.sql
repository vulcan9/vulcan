alter table test_failures add column message varchar(1024) before first_consecutive_build_number;
alter table test_failures add column details varchar(4096) before first_consecutive_build_number;

update db_version set version_number=2;
