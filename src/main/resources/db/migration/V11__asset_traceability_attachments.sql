create table asset_traceability_attachment (
    id uuid primary key,
    traceability_id uuid not null references asset_traceability(id) on delete cascade,
    file_name varchar(255) not null,
    mime_type varchar(120) not null,
    size_bytes bigint not null,
    content bytea not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index ix_asset_traceability_attachment_traceability_id
    on asset_traceability_attachment (traceability_id);
