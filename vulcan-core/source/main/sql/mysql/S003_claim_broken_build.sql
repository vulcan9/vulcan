create table users (
	id int not null auto_increment primary key,
	username varchar(256) not null	
);

alter table builds add column broken_by_user_id int after status;
alter table builds add column claimed_date timestamp default 0 after broken_by_user_id;
alter table builds add constraint fk_broken_by foreign key (broken_by_user_id) references users (id);

update db_version set version_number=3;
