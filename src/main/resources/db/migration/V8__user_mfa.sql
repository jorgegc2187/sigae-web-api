create table user_mfa_settings (
  id uuid primary key,
  user_id uuid not null unique references app_user(id) on delete cascade,
  mfa_required boolean not null default false,
  mfa_enabled boolean not null default false,
  totp_secret_encrypted text,
  mfa_enabled_at timestamp,
  created_at timestamp not null,
  updated_at timestamp not null
);

create table mfa_challenge (
  id uuid primary key,
  user_id uuid not null references app_user(id) on delete cascade,
  token_hash varchar(128) not null unique,
  purpose varchar(16) not null,
  encrypted_totp_secret text,
  expires_at timestamp not null,
  consumed_at timestamp,
  failed_attempts integer not null default 0,
  created_at timestamp not null,
  updated_at timestamp not null
);

create index idx_mfa_challenge_user_id on mfa_challenge(user_id);
create index idx_mfa_challenge_token_hash on mfa_challenge(token_hash);
