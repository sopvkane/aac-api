create table if not exists phrases (
  id uuid primary key,
  text varchar(280) not null,
  created_at timestamptz not null default now()
);
