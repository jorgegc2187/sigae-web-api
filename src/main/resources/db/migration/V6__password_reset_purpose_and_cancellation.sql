ALTER TABLE password_reset_request
  ADD COLUMN purpose VARCHAR(32) NOT NULL DEFAULT 'PASSWORD_RESET';

ALTER TABLE password_reset_request
  ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE;
