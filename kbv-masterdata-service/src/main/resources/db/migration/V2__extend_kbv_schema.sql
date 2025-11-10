-- Extended KBV schema with full data models and historization

-- Extend kbv_icd_entry with all required fields
alter table kbv_icd_entry
  add column if not exists coding_type integer,
  add column if not exists print_indicator integer,
  add column if not exists quarter varchar(10),
  add column if not exists version varchar(50);

-- Create index for quarter lookups
create index if not exists idx_kbv_icd_entry_quarter on kbv_icd_entry(quarter);
create index if not exists idx_kbv_icd_entry_valid_from_to on kbv_icd_entry(valid_from, valid_to);

-- Kostenträgerstammdatei
create table if not exists kbv_cost_carrier (
  id serial primary key,
  code varchar(50) not null,
  name varchar(500) not null,
  valid_from date not null,
  valid_to date,
  quarter varchar(10),
  version varchar(50),
  unique(code, quarter, valid_from)
);
create index if not exists idx_kbv_cost_carrier_code on kbv_cost_carrier(code);
create index if not exists idx_kbv_cost_carrier_quarter on kbv_cost_carrier(quarter);
create index if not exists idx_kbv_cost_carrier_valid_from_to on kbv_cost_carrier(valid_from, valid_to);

-- Versicherungsstammdatei
create table if not exists kbv_insurance (
  id serial primary key,
  code varchar(50) not null,
  name varchar(500) not null,
  valid_from date not null,
  valid_to date,
  quarter varchar(10),
  version varchar(50),
  unique(code, quarter, valid_from)
);
create index if not exists idx_kbv_insurance_code on kbv_insurance(code);
create index if not exists idx_kbv_insurance_quarter on kbv_insurance(quarter);
create index if not exists idx_kbv_insurance_valid_from_to on kbv_insurance(valid_from, valid_to);

-- Import-Historie
create table if not exists kbv_import_history (
  id serial primary key,
  quarter varchar(10) not null,
  version varchar(50) not null,
  import_type varchar(50) not null, -- 'ICD', 'COST_CARRIER', 'INSURANCE', 'FULL'
  status varchar(50) not null, -- 'RUNNING', 'SUCCESS', 'FAILED'
  started_at timestamp not null,
  completed_at timestamp,
  records_imported integer default 0,
  error_message text,
  unique(quarter, version, import_type)
);
create index if not exists idx_kbv_import_history_quarter on kbv_import_history(quarter);
create index if not exists idx_kbv_import_history_status on kbv_import_history(status);
create index if not exists idx_kbv_import_history_started_at on kbv_import_history(started_at desc);
