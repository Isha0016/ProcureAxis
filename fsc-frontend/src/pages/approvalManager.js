import spoApi from '../api/spoAxios.js';

import {
    getPurchaseRequest,
    getPurchaseRequestHistory
} from '../api/purchaseRequests.api.js';

// -----------------------------
// APPROVAL MANAGER DASHBOARD
// -----------------------------

const SUBMITTED_REQUESTS_KEY = 'submittedPurchaseRequests';

function getSubmittedRequests() {
    try {
        return JSON.parse(
            localStorage.getItem(SUBMITTED_REQUESTS_KEY) || '[]'
        );
    } catch (error) {
        console.error('Unable to read submitted requests:', error);
        return [];
    }
}

// -----------------------------
// APPROVAL MANAGER PROFILE
// -----------------------------

function getApprovalManagerProfile() {
    try {
        const token = localStorage.getItem('token');

        if (!token) {
            return {
                name: 'Approval Manager',
                id: 'ID unavailable'
            };
        }

        const jwt = token.replace(/^Bearer\s+/i, '');
        const payload = jwt.split('.')[1];

        if (!payload) {
            throw new Error('Invalid JWT token');
        }

        const base64 = payload
            .replace(/-/g, '+')
            .replace(/_/g, '/');

        const decoded = atob(base64);
        const bytes = Uint8Array.from(
            decoded,
            character => character.charCodeAt(0)
        );

        const user = JSON.parse(
            new TextDecoder().decode(bytes)
        );

        const name =
            user.fullName ||
            user.name ||
            user.employeeName ||
            user.username ||
            user.email ||
            'Approval Manager';

        const id =
            user.employeeId ||
            user.userId ||
            user.sysId ||
            user.id ||
            user.sub ||
            'ID unavailable';

        return { name, id };

    } catch (error) {
        console.error('Unable to read manager profile:', error);

        return {
            name: 'Approval Manager',
            id: 'ID unavailable'
        };
    }
}

export function renderApprovalManagerDashboard() {

    document.querySelector('#app').innerHTML = `
        <div class="procurement-layout">

            <aside class="procurement-sidebar">

                <div class="procurement-brand">
                    <h2>ProcureAxis</h2>
                    <span>APPROVAL MANAGER</span>
                </div>

                <nav class="procurement-nav">
                    <button class="active" data-page="overview">
                        Overview
                    </button>

                    <button data-page="pending">
                        Pending Requests
                    </button>

                    <button data-page="history">
                        Approval History
                    </button>
                </nav>

            </aside>

            <main class="procurement-main">

                <header class="procurement-header">

    <div>
        <h2 id="approval-page-title">
            Approval Dashboard
        </h2>

        <p>Review and manage purchase request approvals</p>
    </div>

    <div class="approval-manager-profile">

        <div class="approval-manager-info">
            <strong id="approval-manager-name">
                Approval Manager
            </strong>

            <span id="approval-manager-id">
                ID: Loading...
            </span>
        </div>

        <button
            id="approval-manager-logout"
            class="procurement-back-btn">

            Logout

        </button>

    </div>

</header>

                <section id="approval-content">
                    <p>Loading approval dashboard...</p>
                </section>

            </main>

        </div>
    `;

    // Display logged-in Approval Manager details
    const manager = getApprovalManagerProfile();

    document.querySelector('#approval-manager-name').textContent =
        manager.name;

    document.querySelector('#approval-manager-id').textContent =
        `ID: ${manager.id}`;


// Logout functionality
    document.querySelector('#approval-manager-logout')
        .addEventListener('click', () => {

            const confirmed = confirm(
                'Are you sure you want to logout?'
            );

            if (!confirmed) {
                return;
            }

            // Remove only the authentication token
            localStorage.removeItem('token');

            // Preserve submittedPurchaseRequests
            window.location.reload();

        });

    document.querySelectorAll('.procurement-nav button')
        .forEach(button => {

            button.addEventListener('click', () => {

                document.querySelectorAll('.procurement-nav button')
                    .forEach(item => item.classList.remove('active'));

                button.classList.add('active');

                loadApprovalPage(button.dataset.page);

            });

        });

    loadApprovalPage('overview');
}

// -----------------------------
// PAGE NAVIGATION
// -----------------------------

