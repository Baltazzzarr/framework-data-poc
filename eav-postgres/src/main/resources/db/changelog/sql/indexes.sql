create index if not exists value_attribute_id_idx on value(attribute_id);
create index if not exists value_entity_id_idx on value (entity_id);
create index if not exists attribute_code_idx on attribute(code);
create index if not exists value_attribute_id_value on value(attribute_id, value);
create index on value using btree(value);