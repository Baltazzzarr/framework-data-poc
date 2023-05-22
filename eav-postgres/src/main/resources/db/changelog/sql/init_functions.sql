create or replace function random_between(low int ,high int)
    returns int as
$$
begin
    return floor(random()* (high-low + 1) + low);
end ;
$$ language plpgsql;

create or replace procedure generate_data(size int) as
$$
declare
    attr attribute%rowtype;
    entity_id bigint;
    values text[] := (select array(select value from dictionary));
begin
    for i in 1..size
        loop
            insert into entity (name) values (md5(random()::text)::text) returning id into entity_id;
            for attr in select * from attribute
                loop
                    insert into value(attribute_id, entity_id, value) values (attr.id, entity_id, values[random_between(1, cardinality(values))]);
                end loop;
        end loop;
end;
$$ language plpgsql;