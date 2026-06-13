alter table app_user
    add column failed_login_attempts integer not null default 0;

alter table app_user
    add column first_failed_login_at timestamp with time zone;

alter table app_user
    add column locked_until timestamp with time zone;

create table auth_rate_limit_bucket (
    id uuid primary key,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    scope varchar(80) not null,
    subject_hash varchar(64) not null,
    client_hash varchar(64) not null,
    request_count integer not null,
    window_started_at timestamp with time zone not null,
    blocked_until timestamp with time zone
);

alter table auth_rate_limit_bucket
    add constraint ux_auth_rate_limit_bucket_scope_subject_client
        unique (scope, subject_hash, client_hash);

create index ix_auth_rate_limit_bucket_scope on auth_rate_limit_bucket (scope);
create index ix_auth_rate_limit_bucket_blocked_until on auth_rate_limit_bucket (blocked_until);
