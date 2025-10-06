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

-- Ensure uniqueness of MR per project/mr/sha
create unique index if not exists uniq_mr_info_project_mr_sha
    on public.mr_info (project_id, mr_id, sha);

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
alter table public.mr_info drop column if exists analysis_result;

-- Migration: drop suggested diff column (if present)
alter table public.mr_analysis_detail
    drop column if exists remediation_diff;

-- Migration: add status to mark whether a suggestion is adopted (0=not adopted, 1=adopted)
alter table public.mr_analysis_detail
    add column if not exists status integer default 0 not null;
alter table mr_analysis_detail add column anchor_id varchar(256);
alter table mr_analysis_detail add column anchor_side varchar(16);
alter table mr_info add column if not exists summary_markdown text default '';

-- Token management table: default token and per-group tokens (group can be any depth path)
create table if not exists public.git_token
(
    id          bigint generated always as identity primary key,
    group_path  varchar(512), -- null means default token
    token       varchar(512)                           not null,
    is_default  boolean                                not null default false,
    created_at  timestamp with time zone default now() not null,
    updated_at  timestamp with time zone default now() not null
);

create index if not exists idx_git_token_group on public.git_token (group_path);
