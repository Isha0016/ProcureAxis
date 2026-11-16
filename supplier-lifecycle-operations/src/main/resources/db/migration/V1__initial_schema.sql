-- =========================================================
-- FSC PROJECT
-- FLYWAY V1 INITIAL SCHEMA
-- Reconstructed from fsc_schema_before PostgreSQL export
-- =========================================================


-- =========================================================
-- SECTION 1: CREATE TABLES (30 TABLES)
-- =========================================================

CREATE TABLE public.bid_lines (
                                  currency_code character varying(3) NOT NULL,
                                  item_description_snapshot character varying(2000) NOT NULL,
                                  line_amount numeric(19,4) NOT NULL,
                                  quoted_quantity numeric(19,4) NOT NULL,
                                  quoted_unit_price numeric(19,4) NOT NULL,
                                  remarks character varying(2000),
                                  "sysId" uuid NOT NULL,
                                  bid_sys_id uuid NOT NULL,
                                  sourcing_request_line_sys_id uuid NOT NULL
);

CREATE TABLE public.bids (
                             bid_number character varying(50) NOT NULL,
                             bid_status character varying(30) NOT NULL,
                             commercial_proposal text,
                             currency_code character varying(3) NOT NULL,
                             delivery_lead_time_days integer,
                             delivery_terms character varying(1000),
                             evaluated_at timestamp(6) with time zone,
                             evaluation_score numeric(7,4),
                             payment_terms character varying(1000),
                             remarks character varying(2000),
                             submission_date timestamp(6) with time zone,
                             technical_proposal text,
                             total_bid_amount numeric(19,4),
                             valid_until date,
                             "sysId" uuid NOT NULL,
                             "sourcingRequest_sysId" uuid,
                             "supplierCompany_sysId" uuid,
                             version bigint,
                             sourcing_request_sys_id uuid NOT NULL,
                             supplier_company_sys_id uuid NOT NULL,
                             CONSTRAINT bids_bid_status_check CHECK (((bid_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'UNDER_EVALUATION'::character varying, 'CLARIFICATION_REQUIRED'::character varying, 'ACCEPTED'::character varying, 'REJECTED'::character varying, 'WITHDRAWN'::character varying, 'EXPIRED'::character varying])::text[])))
);

CREATE TABLE public.companies (
                                  ai_feedback text,
                                  company_code character varying(255),
                                  company_name character varying(255),
                                  contact_number character varying(255),
                                  email_address character varying(255),
                                  gst_number character varying(255),
                                  legal_name character varying(255),
                                  pan_number character varying(255),
                                  status character varying(255),
                                  website character varying(255),
                                  "sysId" uuid NOT NULL,
                                  rejection_reason text,
                                  company_type character varying(255),
                                  CONSTRAINT companies_company_type_check CHECK (((company_type)::text = ANY ((ARRAY['INTERNAL'::character varying, 'PROCUREMENT_COMPANY'::character varying, 'SUPPLIER_COMPANY'::character varying])::text[]))),
    CONSTRAINT companies_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'UNDER_REVIEW'::character varying, 'ACTIVE'::character varying, 'SUSPENDED'::character varying, 'INACTIVE'::character varying, 'REJECTED'::character varying])::text[])))
);

CREATE TABLE public.employees (
                                  date_of_leaving date,
                                  department character varying(255),
                                  designation character varying(255),
                                  employee_code character varying(255) NOT NULL,
                                  employment_status character varying(30) NOT NULL,
                                  employment_type character varying(30),
                                  joining_date date,
                                  office_location character varying(255),
                                  probation_end_date date,
                                  work_email character varying(255),
                                  work_phone character varying(255),
                                  "sysId" uuid NOT NULL,
                                  "company_sysId" uuid,
                                  "manager_sysId" uuid,
                                  CONSTRAINT employees_employment_status_check CHECK (((employment_status)::text = ANY ((ARRAY['ACTIVE'::character varying, 'ON_LEAVE'::character varying, 'NOTICE_PERIOD'::character varying, 'RESIGNED'::character varying, 'TERMINATED'::character varying])::text[]))),
    CONSTRAINT employees_employment_type_check CHECK (((employment_type)::text = ANY ((ARRAY['FULL_TIME'::character varying, 'PART_TIME'::character varying, 'CONTRACT'::character varying, 'INTERN'::character varying, 'TEMPORARY'::character varying, 'CONSULTANT'::character varying])::text[])))
);

CREATE TABLE public."global-records" (
                                         sys_id uuid NOT NULL,
                                         created_at timestamp(6) without time zone,
                                         created_by character varying(255),
                                         updated_at timestamp(6) without time zone,
                                         updated_by character varying(255)
);

CREATE TABLE public.notification_contexts (
                                              context_id character varying(255),
                                              email_map bytea,
                                              sms_map bytea,
                                              subject_line_map bytea,
                                              whats_app_map bytea,
                                              "sysId" uuid NOT NULL
);

