CREATE TABLE patient(
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  owner_id UUID NOT NULL,
  first_name VARCHAR,
  last_name VARCHAR,
  email VARCHAR NOT NULL UNIQUE,
  description TEXT
);
