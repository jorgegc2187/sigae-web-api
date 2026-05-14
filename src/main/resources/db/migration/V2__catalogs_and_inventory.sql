create table location (
    id uuid primary key,
    name varchar(150) not null,
    description text not null,
    status varchar(16) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_location_name on location (name);

create table supplier (
    id uuid primary key,
    name varchar(150) not null,
    ruc varchar(11),
    email varchar(150),
    phone varchar(20),
    address text,
    status varchar(16) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_supplier_name on supplier (name);
create unique index ux_supplier_ruc on supplier (ruc);

create table teacher (
    id uuid primary key,
    dni varchar(8) not null,
    full_name varchar(160) not null,
    specialty varchar(120),
    email varchar(150),
    phone varchar(20),
    status varchar(16) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_teacher_dni on teacher (dni);

create table asset (
    id uuid primary key,
    code varchar(30) not null,
    name varchar(160) not null,
    asset_type_id uuid not null references asset_type(id),
    location_id uuid not null references location(id),
    supplier_id uuid references supplier(id),
    condition varchar(32) not null,
    serial_number varchar(100),
    barcode varchar(100),
    acquisition_date date,
    notes text,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_asset_code on asset (code);
create unique index ux_asset_barcode on asset (barcode);
create index ix_asset_asset_type_id on asset (asset_type_id);
create index ix_asset_location_id on asset (location_id);
create index ix_asset_supplier_id on asset (supplier_id);

create table asset_attribute_value (
    id uuid primary key,
    asset_id uuid not null references asset(id) on delete cascade,
    attribute_definition_id uuid not null references asset_attribute_definition(id),
    attribute_value text not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_asset_attribute_value on asset_attribute_value (asset_id, attribute_definition_id);

create table asset_traceability (
    id uuid primary key,
    asset_id uuid not null references asset(id) on delete cascade,
    event_type varchar(40) not null,
    description text not null,
    previous_value text,
    new_value text,
    reason text,
    user_id uuid references app_user(id),
    occurred_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index ix_asset_traceability_asset_id on asset_traceability (asset_id);
