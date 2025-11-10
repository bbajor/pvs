create unique index if not exists uq_kbv_icd_entry_code_valid_from
  on kbv_icd_entry (code, valid_from);