CREATE TABLE public.notifications (
                                      failure_message text,
                                      is_email boolean NOT NULL,
                                      is_email_delivered boolean NOT NULL,
                                      is_email_failed boolean NOT NULL,
                                      issms boolean NOT NULL,
                                      issmsdelivered boolean NOT NULL,
                                      issmsfailed boolean NOT NULL,
                                      is_whats_app boolean NOT NULL,
                                      is_whats_app_delivered boolean NOT NULL,
                                      is_whats_app_failed boolean NOT NULL,
                                      notification_id character varying(255) NOT NULL,
                                      notification_priority character varying(255),
                                      notification_status character varying(255),
                                      "sysId" uuid NOT NULL,
                                      "notificationContext_sysId" uuid,
                                      notification_template_sys_id uuid,
                                      CONSTRAINT notifications_notification_priority_check CHECK (((notification_priority)::text = ANY ((ARRAY['URGENT'::character varying, 'NORMAL'::character varying])::text[]))),
    CONSTRAINT notifications_notification_status_check CHECK (((notification_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'IN_QUEUE'::character varying, 'DELIVERED'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE public.notifications_bcc_users (
                                                "Notification_sysId" uuid NOT NULL,
                                                "bccUsers_sysId" uuid NOT NULL
);

CREATE TABLE public.notifications_cc_users (
                                               "Notification_sysId" uuid NOT NULL,
                                               "ccUsers_sysId" uuid NOT NULL
);

CREATE TABLE public.notifications_templates (
                                                sys_id uuid NOT NULL,
                                                email_template text,
                                                sms_template text,
                                                subject_line character varying(255) NOT NULL,
                                                template_name character varying(255) NOT NULL,
                                                whats_app_message_template text
);

CREATE TABLE public.notifications_to_users (
                                               "Notification_sysId" uuid NOT NULL,
                                               "toUsers_sysId" uuid NOT NULL
);

CREATE TABLE public.operations (
                                   sys_id uuid NOT NULL,
                                   created_at timestamp(6) without time zone,
                                   created_by character varying(255),
                                   operation_category character varying(255),
                                   operation_id character varying(255) NOT NULL,
                                   operation_name character varying(255) NOT NULL,
                                   updated_at timestamp(6) without time zone,
                                   updated_by character varying(255)
);

CREATE TABLE public."procurement-companies" (
                                                approval_required boolean,
                                                fiscal_year_start_month integer,
                                                procurement_code character varying(255),
                                                "sysId" uuid NOT NULL
);

CREATE TABLE public.product_categories (
                                           active boolean NOT NULL,
                                           category_code character varying(50) NOT NULL,
                                           category_name character varying(150) NOT NULL,
                                           description character varying(1000),
                                           display_order integer,
                                           image_url character varying(255),
                                           "sysId" uuid NOT NULL,
                                           "parentCategory_sysId" uuid
);

CREATE TABLE public.products (
                                 barcode character varying(100),
                                 brand character varying(255),
                                 description character varying(2000),
                                 dimension_unit character varying(255),
                                 height numeric(12,3),
                                 image_url character varying(255),
                                 length numeric(12,3),
                                 manufacturer character varying(255),
                                 model_number character varying(255),
                                 product_code character varying(50) NOT NULL,
                                 product_name character varying(200) NOT NULL,
                                 product_type character varying(30) NOT NULL,
                                 sku character varying(100),
                                 status character varying(30) NOT NULL,
                                 unit_of_measure character varying(30) NOT NULL,
                                 weight numeric(12,3),
                                 weight_unit character varying(255),
                                 width numeric(12,3),
                                 "sysId" uuid NOT NULL,
                                 "category_sysId" uuid,
                                 "supplierCompany_sysId" uuid,
                                 CONSTRAINT products_product_type_check CHECK (((product_type)::text = ANY ((ARRAY['PHYSICAL'::character varying, 'DIGITAL'::character varying, 'SERVICE'::character varying, 'RAW_MATERIAL'::character varying, 'COMPONENT'::character varying, 'FINISHED_GOOD'::character varying, 'CONSUMABLE'::character varying])::text[]))),
    CONSTRAINT products_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'ACTIVE'::character varying, 'INACTIVE'::character varying, 'DISCONTINUED'::character varying])::text[]))),
    CONSTRAINT products_unit_of_measure_check CHECK (((unit_of_measure)::text = ANY ((ARRAY['PIECE'::character varying, 'BOX'::character varying, 'PACK'::character varying, 'SET'::character varying, 'KILOGRAM'::character varying, 'GRAM'::character varying, 'TONNE'::character varying, 'LITRE'::character varying, 'MILLILITRE'::character varying, 'METRE'::character varying, 'CENTIMETRE'::character varying, 'SQUARE_METRE'::character varying, 'CUBIC_METRE'::character varying])::text[])))
);

