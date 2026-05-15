create table loan (
    id uuid primary key,
    code varchar(30) not null,
    teacher_id uuid not null references teacher(id),
    teacher_name_snapshot varchar(160) not null,
    teacher_dni_snapshot varchar(8) not null,
    teacher_specialty_snapshot varchar(120),
    destination_location_id uuid not null references location(id),
    destination_name_snapshot varchar(150) not null,
    loan_date date not null,
    due_date date not null,
    completed_at timestamp with time zone,
    notes text,
    signature_png bytea,
    signature_content_type varchar(80),
    signature_file_name varchar(180),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_loan_code on loan (code);
create index ix_loan_teacher_id on loan (teacher_id);
create index ix_loan_destination_location_id on loan (destination_location_id);
create index ix_loan_completed_at on loan (completed_at);

create table loan_asset (
    id uuid primary key,
    loan_id uuid not null references loan(id) on delete cascade,
    asset_id uuid not null references asset(id),
    asset_code_snapshot varchar(30) not null,
    asset_name_snapshot varchar(160) not null,
    asset_category_snapshot varchar(150) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create unique index ux_loan_asset on loan_asset (loan_id, asset_id);
create index ix_loan_asset_loan_id on loan_asset (loan_id);
create index ix_loan_asset_asset_id on loan_asset (asset_id);

create table loan_attachment (
    id uuid primary key,
    loan_id uuid not null references loan(id) on delete cascade,
    file_name varchar(255) not null,
    mime_type varchar(120) not null,
    size_bytes bigint not null,
    source varchar(32) not null,
    content bytea not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index ix_loan_attachment_loan_id on loan_attachment (loan_id);
