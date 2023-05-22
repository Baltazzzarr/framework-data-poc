create schema if not exists eav;
set search_path to eav;

create table entity (
    id      bigint generated always as identity primary key,
    name    varchar
);

create table attribute (
    id      bigint generated always as identity primary key,
    code    varchar unique not null
);

create table value (
    id              bigint generated always as identity primary key,
    attribute_id    bigint references attribute,
    entity_id       bigint references entity,
    value           varchar
);

create table dictionary (
    id      bigint generated always as identity primary key,
    value   varchar
);