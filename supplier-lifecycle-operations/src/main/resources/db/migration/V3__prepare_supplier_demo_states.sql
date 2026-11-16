-- Prepare existing F10 suppliers for the approval demo.
-- Preserve existing company UUIDs and other company data.

DO $$
DECLARE
supplier_count INTEGER;
BEGIN
SELECT COUNT(*)
INTO supplier_count
FROM companies
WHERE company_code IN ('F10-SUP-A', 'F10-SUP-B')
  AND company_type = 'SUPPLIER_COMPANY';

IF supplier_count <> 2 THEN
        RAISE EXCEPTION
            'Expected exactly 2 F10 supplier companies, found %',
            supplier_count;
END IF;
END $$;

UPDATE companies
SET status = 'PENDING',
    rejection_reason = NULL
WHERE company_code IN ('F10-SUP-A', 'F10-SUP-B')
  AND company_type = 'SUPPLIER_COMPANY';