async function loadApprovalPage(page) {

    const content = document.querySelector('#approval-content');
    const title = document.querySelector('#approval-page-title');

    const titles = {
        overview: 'Approval Dashboard',
        pending: 'Pending Purchase Requests',
        history: 'Approval History'
    };

    title.textContent = titles[page] || 'Approval Dashboard';

    if (page === 'overview') {
        await renderOverview(content);
    } else if (page === 'pending') {
        await renderPendingRequests(content);
    } else if (page === 'history') {
        await renderApprovalHistory(content);
    }
}

// -----------------------------
// LOAD REQUESTS
// -----------------------------

async function loadLatestRequests() {

    const savedRequests = getSubmittedRequests();

    const requests = await Promise.all(
        savedRequests.map(async saved => {

            try {

                return await getPurchaseRequest(saved.sysId);

            } catch (error) {

                console.error(
                    'Unable to load request:',
                    saved.sysId,
                    error
                );

                return saved;

            }

        })
    );

    return requests.filter(request => request && request.sysId);
}

// -----------------------------
// OVERVIEW
// -----------------------------

async function renderOverview(content) {

    content.innerHTML = `
        <div class="procurement-welcome">
            <h2>Welcome to Approval Management</h2>

            <p>
                Review purchase requests and manage approval decisions.
            </p>
        </div>

        <div class="procurement-stats">

            <div class="procurement-stat-card">
                <span>Total Requests</span>
                <h2 id="approval-total">--</h2>
            </div>

            <div class="procurement-stat-card">
                <span>Pending Requests</span>
                <h2 id="approval-pending">--</h2>
            </div>

            <div class="procurement-stat-card">
                <span>Approved Requests</span>
                <h2 id="approval-approved">--</h2>
            </div>

            <div class="procurement-stat-card">
                <span>Rejected Requests</span>
                <h2 id="approval-rejected">--</h2>
            </div>

        </div>

        <div class="procurement-panel">
            <h3>Recent Purchase Requests</h3>
            <div id="approval-recent">Loading...</div>
        </div>
    `;

    try {

        const requests = await loadLatestRequests();

        document.querySelector('#approval-total').textContent =
            requests.length;

        document.querySelector('#approval-pending').textContent =
            requests.filter(request =>
                isPendingStatus(request.requestStatus)
            ).length;

        document.querySelector('#approval-approved').textContent =
            requests.filter(request =>
                request.requestStatus === 'APPROVED'
            ).length;

        document.querySelector('#approval-rejected').textContent =
            requests.filter(request =>
                request.requestStatus === 'REJECTED'
            ).length;

        const recent = requests.slice(-5).reverse();

        document.querySelector('#approval-recent').innerHTML =
            recent.length
                ? recent.map(request => `
                    <div class="procurement-list-row">
                        <div>
                            <strong>
                                ${escapeHtml(request.requestNumber || '-')}
                            </strong>

                            <p>
                                ${escapeHtml(request.title || '-')}
                            </p>
                        </div>

                        <span class="procurement-status">
                            ${escapeHtml(request.requestStatus || '-')}
                        </span>
                    </div>
                `).join('')
                : '<p>No purchase requests found.</p>';

    } catch (error) {

        console.error('Approval dashboard error:', error);

        document.querySelector('#approval-recent').innerHTML =
            '<p>Unable to load purchase requests.</p>';

    }
}

// -----------------------------
// PENDING REQUESTS
// -----------------------------

function isPendingStatus(status) {

    return [
        'SUBMITTED',
        'PENDING',
        'PENDING_APPROVAL',
        'UNDER_REVIEW'
    ].includes(String(status || '').toUpperCase());

}