CREATE TABLE public."purchase-request-lines" (
                                                 currency_code character varying(3),
                                                 delivery_location character varying(500),
                                                 estimated_line_amount numeric(19,4),
                                                 estimated_unit_price numeric(19,4),
                                                 required_date date,
                                                 tax_rate numeric(7,4),
                                                 "sysId" uuid NOT NULL,
                                                 "purchaseRequest_sysId" uuid
);

CREATE TABLE public.purchase_request_decisions (
                                                   decision_at timestamp(6) without time zone NOT NULL,
                                                   decision_type character varying(40) NOT NULL,
                                                   from_status character varying(30),
                                                   reason character varying(1000),
                                                   to_status character varying(30) NOT NULL,
                                                   "sysId" uuid NOT NULL,
                                                   actor_employee_sys_id uuid NOT NULL,
                                                   purchase_request_sys_id uuid NOT NULL,
                                                   CONSTRAINT purchase_request_decisions_decision_type_check CHECK (((decision_type)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'PENDING_APPROVAL_ASSIGNED'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'REVISED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT purchase_request_decisions_from_status_check CHECK (((from_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT purchase_request_decisions_to_status_check CHECK (((to_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE TABLE public.purchase_requests (
                                          budget_code character varying(255),
                                          currency_code character varying(3),
                                          delivery_location character varying(255),
                                          estimated_total_amount numeric(19,4),
                                          procurement_type character varying(30),
                                          purchase_request_type character varying(30),
                                          "sysId" uuid NOT NULL,
                                          "procurementApprovalManager_sysId" uuid,
                                          "submitterEmployee_sysId" uuid,
                                          "supplierManagerApproval_sysId" uuid,
                                          "supplierReviewerApprovalEmployee_sysId" uuid,
                                          CONSTRAINT purchase_requests_procurement_type_check CHECK (((procurement_type)::text = ANY ((ARRAY['DIRECT_PURCHASE'::character varying, 'COMPETITIVE_SOURCING'::character varying])::text[]))),
    CONSTRAINT purchase_requests_purchase_request_type_check CHECK (((purchase_request_type)::text = ANY ((ARRAY['GOODS'::character varying, 'SERVICES'::character varying, 'GOODS_AND_SERVICES'::character varying])::text[])))
);

CREATE TABLE public.request_lines (
                                      item_description character varying(2000) NOT NULL,
                                      line_number integer NOT NULL,
                                      line_status character varying(30) NOT NULL,
                                      notes character varying(1000),
                                      quantity numeric(19,4) NOT NULL,
                                      unit_of_measure character varying(30) NOT NULL,
                                      "sysId" uuid NOT NULL,
                                      "product_sysId" uuid,
                                      CONSTRAINT request_lines_line_status_check CHECK (((line_status)::text = ANY ((ARRAY['OPEN'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'PARTIALLY_FULFILLED'::character varying, 'FULFILLED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT request_lines_unit_of_measure_check CHECK (((unit_of_measure)::text = ANY ((ARRAY['PIECE'::character varying, 'BOX'::character varying, 'PACK'::character varying, 'SET'::character varying, 'KILOGRAM'::character varying, 'GRAM'::character varying, 'TONNE'::character varying, 'LITRE'::character varying, 'MILLILITRE'::character varying, 'METRE'::character varying, 'CENTIMETRE'::character varying, 'SQUARE_METRE'::character varying, 'CUBIC_METRE'::character varying])::text[])))
);

CREATE TABLE public.requests (
                                 description character varying(2000),
                                 priority character varying(20) NOT NULL,
                                 rejection_reason character varying(1000),
                                 request_number character varying(255) NOT NULL,
                                 request_status character varying(30) NOT NULL,
                                 required_date date,
                                 submission_date timestamp(6) with time zone,
                                 title character varying(200) NOT NULL,
                                 "sysId" uuid NOT NULL,
                                 "company_sysId" uuid,
                                 CONSTRAINT requests_priority_check CHECK (((priority)::text = ANY ((ARRAY['LOW'::character varying, 'MEDIUM'::character varying, 'HIGH'::character varying, 'URGENT'::character varying])::text[]))),
    CONSTRAINT requests_request_status_check CHECK (((request_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE TABLE public.roles (
                              role_id character varying(255),
                              role_name character varying(255),
                              "sysId" uuid NOT NULL
);

CREATE TABLE public.roles_operations (
                                         "Role_sysId" uuid NOT NULL,
                                         operations_sys_id uuid NOT NULL
);

CREATE TABLE public.sourcing_awards (
                                        awarded_at timestamp(6) with time zone NOT NULL,
                                        reason character varying(2000) NOT NULL,
                                        "sysId" uuid NOT NULL,
                                        accepted_bid_sys_id uuid NOT NULL,
                                        awarded_by_employee_sys_id uuid NOT NULL,
                                        sourcing_request_sys_id uuid NOT NULL
);

CREATE TABLE public.sourcing_request_decisions (
                                                   decision_at timestamp(6) without time zone NOT NULL,
                                                   decision_type character varying(40) NOT NULL,
                                                   from_status character varying(30),
                                                   reason character varying(1000),
                                                   to_status character varying(30) NOT NULL,
                                                   "sysId" uuid NOT NULL,
                                                   actor_employee_sys_id uuid NOT NULL,
                                                   sourcing_request_sys_id uuid NOT NULL,
                                                   CONSTRAINT sourcing_request_decisions_decision_type_check CHECK (((decision_type)::text = ANY ((ARRAY['SUBMITTED'::character varying, 'PENDING_APPROVAL_ASSIGNED'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'REVISED'::character varying, 'CANCELLED'::character varying, 'PUBLISHED'::character varying, 'CLOSED'::character varying])::text[]))),
    CONSTRAINT sourcing_request_decisions_from_status_check CHECK (((from_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[]))),
    CONSTRAINT sourcing_request_decisions_to_status_check CHECK (((to_status)::text = ANY ((ARRAY['DRAFT'::character varying, 'SUBMITTED'::character varying, 'PENDING_APPROVAL'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'IN_PROGRESS'::character varying, 'COMPLETED'::character varying, 'CANCELLED'::character varying])::text[])))
);

CREATE TABLE public.sourcing_request_lines (
                                               currency_code character varying(3),
                                               delivery_location character varying(500),
                                               estimated_line_amount numeric(19,4),
                                               estimated_unit_price numeric(19,4),
                                               item_description character varying(2000) NOT NULL,
                                               line_number integer NOT NULL,
                                               notes character varying(1000),
                                               product_code_snapshot character varying(50),
                                               quantity numeric(19,4) NOT NULL,
                                               required_date date,
                                               source_product_sys_id uuid,
                                               tax_rate numeric(7,4),
                                               unit_of_measure character varying(30) NOT NULL,
                                               "sysId" uuid NOT NULL,
                                               source_purchase_request_line_sys_id uuid CONSTRAINT sourcing_request_lines_source_purchase_request_line_sy_not_null NOT NULL,
                                               sourcing_request_sys_id uuid NOT NULL,
                                               CONSTRAINT sourcing_request_lines_unit_of_measure_check CHECK (((unit_of_measure)::text = ANY ((ARRAY['PIECE'::character varying, 'BOX'::character varying, 'PACK'::character varying, 'SET'::character varying, 'KILOGRAM'::character varying, 'GRAM'::character varying, 'TONNE'::character varying, 'LITRE'::character varying, 'MILLILITRE'::character varying, 'METRE'::character varying, 'CENTIMETRE'::character varying, 'SQUARE_METRE'::character varying, 'CUBIC_METRE'::character varying])::text[])))
);

CREATE TABLE public.sourcing_requests (
                                          commercial_terms character varying(2000),
                                          currency_code character varying(3),
                                          evaluation_criteria character varying(2000),
                                          evaluation_deadline timestamp(6) with time zone,
                                          response_deadline timestamp(6) with time zone,
                                          sourcing_method character varying(30),
                                          target_supplier_count integer,
                                          "sysId" uuid NOT NULL,
                                          "procurementApprovalManager_sysId" uuid,
                                          "submitterEmployee_sysId" uuid,
                                          budget_code character varying(255),
                                          closed_at timestamp(6) with time zone,
                                          closure_outcome character varying(30),
                                          closure_reason character varying(1000),
                                          delivery_location character varying(500),
                                          estimated_total_amount numeric(19,4),
                                          procurement_type character varying(30),
                                          published_at timestamp(6) with time zone,
                                          version bigint,
                                          source_purchase_request_sys_id uuid,
                                          CONSTRAINT sourcing_requests_closure_outcome_check CHECK (((closure_outcome)::text = ANY ((ARRAY['AWARDED'::character varying, 'NO_AWARD'::character varying])::text[]))),
    CONSTRAINT sourcing_requests_procurement_type_check CHECK (((procurement_type)::text = ANY ((ARRAY['DIRECT_PURCHASE'::character varying, 'COMPETITIVE_SOURCING'::character varying])::text[]))),
    CONSTRAINT sourcing_requests_sourcing_method_check CHECK (((sourcing_method)::text = ANY ((ARRAY['RFI'::character varying, 'RFQ'::character varying, 'RFP'::character varying, 'TENDER'::character varying, 'AUCTION'::character varying])::text[])))
);

CREATE TABLE public.sourcing_supplier_invitations (
                                                      invited_at timestamp(6) with time zone,
                                                      status character varying(30) NOT NULL,
                                                      "sysId" uuid NOT NULL,
                                                      sourcing_request_sys_id uuid NOT NULL,
                                                      supplier_company_sys_id uuid NOT NULL,
                                                      CONSTRAINT sourcing_supplier_invitations_status_check CHECK (((status)::text = ANY ((ARRAY['INVITED'::character varying, 'ACCEPTED'::character varying, 'DECLINED'::character varying, 'REMOVED'::character varying])::text[])))
);

CREATE TABLE public."supplier-companies" (
                                             lead_time_days integer,
                                             minimum_order_value numeric(38,2),
                                             supplier_category character varying(255),
                                             supplier_code character varying(255),
                                             "sysId" uuid NOT NULL
);

CREATE TABLE public.users (
                              email_address character varying(254) NOT NULL,
                              email_verified boolean NOT NULL,
                              first_name character varying(100) NOT NULL,
                              last_login_at timestamp(6) with time zone,
                              last_name character varying(100) NOT NULL,
                              password_hash character varying(255) NOT NULL,
                              phone_number character varying(30),
                              phone_verified boolean NOT NULL,
                              user_status character varying(30) NOT NULL,
                              "sysId" uuid NOT NULL,
                              CONSTRAINT users_user_status_check CHECK (((user_status)::text = ANY ((ARRAY['PENDING_VERIFICATION'::character varying, 'ACTIVE'::character varying, 'LOCKED'::character varying, 'SUSPENDED'::character varying, 'DEACTIVATED'::character varying])::text[])))
);

CREATE TABLE public.users_roles (
                                    "User_sysId" uuid NOT NULL,
                                    "roles_sysId" uuid NOT NULL
);


-- =========================================================
-- SECTION 2: PRIMARY KEY CONSTRAINTS (25)
-- =========================================================

ALTER TABLE ONLY public.bid_lines
    ADD CONSTRAINT bid_lines_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.bids
    ADD CONSTRAINT bids_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT companies_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT employees_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public."global-records"
    ADD CONSTRAINT "global-records_pkey" PRIMARY KEY (sys_id);

ALTER TABLE ONLY public.notification_contexts
    ADD CONSTRAINT notification_contexts_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.notifications_templates
    ADD CONSTRAINT notifications_templates_pkey PRIMARY KEY (sys_id);

ALTER TABLE ONLY public.operations
    ADD CONSTRAINT operations_pkey PRIMARY KEY (sys_id);

ALTER TABLE ONLY public."procurement-companies"
    ADD CONSTRAINT "procurement-companies_pkey" PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.product_categories
    ADD CONSTRAINT product_categories_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.products
    ADD CONSTRAINT products_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public."purchase-request-lines"
    ADD CONSTRAINT "purchase-request-lines_pkey" PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.purchase_request_decisions
    ADD CONSTRAINT purchase_request_decisions_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.purchase_requests
    ADD CONSTRAINT purchase_requests_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.request_lines
    ADD CONSTRAINT request_lines_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT requests_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT roles_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.sourcing_awards
    ADD CONSTRAINT sourcing_awards_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.sourcing_request_decisions
    ADD CONSTRAINT sourcing_request_decisions_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.sourcing_request_lines
    ADD CONSTRAINT sourcing_request_lines_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.sourcing_requests
    ADD CONSTRAINT sourcing_requests_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.sourcing_supplier_invitations
    ADD CONSTRAINT sourcing_supplier_invitations_pkey PRIMARY KEY ("sysId");

ALTER TABLE ONLY public."supplier-companies"
    ADD CONSTRAINT "supplier-companies_pkey" PRIMARY KEY ("sysId");

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY ("sysId");


-- =========================================================
-- SECTION 3: UNIQUE CONSTRAINTS (14)
-- =========================================================

ALTER TABLE ONLY public.users
    ADD CONSTRAINT uk1ar956vx8jufbghpyi09yr16l UNIQUE (email_address);

ALTER TABLE ONLY public.notifications_templates
    ADD CONSTRAINT uk2yuu3c96eg04t88e6sjmlsihp UNIQUE (template_name);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT uk453lw3hykb6ku6o3an402ro5n UNIQUE ("notificationContext_sysId");

ALTER TABLE ONLY public.operations
    ADD CONSTRAINT uk48v99nxef5rmlrblia2g7eyq0 UNIQUE (operation_name);

ALTER TABLE ONLY public.bids
    ADD CONSTRAINT uk_bid_number UNIQUE (bid_number);

ALTER TABLE ONLY public.bid_lines
    ADD CONSTRAINT uk_bid_sourcing_line UNIQUE (bid_sys_id, sourcing_request_line_sys_id);

ALTER TABLE ONLY public.sourcing_awards
    ADD CONSTRAINT uk_sourcing_award_request UNIQUE (sourcing_request_sys_id);

ALTER TABLE ONLY public.sourcing_supplier_invitations
    ADD CONSTRAINT uk_sourcing_request_supplier UNIQUE (sourcing_request_sys_id, supplier_company_sys_id);

ALTER TABLE ONLY public.bids
    ADD CONSTRAINT uk_sourcing_supplier_bid UNIQUE (sourcing_request_sys_id, supplier_company_sys_id);

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT uketqhw9qqnad1kyjq3ks1glw8x UNIQUE (employee_code);

ALTER TABLE ONLY public.sourcing_requests
    ADD CONSTRAINT ukg97x5igs7u33ji7lqnc7uwiha UNIQUE (source_purchase_request_sys_id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT uko9bji4s98gyiqmh1a5l04fuys UNIQUE (notification_id);

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT ukoonqrar8b975jo10i82bjg6gj UNIQUE (request_number);

ALTER TABLE ONLY public.operations
    ADD CONSTRAINT ukps17pi85keg4le2a9f6muwovx UNIQUE (operation_id);


-- =========================================================
-- SECTION 4: FOREIGN KEY CONSTRAINTS (66)
-- =========================================================

ALTER TABLE ONLY public.notification_contexts
    ADD CONSTRAINT fk10q5o9rpojkywemxu7yxq849c FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.sourcing_awards
    ADD CONSTRAINT fk1hba0uxk767do1cjpxarr8dm2 FOREIGN KEY (awarded_by_employee_sys_id) REFERENCES public.employees("sysId");

ALTER TABLE ONLY public.sourcing_awards
    ADD CONSTRAINT fk28nuhgp1w2xwbavbek3j0mm2l FOREIGN KEY (accepted_bid_sys_id) REFERENCES public.bids("sysId");

ALTER TABLE ONLY public.purchase_request_decisions
    ADD CONSTRAINT fk2k39sd2gkf18tgx9u5l2pr3wr FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public."purchase-request-lines"
    ADD CONSTRAINT fk30eonvswf8c10fp2dg44rpdls FOREIGN KEY ("sysId") REFERENCES public.request_lines("sysId");

ALTER TABLE ONLY public.product_categories
    ADD CONSTRAINT fk34phnbaqyon0tafsne3gb36l1 FOREIGN KEY ("parentCategory_sysId") REFERENCES public.product_categories("sysId");

ALTER TABLE ONLY public.users
    ADD CONSTRAINT fk3efo238n8qc815luo8o75rqi5 FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT fk3k27rxcnuuet5jiecgk57yv2b FOREIGN KEY ("company_sysId") REFERENCES public.companies("sysId");

ALTER TABLE ONLY public.notifications_bcc_users
    ADD CONSTRAINT fk3lpv7bahs5f6wbnan93vf78lo FOREIGN KEY ("Notification_sysId") REFERENCES public.notifications("sysId");

ALTER TABLE ONLY public.companies
    ADD CONSTRAINT fk625xmbtg7s5f9qcvumnkyx078 FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.notifications_cc_users
    ADD CONSTRAINT fk6caa9oxik55xpcmrqqpigmtlt FOREIGN KEY ("ccUsers_sysId") REFERENCES public.users("sysId");

ALTER TABLE ONLY public.sourcing_requests
    ADD CONSTRAINT fk6fapyqqqq3nqnhr5a3uoens02 FOREIGN KEY (source_purchase_request_sys_id) REFERENCES public.purchase_requests("sysId");

ALTER TABLE ONLY public.request_lines
    ADD CONSTRAINT fk6gt9ap1kvdl3dm4tba7jk3ukt FOREIGN KEY ("product_sysId") REFERENCES public.products("sysId");

ALTER TABLE ONLY public.roles
    ADD CONSTRAINT fk6hm69n3wqykbb8rxkw2yx6y6n FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fk6mapysko86gjuojato6574y6u FOREIGN KEY ("category_sysId") REFERENCES public.product_categories("sysId");

ALTER TABLE ONLY public."procurement-companies"
    ADD CONSTRAINT fk79r0j58qyrutoo9urmhoo382g FOREIGN KEY ("sysId") REFERENCES public.companies("sysId");

ALTER TABLE ONLY public.sourcing_awards
    ADD CONSTRAINT fk7bgkq4kr42fnbndt79jh80am5 FOREIGN KEY (sourcing_request_sys_id) REFERENCES public.sourcing_requests("sysId");

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT fk7dlootw2en2womtpbc7ag5yah FOREIGN KEY ("sysId") REFERENCES public.users("sysId");

ALTER TABLE ONLY public.roles_operations
    ADD CONSTRAINT fk8pk7uofwbws8kl1fd0fhuthec FOREIGN KEY (operations_sys_id) REFERENCES public.operations(sys_id);

ALTER TABLE ONLY public.notifications_cc_users
    ADD CONSTRAINT fkacv3baordd5odwpqwam4v7ydo FOREIGN KEY ("Notification_sysId") REFERENCES public.notifications("sysId");

ALTER TABLE ONLY public.purchase_requests
    ADD CONSTRAINT fkaf9nddhbipk3jhpllrk6vjjpm FOREIGN KEY ("submitterEmployee_sysId") REFERENCES public.employees("sysId");

ALTER TABLE ONLY public.sourcing_supplier_invitations
    ADD CONSTRAINT fkai1a8ynxdgotuo5rj6avk8mar FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT fkav7plh8rqwhy4onbjok7lhqdt FOREIGN KEY ("company_sysId") REFERENCES public.companies("sysId");

ALTER TABLE ONLY public.notifications_to_users
    ADD CONSTRAINT fkbjx367m33vqax1h97x8fkk81h FOREIGN KEY ("toUsers_sysId") REFERENCES public.users("sysId");

ALTER TABLE ONLY public.sourcing_request_lines
    ADD CONSTRAINT fkc5uklyfctjf73rjwt9i5psqur FOREIGN KEY (sourcing_request_sys_id) REFERENCES public.sourcing_requests("sysId");

ALTER TABLE ONLY public.sourcing_request_lines
    ADD CONSTRAINT fkce2mxvtqgnbm533u5wahdo7pi FOREIGN KEY (source_purchase_request_line_sys_id) REFERENCES public."purchase-request-lines"("sysId");

ALTER TABLE ONLY public.notifications_to_users
    ADD CONSTRAINT fkchyken8kmh01n8efw4pkklicm FOREIGN KEY ("Notification_sysId") REFERENCES public.notifications("sysId");

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fkcko7edtnbgbeecepj320ax06w FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.sourcing_supplier_invitations
    ADD CONSTRAINT fkcpys3nurur0j6g4iwm33itr8l FOREIGN KEY (supplier_company_sys_id) REFERENCES public."supplier-companies"("sysId");

ALTER TABLE ONLY public.sourcing_request_lines
    ADD CONSTRAINT fkcyhdsy31q7lpn8148lkqgttxv FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fkcyiim8ju17vyy3iqxjftgy7tx FOREIGN KEY (notification_template_sys_id) REFERENCES public.notifications_templates(sys_id);

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT fkdm4g6jc2if9nrvij3li9pow6i FOREIGN KEY ("notificationContext_sysId") REFERENCES public.notification_contexts("sysId");

ALTER TABLE ONLY public.sourcing_request_decisions
    ADD CONSTRAINT fkdpmlve3yqjpxwrdtruk5d3tfj FOREIGN KEY (actor_employee_sys_id) REFERENCES public.employees("sysId");

ALTER TABLE ONLY public.bids
    ADD CONSTRAINT fkdpto7vclidl7jq1suxewh3rs8 FOREIGN KEY ("supplierCompany_sysId") REFERENCES public."supplier-companies"("sysId");

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fkdr3ld053oqy4oendgmv6p5ybe FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.product_categories
    ADD CONSTRAINT fkekrxn5kig5lv5ybvrjei48tnh FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.bids
    ADD CONSTRAINT fkey23i1r5q3eqe9klc2o0tg9ay FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.users_roles
    ADD CONSTRAINT fkf6hy0cpc3bds7n86x5pxgypo1 FOREIGN KEY ("User_sysId") REFERENCES public.users("sysId");

ALTER TABLE ONLY public.request_lines
    ADD CONSTRAINT fkfshadxd4g1jgv1l5kxsemtj0t FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.purchase_requests
    ADD CONSTRAINT fkglcvqa1cmverngw4lxjbdi4gh FOREIGN KEY ("sysId") REFERENCES public.requests("sysId");

ALTER TABLE ONLY public.users_roles
    ADD CONSTRAINT fkh69wp0mp8n1b8he3as4fp7xww FOREIGN KEY ("roles_sysId") REFERENCES public.roles("sysId");

ALTER TABLE ONLY public.bid_lines
    ADD CONSTRAINT fki14jh8am4grrxunhk72a8rba0 FOREIGN KEY (sourcing_request_line_sys_id) REFERENCES public.sourcing_request_lines("sysId");

ALTER TABLE ONLY public.sourcing_supplier_invitations
    ADD CONSTRAINT fkiiat1rc4nnc3s07qus5y9wqtc FOREIGN KEY (sourcing_request_sys_id) REFERENCES public.sourcing_requests("sysId");

ALTER TABLE ONLY public.sourcing_request_decisions
    ADD CONSTRAINT fkim7tmtjg997m9tn9d3886suvc FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public."purchase-request-lines"
    ADD CONSTRAINT fkiol5yuymg9xfi1b3tk26y0xuh FOREIGN KEY ("purchaseRequest_sysId") REFERENCES public.purchase_requests("sysId");

ALTER TABLE ONLY public.sourcing_request_decisions
    ADD CONSTRAINT fkit7m8rx4dmyq9cacbs3povthi FOREIGN KEY (sourcing_request_sys_id) REFERENCES public.sourcing_requests("sysId");

ALTER TABLE ONLY public.purchase_requests
    ADD CONSTRAINT fkiubf6p2cr4cqtu2o2xtiimvop FOREIGN KEY ("supplierReviewerApprovalEmployee_sysId") REFERENCES public.employees("sysId");

ALTER TABLE ONLY public.sourcing_requests
    ADD CONSTRAINT fkjmm9xb2vfrs1ahkf39ukyh5au FOREIGN KEY ("procurementApprovalManager_sysId") REFERENCES public.employees("sysId");

ALTER TABLE ONLY public.sourcing_awards
    ADD CONSTRAINT fkjuckpl3m3l0uqfp47angavh9w FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.bids
    ADD CONSTRAINT fkk0vgpcolq77lwe13x3hjfuqdw FOREIGN KEY ("sourcingRequest_sysId") REFERENCES public.sourcing_requests("sysId");

ALTER TABLE ONLY public.purchase_requests
    ADD CONSTRAINT fkk29pnuh2tp0478k4dkplssp6g FOREIGN KEY ("supplierManagerApproval_sysId") REFERENCES public.employees("sysId");

ALTER TABLE ONLY public.bids
    ADD CONSTRAINT fkl7hqet3dkuphn5gbh1nydnfsb FOREIGN KEY (sourcing_request_sys_id) REFERENCES public.sourcing_requests("sysId");

ALTER TABLE ONLY public.roles_operations
    ADD CONSTRAINT fkld2lrog1g0x39b9uu20wrnibu FOREIGN KEY ("Role_sysId") REFERENCES public.roles("sysId");

ALTER TABLE ONLY public.sourcing_requests
    ADD CONSTRAINT fkm5dkqxn13fuyt7y95o66i4m2w FOREIGN KEY ("sysId") REFERENCES public.requests("sysId");

ALTER TABLE ONLY public.sourcing_requests
    ADD CONSTRAINT fkmn4t4g58fjjir6j8aqfgvgdhx FOREIGN KEY ("submitterEmployee_sysId") REFERENCES public.employees("sysId");

ALTER TABLE ONLY public.requests
    ADD CONSTRAINT fkmnde2d0xieli3m5ee7fsdquac FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.bid_lines
    ADD CONSTRAINT fkn05oavurc9asrw97o7tc4at8c FOREIGN KEY ("sysId") REFERENCES public."global-records"(sys_id);

ALTER TABLE ONLY public.bid_lines
    ADD CONSTRAINT fkn4ww02iu0agrpnmje6xkpeol4 FOREIGN KEY (bid_sys_id) REFERENCES public.bids("sysId");

ALTER TABLE ONLY public.purchase_request_decisions
    ADD CONSTRAINT fknugwwtwwassf0l2vop7xbyw6r FOREIGN KEY (purchase_request_sys_id) REFERENCES public.purchase_requests("sysId");

ALTER TABLE ONLY public.employees
    ADD CONSTRAINT fkobwg126ib17rxwh7jag7kw2dm FOREIGN KEY ("manager_sysId") REFERENCES public.employees("sysId");

ALTER TABLE ONLY public.purchase_requests
    ADD CONSTRAINT fkpenr1dmc61qkufvkmvneogi7t FOREIGN KEY ("procurementApprovalManager_sysId") REFERENCES public.employees("sysId");

ALTER TABLE ONLY public."supplier-companies"
    ADD CONSTRAINT fkpfxd9nid977bgevhn7w5eqbry FOREIGN KEY ("sysId") REFERENCES public.companies("sysId");

ALTER TABLE ONLY public.notifications_bcc_users
    ADD CONSTRAINT fkpo68vhvwtfylg2qi5s621x4uo FOREIGN KEY ("bccUsers_sysId") REFERENCES public.users("sysId");

ALTER TABLE ONLY public.purchase_request_decisions
    ADD CONSTRAINT fks4nsw33wybcsqq05sv5v9bmjn FOREIGN KEY (actor_employee_sys_id) REFERENCES public.employees("sysId");

ALTER TABLE ONLY public.products
    ADD CONSTRAINT fks95t3g6c373wkj6as6msge9y9 FOREIGN KEY ("supplierCompany_sysId") REFERENCES public."supplier-companies"("sysId");

ALTER TABLE ONLY public.bids
    ADD CONSTRAINT fksl1wqjpdukbf8fj3mhp0p74y0 FOREIGN KEY (supplier_company_sys_id) REFERENCES public."supplier-companies"("sysId");