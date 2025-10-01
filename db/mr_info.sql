create table public.mr_info
(
    id              bigint generated always as identity
        primary key,
    project_id      bigint                                 not null,
    mr_id           bigint                                 not null,
    mr_title        varchar(512)                           not null,
    state           varchar(64)                            not null,
    target_branch   varchar(256)                           not null,
    source_branch   varchar(256)                           not null,
    sha             varchar(256)                           not null,
    web_url         varchar(512)                           not null,
    created_at      timestamp with time zone default now() not null,
    updated_at      timestamp with time zone default now() not null,
    analysis_result text
);

alter table public.mr_info
    owner to postgres;

create index idx_mr_info_project_id
    on public.mr_info (project_id);

create index idx_mr_info_mr_id
    on public.mr_info (mr_id);

create index idx_mr_info_status
    on public.mr_info (state);