async function renderPendingRequests(content) {

    content.innerHTML = `
        <div class="procurement-panel">

            <h3>Pending Purchase Requests</h3>

            <p>
                Review submitted purchase requests and take approval decisions.
            </p>

            <div id="pending-request-table">
                Loading requests...
            </div>

        </div>
    `;

    try {

        const requests = await loadLatestRequests();

        const pending = requests.filter(request =>
            isPendingStatus(request.requestStatus)
        );

        const table = document.querySelector('#pending-request-table');

        if (!pending.length) {

            table.innerHTML = `
                <p>No pending purchase requests found.</p>
                <p>
                    Requests submitted by Procurement Users will appear here
                    when their latest status is pending approval.
                </p>
            `;

            return;
        }

        table.innerHTML = `
            <div class="procurement-table-wrapper">

                <table class="procurement-table">

                    <thead>
                        <tr>
                            <th>Request Number</th>
                            <th>Title</th>
                            <th>Priority</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                    </thead>

                    <tbody>

                        ${pending.map(request => `
                            <tr>

                                <td>
                                    ${escapeHtml(request.requestNumber || '-')}
                                </td>

                                <td>
                                    ${escapeHtml(request.title || '-')}
                                </td>

                                <td>
                                    ${escapeHtml(request.priority || '-')}
                                </td>

                                <td>
                                    ${escapeHtml(request.requestStatus || '-')}
                                </td>

                                <td>

                                    <button
                                        class="procurement-action-btn"
                                        data-review="${escapeHtml(request.sysId)}">

                                        Review

                                    </button>

                                </td>

                            </tr>
                        `).join('')}

                    </tbody>

                </table>

            </div>
        `;

        table.querySelectorAll('[data-review]')
            .forEach(button => {

                button.addEventListener('click', () => {
                    renderRequestDetails(button.dataset.review);
                });

            });

    } catch (error) {

        console.error('Pending requests error:', error);

        document.querySelector('#pending-request-table').innerHTML =
            '<p>Unable to load pending requests.</p>';

    }
}

// -----------------------------
// REQUEST DETAILS
// -----------------------------

async function renderRequestDetails(requestId) {

    const content = document.querySelector('#approval-content');

    content.innerHTML = `
        <div class="procurement-panel">
            <h3>Purchase Request Details</h3>
            <p>Loading request...</p>
        </div>
    `;

    try {

        const request = await getPurchaseRequest(requestId);

        const lines = request.requestLines || [];

        content.innerHTML = `
            <div class="procurement-panel">

                <h3>Purchase Request Details</h3>

                <p>
                    <strong>Request Number:</strong>
                    ${escapeHtml(request.requestNumber || '-')}
                </p>

                <p>
                    <strong>Request ID:</strong>
                    ${escapeHtml(request.sysId || requestId)}
                </p>

                <p>
                    <strong>Title:</strong>
                    ${escapeHtml(request.title || '-')}
                </p>

                <p>
                    <strong>Description:</strong>
                    ${escapeHtml(request.description || '-')}
                </p>

                <p>
                    <strong>Status:</strong>
                    ${escapeHtml(request.requestStatus || '-')}
                </p>

                <p>
                    <strong>Priority:</strong>
                    ${escapeHtml(request.priority || '-')}
                </p>

                <p>
                    <strong>Required Date:</strong>
                    ${escapeHtml(request.requiredDate || '-')}
                </p>

                <p>
                    <strong>Delivery Location:</strong>
                    ${escapeHtml(request.deliveryLocation || '-')}
                </p>

                <p>
                    <strong>Estimated Total:</strong>
                    ${formatAmount(
            request.estimatedTotalAmount,
            request.currencyCode
        )}
                </p>

                <h3>Requested Products</h3>

                ${
            lines.length
                ? `
                            <div class="procurement-table-wrapper">

                                <table class="procurement-table">

                                    <thead>
                                        <tr>
                                            <th>Product</th>
                                            <th>Quantity</th>
                                            <th>Estimated Unit Price</th>
                                        </tr>
                                    </thead>

                                    <tbody>
                                        ${lines.map(line => `
                                            <tr>

                                                <td>
                                                    ${escapeHtml(
                    line.productName ||
                    line.productDescription ||
                    line.productId ||
                    '-'
                )}
                                                </td>

                                                <td>
                                                    ${escapeHtml(line.quantity ?? '-')}
                                                </td>

                                                <td>
                                                    ${formatAmount(
                    line.estimatedUnitPrice,
                    line.currencyCode || request.currencyCode
                )}
                                                </td>

                                            </tr>
                                        `).join('')}
                                    </tbody>

                                </table>

                            </div>
                        `
                : '<p>No product lines available.</p>'
        }

                <div id="approval-history-area"></div>

                ${
            isPendingStatus(request.requestStatus)
                ? `
                            <div style="display:flex; gap:12px; margin-top:20px;">

                                <button
                                    id="approve-request"
                                    class="procurement-action-btn">

                                    Approve Request

                                </button>

                                <button
                                    id="reject-request"
                                    class="procurement-back-btn">

                                    Reject Request

                                </button>

                            </div>
                        `
                : `
                            <p>
                                This request is not currently awaiting approval.
                            </p>
                        `
        }

                <button
                    class="procurement-back-btn"
                    id="back-to-pending"
                    style="margin-top:20px;">

                    Back to Pending Requests

                </button>

                <div id="decision-message"></div>

            </div>
        `;

        document.querySelector('#back-to-pending')
            .addEventListener('click', () => {
                loadApprovalPage('pending');
            });

        if (isPendingStatus(request.requestStatus)) {

            document.querySelector('#approve-request')
                .addEventListener('click', () => {
                    approvePurchaseRequest(requestId);
                });

            document.querySelector('#reject-request')
                .addEventListener('click', () => {
                    showRejectForm(requestId);
                });

        }

        loadRequestHistory(requestId);

    } catch (error) {

        console.error('Request details error:', error);

        content.innerHTML = `
            <div class="procurement-panel">
                <p>Unable to load purchase request details.</p>

                <button
                    class="procurement-back-btn"
                    id="back-to-pending">

                    Back

                </button>
            </div>
        `;

        document.querySelector('#back-to-pending')
            .addEventListener('click', () => {
                loadApprovalPage('pending');
            });

    }
}

