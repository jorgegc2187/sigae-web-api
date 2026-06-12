drop index if exists ux_asset_barcode;

alter table asset
    drop column if exists barcode;
