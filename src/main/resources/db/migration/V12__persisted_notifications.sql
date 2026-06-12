create table app_notification (
    id uuid primary key,
    external_key varchar(160) not null,
    type varchar(60) not null,
    severity varchar(20) not null,
    title varchar(180) not null,
    message text not null,
    route varchar(255) not null,
    occurred_at timestamp with time zone not null,
    active boolean not null default true,
    admin_only boolean not null default false,
    related_location_id uuid null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_app_notification_external_key
    on app_notification (external_key);

create index ix_app_notification_active_occurred_at
    on app_notification (active, occurred_at desc);

create index ix_app_notification_related_location_id
    on app_notification (related_location_id);

create table user_notification_state (
    id uuid primary key,
    notification_id uuid not null references app_notification(id) on delete cascade,
    user_id uuid not null references app_user(id) on delete cascade,
    read_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_user_notification_state_notification_user
    on user_notification_state (notification_id, user_id);

create index ix_user_notification_state_user_id
    on user_notification_state (user_id);
