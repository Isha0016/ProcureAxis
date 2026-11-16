-- F10 repeatable demo reference data.
-- Insert categories only when their category codes do not exist.

DO $$
DECLARE
category_record RECORD;
    new_sys_id UUID;
BEGIN
FOR category_record IN
SELECT *
FROM (VALUES
          ('F10-RAW', 'Raw Materials',
           'Demo category for raw materials', 1),
          ('F10-COMP', 'Components',
           'Demo category for components', 2),
          ('F10-FG', 'Finished Goods',
           'Demo category for finished goods', 3)
     ) AS categories(code, name, description, display_order)
    LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM product_categories
            WHERE category_code = category_record.code
        ) THEN
            new_sys_id := gen_random_uuid();

INSERT INTO "global-records" (
    sys_id, created_at, updated_at,
    created_by, updated_by
)
VALUES (
           new_sys_id, NOW(), NOW(),
           'F10-DEMO-SEED', 'F10-DEMO-SEED'
       );

INSERT INTO product_categories (
    "sysId",
    category_code,
    category_name,
    description,
    active,
    display_order
)
VALUES (
           new_sys_id,
           category_record.code,
           category_record.name,
           category_record.description,
           TRUE,
           category_record.display_order
       );
END IF;
END LOOP;
END $$;