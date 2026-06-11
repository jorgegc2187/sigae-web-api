alter table asset
    add column decommissioned_at timestamp with time zone;

update asset
set decommissioned_at = current_timestamp
where condition = 'DADO_DE_BAJA';
