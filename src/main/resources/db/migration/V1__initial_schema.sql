create table app_user (
    id uuid primary key,
    full_name varchar(160) not null,
    email varchar(320) not null,
    password_hash varchar(255) not null,
    role varchar(32) not null,
    status varchar(16) not null,
    last_access_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_app_user_email on app_user (email);

create table refresh_token (
    id uuid primary key,
    user_id uuid not null references app_user(id) on delete cascade,
    token_hash varchar(128) not null,
    expires_at timestamp with time zone not null,
    revoked_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_refresh_token_token_hash on refresh_token (token_hash);

create table password_reset_request (
    id uuid primary key,
    user_id uuid not null references app_user(id) on delete cascade,
    token_hash varchar(128) not null,
    expires_at timestamp with time zone not null,
    used_at timestamp with time zone null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index ix_password_reset_request_user_id on password_reset_request (user_id);
create index ix_password_reset_request_token_hash on password_reset_request (token_hash);

create table category (
    id uuid primary key,
    name varchar(120) not null,
    icon varchar(80) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_category_name on category (name);

create table asset_type (
    id uuid primary key,
    category_id uuid not null references category(id) on delete cascade,
    name varchar(120) not null,
    icon varchar(80) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_asset_type_category_name on asset_type (category_id, name);

create table asset_attribute_definition (
    id uuid primary key,
    asset_type_id uuid not null references asset_type(id) on delete cascade,
    name varchar(120) not null,
    description varchar(255) not null,
    is_required boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index ix_asset_attribute_definition_asset_type_id on asset_attribute_definition (asset_type_id);
