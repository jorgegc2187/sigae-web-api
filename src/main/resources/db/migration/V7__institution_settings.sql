create table institution_settings (
    id bigint primary key,
    system_name varchar(255) not null,
    address varchar(255),
    city varchar(120),
    support_phone varchar(60),
    support_email varchar(255) not null,
    logo_file_name varchar(255),
    logo_mime_type varchar(120),
    logo_content bytea,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now()
);

insert into institution_settings (
    id,
    system_name,
    address,
    city,
    support_phone,
    support_email,
    created_at,
    updated_at
) values (
    1,
    'SIGAE',
    null,
    null,
    null,
    'contacto@institucion.edu.pe',
    now(),
    now()
);
