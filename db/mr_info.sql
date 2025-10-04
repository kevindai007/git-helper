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

-- New table to store structured LLM analysis findings per MR
create table public.mr_analysis_detail
(
    id                 bigint generated always as identity primary key,
    mr_info_id         bigint                                 not null,
    project_id         bigint                                 not null,
    mr_id              bigint                                  not null,
    severity           varchar(16),
    category           varchar(32),
    title              varchar(512),
    description        text,
    file               varchar(512),
    line_type          varchar(8),
    start_line         integer,
    end_line           integer,
    start_col          integer,
    end_col            integer,
    evidence           text,
    remediation_steps  text,
    remediation_diff   text,
    confidence         double precision,
    tags_json          text,
    created_at         timestamp with time zone default now() not null,
    updated_at         timestamp with time zone default now() not null,
    constraint fk_mr_analysis_detail_mr_info_id foreign key (mr_info_id) references public.mr_info (id) on delete cascade
);

create index idx_mr_analysis_detail_mr_info_id on public.mr_analysis_detail (mr_info_id);
create index idx_mr_analysis_detail_project_mr on public.mr_analysis_detail (project_id, mr_id);

-- Migration: drop unused location range/column fields (if present)
alter table public.mr_analysis_detail
    drop column if exists end_line,
    drop column if exists start_col,
    drop column if exists end_col;
alter table mr_analysis_detail add column anchor_id varchar(256);
alter table mr_analysis_detail add column anchor_side varchar(16);
