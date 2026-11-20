
import {
    getSupplierCompany,
    startSupplierCompanyReview,
    approveSupplierCompany,
    rejectSupplierCompany,
    suspendSupplierCompany,
    deactivateSupplierCompany,
    reinstateSupplierCompany
} from '../api/supplierCompany.api.js';

import {
    getAllProcurementCompanies,
    updateProcurementCompany,
    approveProcurementCompany,
    rejectProcurementCompany,
    suspendProcurementCompany,
    reinstateProcurementCompany
} from '../api/procurementCompany.api.js';

import {
    inviteEmployee,
    assignEmployeeManager,
    activateEmployee
} from '../api/employee.api.js';

import { createRole } from '../api/roles.api.js';

import './internalAdmin.css';

import { logoutEmployee} from "../api/auth.api.js";

const app = document.querySelector('#app');

let currentSupplier = null;
let currentCompanyId = '';

let procurementCompanies = [];
let currentProcurementPage = 0;
let totalProcurementPages = 0;
let currentProcurementCompany = null;

// =====================================================
// ROLE PERMISSIONS
// Exact operation names from backend OperationService
// =====================================================

const ROLE_PERMISSION_GROUPS = {
    COMPANY: [
        'VIEW_COMPANY_PROFILE',
        'UPDATE_COMPANY_PROFILE',
        'CREATE_ROLE',
        'INVITE_EMPLOYEE'
    ],

    USER_MANAGEMENT: [
        'CREATE_COMPANY_EMPLOYEE',
        'VIEW_COMPANY_EMPLOYEE',
        'UPDATE_COMPANY_EMPLOYEE',
        'DEACTIVATE_COMPANY_EMPLOYEE'
    ],

    SUPPLIER_MANAGEMENT: [
        'CREATE_SUPPLIER',
        'VIEW_SUPPLIER',
        'UPDATE_SUPPLIER',
        'APPROVE_SUPPLIER'
    ],

    PROCUREMENT_MANAGEMENT: [
        'APPROVE_PROCUREMENT'
    ],

    CATALOG: [
        'CREATE_PRODUCT',
        'VIEW_PRODUCT',
        'UPDATE_PRODUCT',
        'DELETE_PRODUCT'
    ],

    PURCHASE_REQUEST: [
        'CREATE_PURCHASE_REQUEST',
        'VIEW_PURCHASE_REQUEST',
        'UPDATE_PURCHASE_REQUEST',
        'SUBMIT_PURCHASE_REQUEST',
        'APPROVE_PURCHASE_REQUEST',
        'REJECT_PURCHASE_REQUEST'
    ],

    SOURCING: [
        'CREATE_SOURCING_REQUEST',
        'VIEW_SOURCING_REQUEST',
        'UPDATE_SOURCING_REQUEST',
        'PUBLISH_SOURCING_REQUEST',
        'CLOSE_SOURCING_REQUEST'
    ],

    BID_MANAGEMENT: [
        'CREATE_BID',
        'VIEW_BID',
        'UPDATE_BID',
        'SUBMIT_BID',
        'COMPARE_BIDS',
        'AWARD_BID',
        'WITHDRAW_BID'
    ],

    PURCHASE_ORDER: [
        'CREATE_PURCHASE_ORDER',
        'VIEW_PURCHASE_ORDER',
        'UPDATE_PURCHASE_ORDER',
        'APPROVE_PURCHASE_ORDER',
        'CANCEL_PURCHASE_ORDER',
        'ACKNOWLEDGE_PURCHASE_ORDER'
    ],

    CONTRACT: [
        'CREATE_CONTRACT',
        'VIEW_CONTRACT',
        'APPROVE_CONTRACT'
    ],

    INVOICE: [
        'CREATE_INVOICE',
        'VIEW_INVOICE',
        'APPROVE_INVOICE'
    ],

    PAYMENT: [
        'VIEW_PAYMENT_HISTORY'
    ],

    REPORTING: [
        'VIEW_PROCUREMENT_REPORT',
        'EXPORT_PROCUREMENT_REPORT'
    ],

    AUDIT: [
        'VIEW_AUDIT_LOG'
    ]
};

// =====================================================
// COMMON HELPERS
// =====================================================

