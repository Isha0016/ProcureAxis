-- F10 demo seed: stable IDs for repeatable test data

-- 1. Create parent records
INSERT INTO public."global-records" (sys_id, created_at, created_by)
VALUES
    ('10000000-0000-0000-0000-000000000001', CURRENT_TIMESTAMP, 'F10-SEED'),
    ('10000000-0000-0000-0000-000000000002', CURRENT_TIMESTAMP, 'F10-SEED'),
    ('10000000-0000-0000-0000-000000000003', CURRENT_TIMESTAMP, 'F10-SEED');

-- 2. Create companies
INSERT INTO public.companies
("sysId", company_code, company_name, legal_name, email_address,
 company_type, status)
VALUES
    ('10000000-0000-0000-0000-000000000001',
     'F10-BUYER', 'F10 Demo Buyer', 'F10 Demo Buyer',
     'buyer@example.test', 'PROCUREMENT_COMPANY', 'ACTIVE'),

    ('10000000-0000-0000-0000-000000000002',
     'F10-SUP-A', 'F10 Demo Supplier A', 'F10 Demo Supplier A',
     'supplier.a@example.test', 'SUPPLIER_COMPANY', 'ACTIVE'),

    ('10000000-0000-0000-0000-000000000003',
     'F10-SUP-B', 'F10 Demo Supplier B', 'F10 Demo Supplier B',
     'supplier.b@example.test', 'SUPPLIER_COMPANY', 'ACTIVE');

-- 3. Create buyer subtype
INSERT INTO public."procurement-companies"
("sysId", procurement_code, fiscal_year_start_month, approval_required)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'F10-BUYER', 4, TRUE);

-- 4. Create supplier subtypes
INSERT INTO public."supplier-companies"
("sysId", supplier_code, supplier_category)
VALUES
    ('10000000-0000-0000-0000-000000000002', 'F10-SUP-A', 'MANUFACTURER'),
    ('10000000-0000-0000-0000-000000000003', 'F10-SUP-B', 'MANUFACTURER');
-- 5. Seed notification templates required by Common

INSERT INTO public.notifications_templates
(sys_id, template_name, subject_line, email_template)
VALUES
    (
        'cad4f6cb-0ca0-439d-ac99-536d9b33523f',
        'COMPANY_REGISTRATION',
        'Company Registration',
        'Your company registration has been received successfully.'
    ),
    (
        '5bbfa963-3e82-4216-848b-ad0dd14044ee',
        'INVITE_EMPLOYEE',
        'Employee Invitation',
        'You have been invited to join the company. Please follow the invitation instructions.'
    );
