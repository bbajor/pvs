-- Single admin role: former INSTITUTION_ADMIN is now ADMIN (same privileges).
UPDATE user_account_roles SET roles = 'ADMIN' WHERE roles = 'INSTITUTION_ADMIN';
