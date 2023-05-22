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
    attr_code text;
    dict_values text[] := (select array(select value from dictionary));
    properties jsonb := '{}'::jsonb;
begin
    for i in 1..size
        loop
            for attr_code in select code from attribute
                loop
                    properties := properties || format('{"%s":"%s"}',
                        attr_code, dict_values[random_between(1, cardinality(dict_values))])::jsonb;
                end loop;
            insert into entity(name, properties) values (md5(random()::text)::text, properties);
        end loop;
end;
$$ language plpgsql;