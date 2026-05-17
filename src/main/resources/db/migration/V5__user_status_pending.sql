alter table app_user
    add constraint ck_app_user_status
    check (status in ('ACTIVE', 'INACTIVE', 'PENDING'));
