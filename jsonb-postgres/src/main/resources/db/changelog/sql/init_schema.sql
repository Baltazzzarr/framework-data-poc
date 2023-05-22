create schema if not exists jsonb;
set search_path to jsonb;

create table entity (
    id      bigint generated always as identity primary key,
    name    varchar,
    properties jsonb
);

create table attribute (
    id      bigint generated always as identity primary key,
    code    varchar
);

create table dictionary (
    id      bigint generated always as identity primary key,
    value   varchar
);