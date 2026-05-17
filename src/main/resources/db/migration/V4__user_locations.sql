create table user_location (
    user_id uuid not null references app_user(id) on delete cascade,
    location_id uuid not null references location(id) on delete cascade,
    primary key (user_id, location_id)
);

create index ix_user_location_location_id on user_location (location_id);
