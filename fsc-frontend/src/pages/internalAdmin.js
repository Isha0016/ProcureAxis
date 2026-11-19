import {
    getSupplierCompany,
    startSupplierCompanyReview,
    approveSupplierCompany,
    rejectSupplierCompany,
    suspendSupplierCompany,
    deactivateSupplierCompany,
    reinstateSupplierCompany
} from '../api/supplierCompany.api.js';

import './internalAdmin.css';

const app = document.querySelector('#app');

let currentSupplier = null;
let currentCompanyId = '';

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

function getAvailableActions(status) {
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
                        disabled
                        title="Coming in the next module">
                    <span>🏭</span>
                    Procurement Companies
                </button>

                <button class="admin-nav-item"
                        disabled
                        title="Coming in the next module">
                    <span>👥</span>
                    Employee Management
                </button>

                <button class="admin-nav-item"
                        disabled
                        title="Coming in the next module">
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

                    <div class="admin-user-label">
                        <span class="admin-user-avatar">A</span>
                        <span>Internal Admin</span>
                    </div>
                </header>

                <section class="admin-content">

                    <div class="admin-page-heading">
                        <div>
                            <span class="admin-eyebrow">
                                COMPANY MANAGEMENT
                            </span>
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

                        <form id="supplierSearchForm" style="
    width: 100%;
    margin-top: 20px;
">
    <div id="supplierSearchRow" style="
        display: flex;
        align-items: center;
        gap: 12px;
        width: 100%;
    ">
        <input
            type="text"
            id="supplierCompanyId"
            placeholder="Enter Supplier Company UUID"
            autocomplete="off"
            spellcheck="false"
            required
            style="
                display: block;
                flex: 1 1 0;
                width: 100%;
                min-width: 0;
                height: 48px;
                padding: 0 16px;
                background: #ffffff;
                color: #111827;
                border: 1px solid #cbd5e1;
                border-radius: 8px;
                font-size: 15px;
                outline: none;
                box-sizing: border-box;
                cursor: text;
            "
        />

        <button
            type="submit"
            id="supplierSearchButton"
            style="
                flex: 0 0 150px;
                width: 150px;
                height: 48px;
                padding: 0 12px;
                border: none;
                border-radius: 8px;
                background: #2563eb;
                color: #ffffff;
                font-size: 14px;
                font-weight: 600;
                white-space: nowrap;
                cursor: pointer;
            "
        >
            Fetch Details
        </button>
    </div>
</form>
                        <p id="supplierMessage"
                           class="admin-message"></p>

                    </div>

                    <section id="supplierResult"
                             class="admin-result-section">

                        <div class="admin-empty-state">
                            <div>🔎</div>
                            <h3>No Supplier Selected</h3>
                            <p>
                                Enter a supplier company ID above
                                to view its details and available actions.
                            </p>
                        </div>

                    </section>

                </section>

            </main>

        </div>
    `;
    const supplierIdInput = document.querySelector('#supplierCompanyId');

    supplierIdInput.style.setProperty('color', '#111827', 'important');
    supplierIdInput.style.setProperty('background', '#ffffff', 'important');
    supplierIdInput.style.setProperty('width', '100%', 'important');
    supplierIdInput.style.setProperty('min-width', '0', 'important');
    supplierIdInput.style.setProperty('cursor', 'text', 'important');

    supplierIdInput.disabled = false;
    supplierIdInput.readOnly = false;

    document.querySelector('#adminLogoutButton')
        .addEventListener('click', () => {
            localStorage.removeItem('token');
            window.location.reload();
        });

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

        message.textContent = getErrorMessage(error);
        message.className = 'admin-message error';

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
                    ${getAvailableActions(status)}
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
            renderRejectionForm();
            return;
        }

        const actionLabels = {
            review: 'start review',
            approve: 'approve',
            suspend: 'suspend',
            deactivate: 'deactivate',
            reinstate: 'reinstate'
        };

        const confirmed = window.confirm(
            `Are you sure you want to ${actionLabels[action]} this supplier?`
        );

        if (!confirmed) return;

        message.textContent = 'Processing...';
        message.className = 'admin-message';

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

        const updatedMessage = document.querySelector('#supplierActionMessage');

        if (updatedMessage) {
            updatedMessage.textContent = 'Supplier company updated successfully.';
            updatedMessage.className = 'admin-message success';
        }

    } catch (error) {
        message.textContent = getErrorMessage(error);
        message.className = 'admin-message error';
    }
}

function renderRejectionForm() {
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
        message.textContent = 'Rejection reason is required.';
        message.className = 'admin-message error';
        return;
    }

    const confirmed = window.confirm(
        'Are you sure you want to reject this supplier company?'
    );

    if (!confirmed) return;

    button.disabled = true;
    button.textContent = 'Rejecting...';

    try {
        await rejectSupplierCompany(currentCompanyId, reason);

        await loadSupplierCompany(currentCompanyId);

        const updatedMessage = document.querySelector('#supplierActionMessage');

        if (updatedMessage) {
            updatedMessage.textContent = 'Supplier company rejected successfully.';
            updatedMessage.className = 'admin-message success';
        }

    } catch (error) {
        message.textContent = getErrorMessage(error);
        message.className = 'admin-message error';

        button.disabled = false;
        button.textContent = 'Confirm Rejection';
    }
}

export function renderInternalAdminDashboard() {
    renderDashboardLayout();
}