function escapeHtml(value) {
    return String(value ?? '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

function getErrorMessage(error) {
    return error.response?.data?.message ||
        error.response?.data?.error ||
        error.message ||
        'Something went wrong.';
}

function getStatusClass(status) {
    return String(status || 'UNKNOWN')
        .toLowerCase()
        .replace(/[^a-z0-9-]/g, '-');
}

function setActiveNavigation(activeId) {
    document.querySelectorAll('.admin-nav-item')
        .forEach(button => button.classList.remove('active'));

    document.querySelector(`#${activeId}`)
        ?.classList.add('active');
}

function showMessage(element, message, type = '') {
    if (!element) return;

    element.textContent = message;
    element.className = `admin-message ${type}`;
}

// =====================================================
// DASHBOARD LAYOUT
// =====================================================

function renderDashboardLayout() {
    currentSupplier = null;
    currentCompanyId = '';

    app.innerHTML = `
<div class="internal-admin-layout">

<aside class="internal-admin-sidebar">

<div class="admin-brand">
<div class="brand-logo">P</div>
<div>
<h2>ProcureAxis</h2>
<p>INTERNAL ADMIN</p>
</div>
</div>

<div class="admin-sidebar-heading">
ADMINISTRATION
</div>

<button class="admin-nav-item active"
id="supplierManagementNav">
<span>🏢</span>
Supplier Companies
</button>

<button class="admin-nav-item"
id="procurementManagementNav">
<span>🏭</span>
Procurement Companies
</button>

<button class="admin-nav-item"
id="employeeManagementNav">
<span>👥</span>
Employee Management
</button>

<button class="admin-nav-item"
id="roleManagementNav">
<span>🔐</span>
Role Management
</button>

<div class="admin-sidebar-bottom">
<button id="adminLogoutButton">
← Back to Portal Selection
</button>
</div>

</aside>

<main class="internal-admin-main">

<header class="admin-topbar">
<div>
<h1>Internal Administration</h1>
<p>Manage companies and platform access.</p>
</div>

${renderAdminProfile()}
</header>

<section class="admin-content" id="adminPageContent">
</section>

</main>

</div>
`;

    document.querySelector('#adminLogoutButton')
        .addEventListener('click', () => {
            logoutEmployee();
        });

    attachAdminProfileEvents();

    document.querySelector('#supplierManagementNav')
        .addEventListener('click', () => {
            setActiveNavigation('supplierManagementNav');
            renderSupplierPage();
        });

    document.querySelector('#procurementManagementNav')
        .addEventListener('click', () => {
            setActiveNavigation('procurementManagementNav');
            renderProcurementPage();
        });

    document.querySelector('#employeeManagementNav')
        .addEventListener('click', () => {
            setActiveNavigation('employeeManagementNav');
            renderEmployeePage();
        });

    document.querySelector('#roleManagementNav')
        .addEventListener('click', () => {
            setActiveNavigation('roleManagementNav');
            renderRolePage();
        });

    renderSupplierPage();
}

// =====================================================
// SUPPLIER COMPANY MANAGEMENT
// =====================================================

function renderSupplierDetails(company) {
    const ignoredFields = [
        'hibernateLazyInitializer',
        'handler'
    ];

    const fields = Object.entries(company)
        .filter(([key, value]) =>
            !ignoredFields.includes(key) &&
            value !== null &&
            value !== undefined &&
            typeof value !== 'object'
        );

    return fields.map(([key, value]) => `
<div class="admin-detail-item">
<span class="admin-detail-label">
${escapeHtml(
        key.replace(/([A-Z])/g, ' $1')
            .replace(/^./, char => char.toUpperCase())
    )}
</span>

<span class="admin-detail-value">
${escapeHtml(value)}
</span>
</div>
`).join('');
}

function getSupplierAvailableActions(status) {
    switch (status) {
        case 'PENDING':
            return `
<button class="admin-action primary"
data-action="review">
Start Review
</button>
`;

        case 'UNDER_REVIEW':
            return `
<button class="admin-action success"
data-action="approve">
Approve Supplier
</button>

<button class="admin-action danger"
data-action="reject">
Reject Supplier
</button>
`;

        case 'ACTIVE':
            return `
<button class="admin-action warning"
data-action="suspend">
Suspend Supplier
</button>

<button class="admin-action danger"
data-action="deactivate">
Deactivate Supplier
</button>
`;

        case 'SUSPENDED':
        case 'INACTIVE':
            return `
<button class="admin-action primary"
data-action="reinstate">
Reinstate for Review
</button>
`;

        default:
            return `
<p class="admin-no-actions">
No actions are available for this status.
</p>
`;
    }
}

function renderSupplierPage() {
    currentSupplier = null;
    currentCompanyId = '';

    document.querySelector('#adminPageContent').innerHTML = `
<div class="admin-page-heading">
<div>
<span class="admin-eyebrow">COMPANY MANAGEMENT</span>
<h2>Supplier Companies</h2>
<p>
Search and manage supplier company
registration and approval.
</p>
</div>
</div>

<div class="admin-search-card">
<h3>Find Supplier Company</h3>

<p>
Enter the supplier company ID to retrieve
its details from the system.
</p>

<form id="supplierSearchForm"
style="width:100%;margin-top:20px;">

<div style="
display:flex;
align-items:center;
gap:12px;
width:100%;
">
<input
type="text"
id="supplierCompanyId"
placeholder="Enter Supplier Company UUID"
autocomplete="off"
spellcheck="false"
required
style="
flex:1;
min-width:0;
height:48px;
padding:0 16px;
background:#fff;
color:#111827;
border:1px solid #cbd5e1;
border-radius:8px;
font-size:15px;
box-sizing:border-box;
"
/>

<button type="submit"
id="supplierSearchButton"
style="
flex:0 0 150px;
height:48px;
border:none;
border-radius:8px;
background:#2563eb;
color:white;
font-weight:600;
cursor:pointer;
">
Fetch Details
</button>
</div>
</form>

<p id="supplierMessage" class="admin-message"></p>
</div>

<section id="supplierResult" class="admin-result-section">
<div class="admin-empty-state">
<div>🔎</div>
<h3>No Supplier Selected</h3>
<p>
Enter a supplier company ID above
to view its details and available actions.
</p>
</div>
</section>
`;

    const input = document.querySelector('#supplierCompanyId');

    input.style.setProperty('color', '#111827', 'important');
    input.style.setProperty('background', '#ffffff', 'important');

    document.querySelector('#supplierSearchForm')
        .addEventListener('submit', handleSupplierSearch);
}

async function handleSupplierSearch(event) {
    event.preventDefault();

    const companyId = document
        .querySelector('#supplierCompanyId')
        .value.trim();

    if (!companyId) return;

    currentCompanyId = companyId;

    const button = document.querySelector('#supplierSearchButton');
    const message = document.querySelector('#supplierMessage');

    button.disabled = true;
    button.textContent = 'Fetching...';
    message.textContent = '';

    try {
        await loadSupplierCompany(companyId);
    } catch (error) {
        currentSupplier = null;

        document.querySelector('#supplierResult').innerHTML = `
<div class="admin-error-state">
<h3>Unable to Fetch Supplier</h3>
<p>${escapeHtml(getErrorMessage(error))}</p>
</div>
`;

        showMessage(message, getErrorMessage(error), 'error');

    } finally {
        button.disabled = false;
        button.textContent = 'Fetch Details';
    }
}

async function loadSupplierCompany(companyId) {
    const supplier = await getSupplierCompany(companyId);
    currentSupplier = supplier;
    renderSupplierResult(supplier);
}

function renderSupplierResult(company) {
    const status = String(company.status || '').toUpperCase();

    document.querySelector('#supplierResult').innerHTML = `
<div class="admin-company-card">

<div class="admin-company-header">
<div>
<span class="admin-eyebrow">
SUPPLIER INFORMATION
</span>

<h2>
${escapeHtml(company.companyName || 'Supplier Company')}
</h2>

<p>
${escapeHtml(company.companyCode || company.supplierCode || '')}
</p>
</div>

<span class="admin-status ${getStatusClass(status)}">
${escapeHtml(status || 'UNKNOWN')}
</span>
</div>

<div class="admin-details-grid">
${renderSupplierDetails(company)}
</div>

<div class="admin-actions-section">
<h3>Available Actions</h3>

<div class="admin-actions">
${getSupplierAvailableActions(status)}
</div>
</div>

<div id="supplierActionMessage"
class="admin-message"></div>

<div id="supplierRejectSection"></div>

</div>
`;

    document.querySelectorAll('[data-action]')
        .forEach(button => {
            button.addEventListener('click', () => {
                handleSupplierAction(button.dataset.action);
            });
        });
}

async function handleSupplierAction(action) {
    if (!currentSupplier || !currentCompanyId) return;

    const message = document.querySelector('#supplierActionMessage');

    try {
        if (action === 'reject') {
            renderSupplierRejectionForm();
            return;
        }

        const actionLabels = {
            review: 'start review',
            approve: 'approve',
            suspend: 'suspend',
            deactivate: 'deactivate',
            reinstate: 'reinstate'
        };

        if (!window.confirm(
            `Are you sure you want to ${actionLabels[action]} this supplier?`
        )) return;

        showMessage(message, 'Processing...');

        switch (action) {
            case 'review':
                await startSupplierCompanyReview(currentCompanyId);
                break;
            case 'approve':
                await approveSupplierCompany(currentCompanyId);
                break;
            case 'suspend':
                await suspendSupplierCompany(currentCompanyId);
                break;
            case 'deactivate':
                await deactivateSupplierCompany(currentCompanyId);
                break;
            case 'reinstate':
                await reinstateSupplierCompany(currentCompanyId);
                break;
            default:
                return;
        }

        await loadSupplierCompany(currentCompanyId);

        const updatedMessage =
            document.querySelector('#supplierActionMessage');

        showMessage(
            updatedMessage,
            'Supplier company updated successfully.',
            'success'
        );

    } catch (error) {
        showMessage(message, getErrorMessage(error), 'error');
    }
}

function renderSupplierRejectionForm() {
    const section = document.querySelector('#supplierRejectSection');

    section.innerHTML = `
<div class="admin-rejection-box">
<h3>Reject Supplier Company</h3>

<label for="supplierRejectionReason">
Rejection Reason
</label>

<textarea
id="supplierRejectionReason"
rows="4"
placeholder="Enter the reason for rejecting this supplier..."
required
></textarea>

<div class="admin-rejection-actions">
<button id="cancelSupplierRejection"
class="admin-action secondary">
Cancel
</button>

<button id="confirmSupplierRejection"
class="admin-action danger">
Confirm Rejection
</button>
</div>
</div>
`;

    document.querySelector('#cancelSupplierRejection')
        .addEventListener('click', () => {
            section.innerHTML = '';
        });

    document.querySelector('#confirmSupplierRejection')
        .addEventListener('click', submitSupplierRejection);
}

async function submitSupplierRejection() {
    const reason = document
        .querySelector('#supplierRejectionReason')
        .value.trim();

    const message = document.querySelector('#supplierActionMessage');
    const button = document.querySelector('#confirmSupplierRejection');

    if (!reason) {
        showMessage(message, 'Rejection reason is required.', 'error');
        return;
    }

    if (!window.confirm(
        'Are you sure you want to reject this supplier company?'
    )) return;

    button.disabled = true;
    button.textContent = 'Rejecting...';

    try {
        await rejectSupplierCompany(currentCompanyId, reason);
        await loadSupplierCompany(currentCompanyId);

        showMessage(
            document.querySelector('#supplierActionMessage'),
            'Supplier company rejected successfully.',
            'success'
        );

    } catch (error) {
        showMessage(message, getErrorMessage(error), 'error');
        button.disabled = false;
        button.textContent = 'Confirm Rejection';
    }
}

// =====================================================
// PROCUREMENT COMPANY MANAGEMENT
// =====================================================

function renderProcurementPage() {
    currentProcurementCompany = null;
    currentProcurementPage = 0;

    document.querySelector('#adminPageContent').innerHTML = `
<div class="admin-page-heading">
<div>
<span class="admin-eyebrow">COMPANY MANAGEMENT</span>
<h2>Procurement Companies</h2>
<p>
View and manage procurement company registrations,
approvals and account status.
</p>
</div>
</div>

<div class="admin-search-card">
<div style="
display:flex;
justify-content:space-between;
align-items:center;
gap:16px;
flex-wrap:wrap;
">
<div>
<h3>Registered Procurement Companies</h3>
<p>
Review company information and manage
procurement company accounts.
</p>
</div>

<button id="refreshProcurementButton"
class="admin-action primary">
Refresh Companies
</button>
</div>

<p id="procurementMessage" class="admin-message"></p>
</div>

<section id="procurementTableSection"
class="admin-result-section">
<div class="admin-empty-state">
<p>Loading procurement companies...</p>
</div>
</section>
`;

    document.querySelector('#refreshProcurementButton')
        .addEventListener('click', () => {
            loadProcurementCompanies(currentProcurementPage);
        });

    loadProcurementCompanies(0);
}

async function loadProcurementCompanies(page = 0) {
    const section = document.querySelector('#procurementTableSection');
    const message = document.querySelector('#procurementMessage');

    if (!section) return;

    section.innerHTML = `
<div class="admin-empty-state">
<p>Loading procurement companies...</p>
</div>
`;

    try {
        const response = await getAllProcurementCompanies(page, 10);

        procurementCompanies = response.content || [];
        currentProcurementPage = response.number ?? page;
        totalProcurementPages = response.totalPages ?? 0;

        renderProcurementTable();

        if (message) {
            message.textContent = '';
            message.className = 'admin-message';
        }

    } catch (error) {
        section.innerHTML = `
<div class="admin-error-state">
<h3>Unable to Fetch Procurement Companies</h3>
<p>${escapeHtml(getErrorMessage(error))}</p>
</div>
`;

        showMessage(message, getErrorMessage(error), 'error');
    }
}

function renderProcurementTable() {
    const section = document.querySelector('#procurementTableSection');

    if (!section) return;

    if (!procurementCompanies.length) {
        section.innerHTML = `
<div class="admin-empty-state">
<div>🏭</div>
<h3>No Procurement Companies Found</h3>
<p>
There are currently no procurement companies
available in the system.
</p>
</div>
`;
        return;
    }

    section.innerHTML = `
<div class="admin-company-card">

<div class="admin-company-header">
<div>
<h2>Procurement Company Directory</h2>
<p>
${procurementCompanies.length}
companies on this page
</p>
</div>
</div>

<div style="width:100%;overflow-x:auto;">
<table style="
width:100%;
border-collapse:collapse;
min-width:850px;
">
<thead>
<tr>
<th style="text-align:left;padding:14px;">Company</th>
<th style="text-align:left;padding:14px;">Company Code</th>
<th style="text-align:left;padding:14px;">Email</th>
<th style="text-align:left;padding:14px;">Status</th>
<th style="text-align:left;padding:14px;">Action</th>
</tr>
</thead>

<tbody>
${procurementCompanies.map(company => `
<tr style="border-top:1px solid #e5e7eb;">
<td style="padding:14px;">
<strong>
${escapeHtml(company.companyName || 'N/A')}
</strong>
<br>
<small>
${escapeHtml(company.legalName || '')}
</small>
</td>

<td style="padding:14px;">
${escapeHtml(company.companyCode || company.procurementCode || 'N/A')}
</td>

<td style="padding:14px;">
${escapeHtml(company.emailAddress || 'N/A')}
</td>

<td style="padding:14px;">
<span class="admin-status ${getStatusClass(company.status)}">
${escapeHtml(company.status || 'UNKNOWN')}
</span>
</td>

<td style="padding:14px;">
<button
class="admin-action primary"
data-procurement-view="${escapeHtml(company.id)}">
View Details
</button>
</td>
</tr>
`).join('')}
</tbody>
</table>
</div>

<div style="
display:flex;
justify-content:space-between;
align-items:center;
gap:12px;
margin-top:20px;
flex-wrap:wrap;
">
<span>
Page ${currentProcurementPage + 1}
of ${Math.max(totalProcurementPages, 1)}
</span>

<div style="display:flex;gap:10px;">
<button id="previousProcurementPage"
class="admin-action secondary"
${currentProcurementPage <= 0 ? 'disabled' : ''}>
Previous
</button>

<button id="nextProcurementPage"
class="admin-action primary"
${currentProcurementPage >= totalProcurementPages - 1 ? 'disabled' : ''}>
Next
</button>
</div>
</div>

</div>

<section id="procurementDetailsSection"
style="margin-top:24px;">
</section>
`;

    document.querySelectorAll('[data-procurement-view]')
        .forEach(button => {
            button.addEventListener('click', () => {
                const companyId = button.dataset.procurementView;

                const company = procurementCompanies.find(
                    item => String(item.id) === String(companyId)
                );

                if (company) {
                    currentProcurementCompany = company;
                    renderProcurementDetails(company);
                }
            });
        });

    document.querySelector('#previousProcurementPage')
        ?.addEventListener('click', () => {
            if (currentProcurementPage > 0) {
                loadProcurementCompanies(currentProcurementPage - 1);
            }
        });

    document.querySelector('#nextProcurementPage')
        ?.addEventListener('click', () => {
            if (currentProcurementPage < totalProcurementPages - 1) {
                loadProcurementCompanies(currentProcurementPage + 1);
            }
        });
}

function renderProcurementDetails(company) {
    const section = document.querySelector('#procurementDetailsSection');
    const status = String(company.status || '').toUpperCase();

    section.innerHTML = `
<div class="admin-company-card">

<div class="admin-company-header">
<div>
<span class="admin-eyebrow">
PROCUREMENT COMPANY INFORMATION
</span>

<h2>
${escapeHtml(company.companyName || 'Procurement Company')}
</h2>

<p>
Company ID: ${escapeHtml(company.id)}
</p>
</div>

<span class="admin-status ${getStatusClass(status)}">
${escapeHtml(status)}
</span>
</div>

<div class="admin-details-grid">
${renderSupplierDetails(company)}
</div>

<div class="admin-actions-section">
<h3>Available Actions</h3>

<div class="admin-actions">
${getProcurementAvailableActions(status)}

<button class="admin-action secondary"
data-procurement-action="edit">
Edit Company Details
</button>
</div>
</div>

<div id="procurementActionMessage"
class="admin-message"></div>

<div id="procurementRejectSection"></div>
<div id="procurementEditSection"></div>

</div>
`;

    section.querySelectorAll('[data-procurement-action]')
        .forEach(button => {
            button.addEventListener('click', () => {
                handleProcurementAction(
                    button.dataset.procurementAction
                );
            });
        });
}

function getProcurementAvailableActions(status) {
    switch (status) {
        case 'PENDING':
        case 'UNDER_REVIEW':
            return `
<button class="admin-action success"
data-procurement-action="approve">
Approve Company
</button>

<button class="admin-action danger"
data-procurement-action="reject">
Reject Company
</button>
`;

        case 'ACTIVE':
            return `
<button class="admin-action warning"
data-procurement-action="suspend">
Suspend Company
</button>
`;

        case 'SUSPENDED':
        case 'INACTIVE':
            return `
<button class="admin-action primary"
data-procurement-action="reinstate">
Reinstate for Review
</button>
`;

        default:
            return `
<p class="admin-no-actions">
No actions are available for this status.
</p>
`;
    }
}

async function handleProcurementAction(action) {
    const company = currentProcurementCompany;

    if (!company?.id) return;

    const message = document.querySelector('#procurementActionMessage');

    if (action === 'reject') {
        renderProcurementRejectionForm();
        return;
    }

    if (action === 'edit') {
        renderProcurementEditForm();
        return;
    }

    const labels = {
        approve: 'approve',
        suspend: 'suspend',
        reinstate: 'reinstate'
    };

    if (!window.confirm(
        `Are you sure you want to ${labels[action]} this procurement company?`
    )) return;

    try {
        showMessage(message, 'Processing...');

        switch (action) {
            case 'approve':
                await approveProcurementCompany(company.id);
                break;
            case 'suspend':
                await suspendProcurementCompany(company.id);
                break;
            case 'reinstate':
                await reinstateProcurementCompany(company.id);
                break;
            default:
                return;
        }

        await loadProcurementCompanies(currentProcurementPage);

        const updatedCompany = procurementCompanies.find(
            item => String(item.id) === String(company.id)
        );

        if (updatedCompany) {
            currentProcurementCompany = updatedCompany;
            renderProcurementDetails(updatedCompany);

            showMessage(
                document.querySelector('#procurementActionMessage'),
                'Procurement company updated successfully.',
                'success'
            );
        }

    } catch (error) {
        showMessage(message, getErrorMessage(error), 'error');
    }
}

function renderProcurementRejectionForm() {
    const section = document.querySelector('#procurementRejectSection');

    section.innerHTML = `
<div class="admin-rejection-box">
<h3>Reject Procurement Company</h3>

<label for="procurementRejectionReason">
Rejection Reason
</label>

<textarea
id="procurementRejectionReason"
rows="4"
placeholder="Enter the reason for rejecting this company..."
required
></textarea>

<div class="admin-rejection-actions">
<button id="cancelProcurementRejection"
class="admin-action secondary">
Cancel
</button>

<button id="confirmProcurementRejection"
class="admin-action danger">
Confirm Rejection
</button>
</div>
</div>
`;

    document.querySelector('#cancelProcurementRejection')
        .addEventListener('click', () => {
            section.innerHTML = '';
        });

    document.querySelector('#confirmProcurementRejection')
        .addEventListener('click', submitProcurementRejection);
}

async function submitProcurementRejection() {
    const reason = document
        .querySelector('#procurementRejectionReason')
        .value.trim();

    const message = document.querySelector('#procurementActionMessage');
    const button = document.querySelector('#confirmProcurementRejection');

    if (!reason) {
        showMessage(message, 'Rejection reason is required.', 'error');
        return;
    }

    if (!window.confirm(
        'Are you sure you want to reject this procurement company?'
    )) return;

    button.disabled = true;
    button.textContent = 'Rejecting...';

    try {
        await rejectProcurementCompany(
            currentProcurementCompany.id,
            reason
        );

        await loadProcurementCompanies(currentProcurementPage);

        const updatedCompany = procurementCompanies.find(
            item =>
                String(item.id) ===
                String(currentProcurementCompany.id)
        );

        if (updatedCompany) {
            currentProcurementCompany = updatedCompany;
            renderProcurementDetails(updatedCompany);

            showMessage(
                document.querySelector('#procurementActionMessage'),
                'Procurement company rejected successfully.',
                'success'
            );
        }

    } catch (error) {
        showMessage(message, getErrorMessage(error), 'error');
        button.disabled = false;
        button.textContent = 'Confirm Rejection';
    }
}

// =====================================================
// PROCUREMENT COMPANY EDIT
// =====================================================

function renderProcurementEditForm() {
    const company = currentProcurementCompany;
    const section = document.querySelector('#procurementEditSection');

    section.innerHTML = `
<div class="admin-rejection-box">

<h3>Update Procurement Company</h3>

<label for="procurementLegalName">Legal Name</label>
<input
id="procurementLegalName"
type="text"
value="${escapeHtml(company.legalName || '')}"
/>

<label for="procurementWebsite">Website</label>
<input
id="procurementWebsite"
type="text"
value="${escapeHtml(company.website || '')}"
/>

<label for="procurementContactNumber">Contact Number</label>
<input
id="procurementContactNumber"
type="text"
value="${escapeHtml(company.contactNumber || '')}"
/>

<div class="admin-rejection-actions">
<button id="cancelProcurementEdit"
class="admin-action secondary">
Cancel
</button>

<button id="saveProcurementEdit"
class="admin-action primary">
Save Changes
</button>
</div>

</div>
`;

    document.querySelector('#cancelProcurementEdit')
        .addEventListener('click', () => {
            section.innerHTML = '';
        });

    document.querySelector('#saveProcurementEdit')
        .addEventListener('click', saveProcurementEdit);
}

async function saveProcurementEdit() {
    const company = currentProcurementCompany;
    const message = document.querySelector('#procurementActionMessage');
    const button = document.querySelector('#saveProcurementEdit');

    const data = {
        legalName: document
            .querySelector('#procurementLegalName')
            .value.trim(),

        website: document
            .querySelector('#procurementWebsite')
            .value.trim(),

        contactNumber: document
            .querySelector('#procurementContactNumber')
            .value.trim()
    };

    if (!data.legalName) {
        showMessage(message, 'Legal name is required.', 'error');
        return;
    }

    button.disabled = true;
    button.textContent = 'Saving...';

    try {
        await updateProcurementCompany(company.id, data);

        await loadProcurementCompanies(currentProcurementPage);

        const updatedCompany = procurementCompanies.find(
            item => String(item.id) === String(company.id)
        );

        if (updatedCompany) {
            currentProcurementCompany = updatedCompany;
            renderProcurementDetails(updatedCompany);

            showMessage(
                document.querySelector('#procurementActionMessage'),
                'Procurement company details updated successfully.',
                'success'
            );
        }

    } catch (error) {
        showMessage(message, getErrorMessage(error), 'error');
        button.disabled = false;
        button.textContent = 'Save Changes';
    }
}

// =====================================================
// LOAD PROCUREMENT COMPANIES FOR EMPLOYEE DROPDOWN
// =====================================================

async function loadEmployeeProcurementCompanies() {
    const companySelect = document.querySelector(
        '#employeeCompanySysId'
    );

    const note = document.querySelector('#employeeCompanyNote');

    if (!companySelect) return;

    companySelect.innerHTML =
        '<option value="">Loading procurement companies...</option>';

    companySelect.disabled = true;

    if (note) note.textContent = '';

    try {
        const response = await getAllProcurementCompanies(0, 100);

        const companies = Array.isArray(response.content)
            ? response.content
            : [];

        if (companies.length === 0) {
            companySelect.innerHTML =
                '<option value="">No procurement companies found</option>';
            return;
        }

        const validCompanies = companies.filter(company =>
            company.id && company.companyName
        );

        if (validCompanies.length === 0) {
            companySelect.innerHTML =
                '<option value="">No valid procurement companies found</option>';
            return;
        }

        companySelect.innerHTML = `
            <option value="">Select Procurement Company</option>

            ${validCompanies.map(company => `
                <option value="${escapeHtml(company.id)}">
                    ${escapeHtml(company.companyName)}
                </option>
            `).join('')}
        `;

    } catch (error) {
        console.error('Procurement dropdown error:', error);

        companySelect.innerHTML =
            '<option value="">Unable to load companies</option>';

        if (note) {
            note.textContent = getErrorMessage(error);
        }

    } finally {
        companySelect.disabled = false;
    }
}
// =====================================================
// EMPLOYEE MANAGEMENT
// =====================================================

function renderEmployeePage() {
    document.querySelector('#adminPageContent').innerHTML = `
<div class="admin-page-heading">
<div>
<span class="admin-eyebrow">
EMPLOYEE MANAGEMENT
</span>

<h2>Employee Management</h2>

<p>
Invite employees, assign reporting managers
and activate employee accounts.
</p>
</div>
</div>

<div class="admin-search-card">
<h3>Invite New Employee</h3>

<p>
Enter the employee's details to send an
account activation invitation.
</p>

<form id="inviteEmployeeForm"
style="margin-top:20px;">

<div class="admin-details-grid">

<div class="admin-detail-item">
<label for="employeeCompanySysId">
Procurement Company *
</label>

<select
id="employeeCompanySysId"
name="companySysId"
required
>
<option value="">
Loading procurement companies...
</option>
</select>

<small id="employeeCompanyNote"></small>
</div>

<div class="admin-detail-item">
<label for="employeeFirstName">
First Name *
</label>
<input id="employeeFirstName"
type="text"
autocomplete="given-name"
required />
</div>

<div class="admin-detail-item">
<label for="employeeLastName">
Last Name *
</label>
<input id="employeeLastName"
type="text"
autocomplete="family-name"
required />
</div>

<div class="admin-detail-item">
<label for="employeeEmailAddress">
Personal Email Address *
</label>
<input id="employeeEmailAddress"
type="email"
autocomplete="email"
required />
</div>

<div class="admin-detail-item">
<label for="employeeWorkEmail">
Work Email
</label>
<input id="employeeWorkEmail"
type="email" />
</div>

<div class="admin-detail-item">
<label for="employeePhoneNumber">
Personal Phone Number
</label>
<input id="employeePhoneNumber"
type="text" />
</div>

<div class="admin-detail-item">
<label for="employeeWorkPhone">
Work Phone
</label>
<input id="employeeWorkPhone"
type="text" />
</div>

<div class="admin-detail-item">
<label for="employeeDepartment">
Department *
</label>
<input id="employeeDepartment"
type="text"
required />
</div>

<div class="admin-detail-item">
<label for="employeeDesignation">
Designation *
</label>
<input id="employeeDesignation"
type="text"
required />
</div>

<div class="admin-detail-item">
<label for="employeeEmploymentType">
Employment Type *
</label>

<select id="employeeEmploymentType" required>
<option value="">Select employment type</option>
<option value="FULL_TIME">Full Time</option>
<option value="PART_TIME">Part Time</option>
<option value="CONTRACT">Contract</option>
<option value="INTERN">Intern</option>
<option value="TEMPORARY">Temporary</option>
<option value="CONSULTANT">Consultant</option>
</select>
</div>

<div class="admin-detail-item">
<label for="employeeJoiningDate">
Joining Date
</label>
<input id="employeeJoiningDate"
type="date" />
</div>

<div class="admin-detail-item">
<label for="employeeProbationEndDate">
Probation End Date
</label>
<input id="employeeProbationEndDate"
type="date" />
</div>

<div class="admin-detail-item">
<label for="employeeManagerSysId">
Manager ID (Optional)
</label>
<input id="employeeManagerSysId"
type="text"
placeholder="Manager UUID" />
</div>

<div class="admin-detail-item">
<label for="employeeOfficeLocation">
Office Location
</label>
<input id="employeeOfficeLocation"
type="text" />
</div>

<div class="admin-detail-item">
<label for="employeeRoleSysIds">
Role IDs *
</label>
<input id="employeeRoleSysIds"
type="text"
placeholder="Role UUIDs separated by commas"
required />
</div>

</div>

<p class="admin-message">
Enter valid company and role UUIDs.
Separate multiple role UUIDs with commas.
</p>

<button type="submit"
id="inviteEmployeeButton"
class="admin-action primary">
Send Employee Invitation
</button>

</form>

<div id="employeeInviteMessage"
class="admin-message"></div>
</div>

<div class="admin-search-card"
style="margin-top:24px;">

<h3>Assign Employee Manager</h3>

<p>
Assign or update the reporting manager for
an existing employee.
</p>

<form id="assignManagerForm"
style="margin-top:20px;">

<div class="admin-details-grid">

<div class="admin-detail-item">
<label for="managerEmployeeSysId">
Employee ID *
</label>
<input id="managerEmployeeSysId"
type="text"
placeholder="Employee UUID"
required />
</div>

<div class="admin-detail-item">
<label for="managerSysId">
Manager ID *
</label>
<input id="managerSysId"
type="text"
placeholder="Manager UUID"
required />
</div>

</div>

<button type="submit"
id="assignManagerButton"
class="admin-action primary">
Assign Manager
</button>

</form>

<div id="managerAssignmentMessage"
class="admin-message"></div>
</div>

<div class="admin-search-card"
style="margin-top:24px;">

<h3>Activate Employee Account</h3>

<p>
Complete account activation using the token
received through the employee invitation email.
</p>

<form id="activateEmployeeForm"
style="margin-top:20px;">

<div class="admin-details-grid">

<div class="admin-detail-item">
<label for="employeeActivationToken">
Activation Token *
</label>
<input id="employeeActivationToken"
type="text"
required />
</div>

<div class="admin-detail-item">
<label for="employeeNewPassword">
New Password *
</label>
<input id="employeeNewPassword"
type="password"
minlength="8"
required />
</div>

<div class="admin-detail-item">
<label for="employeeConfirmPassword">
Confirm Password *
</label>
<input id="employeeConfirmPassword"
type="password"
minlength="8"
required />
</div>

</div>

<button type="submit"
id="activateEmployeeButton"
class="admin-action success">
Activate Account
</button>

</form>

<div id="employeeActivationMessage"
class="admin-message"></div>
</div>
`;

    document.querySelector('#inviteEmployeeForm')
        .addEventListener('submit', handleEmployeeInvitation);

    document.querySelector('#assignManagerForm')
        .addEventListener('submit', handleManagerAssignment);

    document.querySelector('#activateEmployeeForm')
        .addEventListener('submit', handleEmployeeActivation);

    loadEmployeeProcurementCompanies();
}

// =====================================================
// EMPLOYEE INVITATION
// =====================================================

async function handleEmployeeInvitation(event) {
    event.preventDefault();

    const button = document.querySelector('#inviteEmployeeButton');
    const message = document.querySelector('#employeeInviteMessage');

    const roleIds = document
        .querySelector('#employeeRoleSysIds')
        .value
        .split(',')
        .map(id => id.trim())
        .filter(Boolean);

    if (!roleIds.length) {
        showMessage(message, 'At least one role ID is required.', 'error');
        return;
    }

    const selectedCompanySysId = document
        .querySelector('#employeeCompanySysId')
        .value;

    if (!selectedCompanySysId) {
        showMessage(
            message,
            'Please select a company with a valid company UUID. The numeric company ID is not accepted by this API.',
            'error'
        );
        return;
    }

    const employeeData = {
        companySysId: selectedCompanySysId,

        firstName: document
            .querySelector('#employeeFirstName')
            .value.trim(),

        lastName: document
            .querySelector('#employeeLastName')
            .value.trim(),

        emailAddress: document
            .querySelector('#employeeEmailAddress')
            .value.trim(),

        department: document
            .querySelector('#employeeDepartment')
            .value.trim(),

        designation: document
            .querySelector('#employeeDesignation')
            .value.trim(),

        employmentType: document
            .querySelector('#employeeEmploymentType')
            .value,

        roleSysIds: roleIds
    };

    const optionalFields = {
        workEmail: '#employeeWorkEmail',
        workPhone: '#employeeWorkPhone',
        phoneNumber: '#employeePhoneNumber',
        officeLocation: '#employeeOfficeLocation',
        managerSysId: '#employeeManagerSysId',
        joiningDate: '#employeeJoiningDate',
        probationEndDate: '#employeeProbationEndDate'
    };

    Object.entries(optionalFields).forEach(([key, selector]) => {
        const value = document.querySelector(selector).value.trim();

        if (value) {
            employeeData[key] = value;
        }
    });

    button.disabled = true;
    button.textContent = 'Sending Invitation...';

    showMessage(message, 'Submitting employee invitation...');

    try {
        await inviteEmployee(employeeData);

        showMessage(
            message,
            'Employee invitation submitted successfully.',
            'success'
        );

        document.querySelector('#inviteEmployeeForm').reset();

    } catch (error) {
        showMessage(message, getErrorMessage(error), 'error');

    } finally {
        button.disabled = false;
        button.textContent = 'Send Employee Invitation';
    }
}

// =====================================================
// ASSIGN EMPLOYEE MANAGER
// =====================================================

async function handleManagerAssignment(event) {
    event.preventDefault();

    const employeeSysId = document
        .querySelector('#managerEmployeeSysId')
        .value.trim();

    const managerSysId = document
        .querySelector('#managerSysId')
        .value.trim();

    const button = document.querySelector('#assignManagerButton');
    const message = document.querySelector('#managerAssignmentMessage');

    if (!employeeSysId || !managerSysId) {
        showMessage(
            message,
            'Both employee ID and manager ID are required.',
            'error'
        );
        return;
    }

    if (employeeSysId === managerSysId) {
        showMessage(
            message,
            'An employee cannot be assigned as their own manager.',
            'error'
        );
        return;
    }

    button.disabled = true;
    button.textContent = 'Assigning...';

    showMessage(message, 'Updating employee manager...');

    try {
        const response = await assignEmployeeManager(
            employeeSysId,
            managerSysId
        );

        showMessage(message, 'Manager assigned successfully.', 'success');

        console.log('Updated employee:', response);

        document.querySelector('#assignManagerForm').reset();

    } catch (error) {
        showMessage(message, getErrorMessage(error), 'error');

    } finally {
        button.disabled = false;
        button.textContent = 'Assign Manager';
    }
}

// =====================================================
// EMPLOYEE ACCOUNT ACTIVATION
// =====================================================

async function handleEmployeeActivation(event) {
    event.preventDefault();

    const token = document
        .querySelector('#employeeActivationToken')
        .value.trim();

    const password = document
        .querySelector('#employeeNewPassword')
        .value;

    const confirmPassword = document
        .querySelector('#employeeConfirmPassword')
        .value;

    const button = document.querySelector('#activateEmployeeButton');
    const message = document.querySelector('#employeeActivationMessage');

    if (!token || !password || !confirmPassword) {
        showMessage(message, 'All activation fields are required.', 'error');
        return;
    }

    if (password.length < 8) {
        showMessage(
            message,
            'Password must contain at least 8 characters.',
            'error'
        );
        return;
    }

    if (password !== confirmPassword) {
        showMessage(message, 'Passwords do not match.', 'error');
        return;
    }

    button.disabled = true;
    button.textContent = 'Activating...';

    showMessage(message, 'Activating employee account...');

    try {
        await activateEmployee(token, password);

        showMessage(
            message,
            'Employee account activated successfully.',
            'success'
        );

        document.querySelector('#activateEmployeeForm').reset();

    } catch (error) {
        showMessage(message, getErrorMessage(error), 'error');

    } finally {
        button.disabled = false;
        button.textContent = 'Activate Account';
    }
}

// =====================================================
// ROLE MANAGEMENT
// =====================================================

function renderRolePage() {
    const content = document.querySelector('#adminPageContent');

    content.innerHTML = `
<div class="admin-page-heading">
<div>
<span class="admin-eyebrow">
ACCESS CONTROL
</span>

<h2>Role Management</h2>

<p>
Create custom roles and assign permissions
to employees within a company.
</p>
</div>
</div>

<div class="admin-search-card">

<h3>Create Custom Role</h3>

<p>
Enter the company's UUID, provide a role name,
and select the permissions required for this role.
</p>

<form id="createRoleForm" style="margin-top:20px;">

<div class="admin-details-grid">

<div class="admin-detail-item">
<label for="roleCompanySysId">
Company UUID *
</label>

<input
id="roleCompanySysId"
type="text"
placeholder="Enter company UUID"
autocomplete="off"
required
/>
</div>

<div class="admin-detail-item">
<label for="roleName">
Role Name *
</label>

<input
id="roleName"
type="text"
placeholder="Example: Procurement Manager"
required
/>
</div>

</div>

<div class="admin-actions-section">
<h3>Select Permissions</h3>

<p>
Select the operations that this custom role
should be allowed to perform.
</p>

<div style="
display:flex;
gap:10px;
flex-wrap:wrap;
margin:15px 0;
">
<button type="button"
id="selectAllRolePermissions"
class="admin-action secondary">
Select All
</button>

<button type="button"
id="clearAllRolePermissions"
class="admin-action secondary">
Clear All
</button>
</div>

<div id="rolePermissionGroups">
${renderRolePermissionGroups()}
</div>
</div>

<div id="roleCreateMessage"
class="admin-message"></div>

<button type="submit"
id="createRoleButton"
class="admin-action primary">
Create Role
</button>

</form>
</div>


`;

    document.querySelector('#createRoleForm')
        .addEventListener('submit', handleCreateRole);

    document.querySelector('#selectAllRolePermissions')
        .addEventListener('click', () => {
            document.querySelectorAll('.role-permission-checkbox')
                .forEach(checkbox => {
                    checkbox.checked = true;
                });
        });

    document.querySelector('#clearAllRolePermissions')
        .addEventListener('click', () => {
            document.querySelectorAll('.role-permission-checkbox')
                .forEach(checkbox => {
                    checkbox.checked = false;
                });
        });
}

function renderRolePermissionGroups() {
    return Object.entries(ROLE_PERMISSION_GROUPS)
        .map(([category, operations]) => `
<div style="
border:1px solid #e5e7eb;
border-radius:10px;
padding:16px;
margin-bottom:14px;
background:#fff;
">

<h4 style="
margin:0 0 12px;
text-transform:capitalize;
">
${escapeHtml(category.replace(/_/g, ' '))}
</h4>

<div style="
display:grid;
grid-template-columns:repeat(auto-fit,minmax(220px,1fr));
gap:12px;
">
${operations.map(operation => `
<label style="
display:flex;
align-items:center;
gap:9px;
font-size:14px;
cursor:pointer;
">
<input
type="checkbox"
class="role-permission-checkbox"
value="${escapeHtml(operation)}"
/>

<span>${escapeHtml(operation)}</span>
</label>
`).join('')}
</div>

</div>
`).join('');
}

async function handleCreateRole(event) {
    event.preventDefault();

    const companySysId = document
        .querySelector('#roleCompanySysId')
        .value.trim();

    const roleName = document
        .querySelector('#roleName')
        .value.trim();

    const message = document.querySelector('#roleCreateMessage');
    const button = document.querySelector('#createRoleButton');

    const selectedOperations = [
        ...document.querySelectorAll('.role-permission-checkbox:checked')
    ].map(checkbox => checkbox.value);

    if (!companySysId) {
        showMessage(message, 'Company UUID is required.', 'error');
        return;
    }

    if (!roleName) {
        showMessage(message, 'Role name is required.', 'error');
        return;
    }

    if (!selectedOperations.length) {
        showMessage(
            message,
            'Select at least one permission for this role.',
            'error'
        );
        return;
    }

    const roleData = {
        companySysId,
        roleName,
        operationName: selectedOperations
    };

    button.disabled = true;
    button.textContent = 'Creating Role...';

    showMessage(message, 'Creating custom role...');

    try {
        const response = await createRole(roleData);

        console.log('Created role response:', response);

        showMessage(
            message,
            'Role created successfully. The backend has accepted the selected permissions.',
            'success'
        );

        document.querySelector('#createRoleForm').reset();

    } catch (error) {
        console.error('Create role error:', error);

        showMessage(
            message,
            getErrorMessage(error),
            'error'
        );

    } finally {
        button.disabled = false;
        button.textContent = 'Create Role';
    }
}

// =====================================================
// DASHBOARD ENTRY POINT
// =====================================================

export function renderInternalAdminDashboard() {
    renderDashboardLayout();
}

// =====================================================
// INTERNAL ADMIN PROFILE DROPDOWN
// =====================================================

function renderAdminProfile() {
    let user = {};

    try {
        user = JSON.parse(
            localStorage.getItem('loggedInUser') || '{}'
        );
    } catch (error) {
        console.error('Unable to read admin profile:', error);
    }

    const adminName =
        user.name ||
        user.fullName ||
        [user.firstName, user.lastName]
            .filter(Boolean)
            .join(' ') ||
        'Internal Admin';

    return `
        <div class="admin-user-menu" id="adminUserMenu">

            <button
                type="button"
                class="admin-user-trigger"
                id="adminUserTrigger"
                aria-expanded="false"
                aria-haspopup="true"
            >
                <span class="admin-user-avatar">
                    ${escapeHtml(adminName.charAt(0).toUpperCase())}
                </span>

                <span class="admin-user-name">
                    ${escapeHtml(adminName)}
                </span>

                <span class="admin-user-arrow">⌄</span>
            </button>

            <div
                class="admin-user-dropdown"
                id="adminUserDropdown"
            >
                <button
                    type="button"
                    id="adminProfileLogout"
                >
                    Logout
                </button>
            </div>

        </div>
    `;
}

function attachAdminProfileEvents() {
    const trigger = document.querySelector('#adminUserTrigger');
    const dropdown = document.querySelector('#adminUserDropdown');
    const menu = document.querySelector('#adminUserMenu');
    const logoutButton = document.querySelector('#adminProfileLogout');

    if (!trigger || !dropdown || !menu || !logoutButton) {
        return;
    }

    trigger.addEventListener('click', (event) => {
        event.stopPropagation();

        const isOpen = dropdown.classList.toggle('show');

        trigger.setAttribute('aria-expanded', String(isOpen));
    });

    logoutButton.addEventListener('click', () => {
        logoutEmployee();
    });

    document.addEventListener('click', (event) => {
        if (!menu.contains(event.target)) {
            dropdown.classList.remove('show');
            trigger.setAttribute('aria-expanded', 'false');
        }
    });
}