// -----------------------------
// APPROVE REQUEST
// -----------------------------

async function approvePurchaseRequest(requestId) {

    const button = document.querySelector('#approve-request');
    const message = document.querySelector('#decision-message');

    if (!confirm('Are you sure you want to approve this purchase request?')) {
        return;
    }

    try {

        button.disabled = true;
        button.textContent = 'Approving...';

        await spoApi.post(
            `/api/v1/spo/purchase-requests/${requestId}/approve`
        );

        message.style.color = 'green';
        message.textContent = 'Purchase request approved successfully.';

        setTimeout(() => {
            loadApprovalPage('pending');
        }, 800);

    } catch (error) {

        console.error('Approval failed:', error);
        console.log('Backend response:', error.response?.data);

        message.style.color = 'red';

        message.textContent =
            error.response?.data?.message ||
            error.response?.data?.detail ||
            'Unable to approve purchase request.';

        button.disabled = false;
        button.textContent = 'Approve Request';

    }
}

// -----------------------------
// REJECT REQUEST
// -----------------------------

function showRejectForm(requestId) {

    const message = document.querySelector('#decision-message');

    message.innerHTML = `
        <div style="margin-top:20px;">

            <h3>Reject Purchase Request</h3>

            <label for="rejection-reason">
                Reason for rejection
            </label>

            <textarea
                id="rejection-reason"
                rows="4"
                maxlength="1000"
                placeholder="Enter rejection reason..."
                style="width:100%; margin-top:8px;">
            </textarea>

            <button
                id="confirm-rejection"
                class="procurement-action-btn"
                style="margin-top:12px;">

                Confirm Rejection

            </button>

        </div>
    `;

    document.querySelector('#confirm-rejection')
        .addEventListener('click', () => {
            rejectPurchaseRequest(requestId);
        });

}

async function rejectPurchaseRequest(requestId) {

    const reason = document.querySelector('#rejection-reason').value.trim();

    const message = document.querySelector('#decision-message');

    if (!reason) {

        message.style.color = 'red';
        message.textContent = 'Please enter a rejection reason.';

        return;
    }

    const button = document.querySelector('#confirm-rejection');

    try {

        button.disabled = true;
        button.textContent = 'Rejecting...';

        // Existing backend endpoint.
        // Confirm the exact request body field against the DTO.
        await spoApi.post(
            `/api/v1/spo/purchase-requests/${requestId}/reject`,
            { reason }
        );

        message.style.color = 'green';
        message.textContent = 'Purchase request rejected successfully.';

        setTimeout(() => {
            loadApprovalPage('pending');
        }, 800);

    } catch (error) {

        console.error('Rejection failed:', error);
        console.log('Backend response:', error.response?.data);

        message.style.color = 'red';

        message.textContent =
            error.response?.data?.message ||
            error.response?.data?.detail ||
            'Unable to reject purchase request.';

        button.disabled = false;
        button.textContent = 'Confirm Rejection';

    }
}

