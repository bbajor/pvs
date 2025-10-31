-- Initial KBV schema for historized master data (placeholder)
create table if not exists kbv_icd_entry (
  id serial primary key,
  code varchar(50) not null,
  text_content text not null,
  valid_from date not null,
  valid_to date
);
create index if not exists idx_kbv_icd_entry_code on kbv_icd_entry(code);
