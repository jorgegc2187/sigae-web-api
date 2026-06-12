alter table loan
    add column created_by uuid references app_user(id),
    add column completed_by uuid references app_user(id);

create index ix_loan_created_by on loan (created_by);
create index ix_loan_completed_by on loan (completed_by);