// -----------------------------
// APPROVAL HISTORY
// -----------------------------

async function loadRequestHistory(requestId) {

    const container = document.querySelector('#approval-history-area');

    if (!container) return;

    try {

        const history = await getPurchaseRequestHistory(requestId);

        const entries = Array.isArray(history) ? history : [];

        container.innerHTML = `
            <h3 style="margin-top:25px;">Request History</h3>

            ${
            entries.length
                ? entries.map(item => `
                        <div class="procurement-list-row">

                            <span>
                                ${escapeHtml(
                    item.toStatus ||
                    item.action ||
                    item.eventType ||
                    'Request updated'
                )}
                            </span>

                            <span>
                                ${escapeHtml(
                    item.createdAt ||
                    item.timestamp ||
                    ''
                )}
                            </span>

                        </div>
                    `).join('')
                : '<p>No history entries available.</p>'
        }
        `;

    } catch (error) {

        console.error('Approval history error:', error);

        container.innerHTML =
            '<p>Request history is currently unavailable.</p>';

    }
}

// -----------------------------
// APPROVAL HISTORY PAGE
// -----------------------------

async function renderApprovalHistory(content) {

    content.innerHTML = `
        <div class="procurement-panel">

            <h3>Approval History</h3>

            <div id="all-approval-history">
                Loading history...
            </div>

        </div>
    `;

    try {

        const requests = await loadLatestRequests();

        const container = document.querySelector('#all-approval-history');

        if (!requests.length) {

            container.innerHTML = '<p>No purchase request history found.</p>';

            return;
        }

        container.innerHTML = `
            <div class="procurement-table-wrapper">

                <table class="procurement-table">

                    <thead>
                        <tr>
                            <th>Request Number</th>
                            <th>Title</th>
                            <th>Status</th>
                            <th>Action</th>
                        </tr>
                    </thead>

                    <tbody>
                        ${requests.map(request => `
                            <tr>

                                <td>
                                    ${escapeHtml(request.requestNumber || '-')}
                                </td>

                                <td>
                                    ${escapeHtml(request.title || '-')}
                                </td>

                                <td>
                                    ${escapeHtml(request.requestStatus || '-')}
                                </td>

                                <td>
                                    <button
                                        class="procurement-action-btn"
                                        data-history="${escapeHtml(request.sysId)}">

                                        View History

                                    </button>
                                </td>

                            </tr>
                        `).join('')}
                    </tbody>

                </table>

            </div>

            <div id="selected-history"></div>
        `;

        container.querySelectorAll('[data-history]')
            .forEach(button => {

                button.addEventListener('click', async () => {

                    const historyArea = document.querySelector('#selected-history');

                    historyArea.innerHTML = '<p>Loading history...</p>';

                    try {

                        const history = await getPurchaseRequestHistory(
                            button.dataset.history
                        );

                        const entries = Array.isArray(history) ? history : [];

                        historyArea.innerHTML = `
                            <h3 style="margin-top:20px;">
                                Request History
                            </h3>

                            ${
                            entries.length
                                ? entries.map(item => `
                                        <div class="procurement-list-row">
                                            <span>
                                                ${escapeHtml(
                                    item.toStatus ||
                                    item.action ||
                                    item.eventType ||
                                    'Request updated'
                                )}
                                            </span>

                                            <span>
                                                ${escapeHtml(
                                    item.createdAt ||
                                    item.timestamp ||
                                    ''
                                )}
                                            </span>
                                        </div>
                                    `).join('')
                                : '<p>No history entries found.</p>'
                        }
                        `;

                    } catch (error) {

                        console.error('History loading failed:', error);

                        historyArea.innerHTML =
                            '<p>Unable to load request history.</p>';

                    }

                });

            });

    } catch (error) {

        console.error('Approval history page failed:', error);

        document.querySelector('#all-approval-history').innerHTML =
            '<p>Unable to load approval history.</p>';

    }
}

// -----------------------------
// UTILITIES
// -----------------------------

function formatAmount(amount, currency) {

    if (amount === null || amount === undefined) {
        return '-';
    }

    return `${escapeHtml(currency || '')} ${escapeHtml(amount)}`.trim();

}

function escapeHtml(value) {

    return String(value ?? '').replace(/[&<>"']/g, character => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[character]);

}