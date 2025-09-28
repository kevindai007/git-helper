create table public.mr_info
(
    id         bigint generated always as identity primary key,
    project_id int8         not null,
    mr_id      int8         not null,
    mr_title   varchar(512) not null,
    state     varchar(64)  not null,
    target_branch varchar(256) not null,
    source_branch varchar(256) not null,
    sha       varchar(256) not null,
    web_url   varchar(512) not null,
    created_at timestamptz  not null default now(),
    updated_at timestamptz  not null default now()
);

create index idx_mr_info_project_id on public.mr_info (project_id);
create index idx_mr_info_mr_id on public.mr_info (mr_id);
create index idx_mr_info_status on public.mr_info (state);

alter table public.mr_info
    add constraint uq_project_mr_id unique (project_id, mr_id);