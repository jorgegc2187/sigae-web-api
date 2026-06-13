alter table asset
    add column created_by uuid;

alter table asset
    add constraint fk_asset_created_by
        foreign key (created_by) references app_user(id);

create index ix_asset_created_by on asset (created_by);
