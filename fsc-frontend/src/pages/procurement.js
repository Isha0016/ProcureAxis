import spoApi from '../api/spoAxios.js';

import {
    createPurchaseRequest,
    getPurchaseRequest,
    submitPurchaseRequest,
    getPurchaseRequestHistory
} from '../api/purchaseRequests.api.js';

import { browseActiveProducts } from '../api/products.api.js';

let sourcingRequests = [];

const SUBMITTED_REQUESTS_KEY = 'submittedPurchaseRequests';

// ---------------- SUBMITTED REQUEST STORAGE ----------------

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

function saveSubmittedRequest(request) {
    const requests = getSubmittedRequests();

    const existingIndex = requests.findIndex(
        item => item.sysId === request.sysId
    );

    const savedRequest = {
        sysId: request.sysId,
        requestNumber: request.requestNumber,
        requestStatus: request.requestStatus
    };

    if (existingIndex >= 0) {
        requests[existingIndex] = savedRequest;
    } else {
        requests.push(savedRequest);
    }

    localStorage.setItem(
        SUBMITTED_REQUESTS_KEY,
        JSON.stringify(requests)
    );
}

// ---------------- PROCUREMENT USER DETAILS ----------------

function getProcurementUserDetails() {
    const token = localStorage.getItem('token');

    if (!token) {
        return {
            name: 'Procurement User',
            userId: 'Not available',
            email: 'Not available'
        };
    }

    try {
        const base64 = token.split('.')[1]
            .replace(/-/g, '+')
            .replace(/_/g, '/');

        const paddedBase64 = base64.padEnd(
            base64.length + (4 - base64.length % 4) % 4,
            '='
        );

        const payload = JSON.parse(atob(paddedBase64));

        console.log('Procurement User JWT Payload:', payload);

        return {
            name:
                payload.name ||
                payload.fullName ||
                payload.username ||
                payload.preferred_username ||
                'Procurement User',

            userId:
                payload.userId ||
                payload.user_id ||
                payload.employeeId ||
                payload.id ||
                payload.sub ||
                'Not available',

            email:
                payload.email ||
                payload.emailAddress ||
                payload.userEmail ||
                'Not available'
        };

    } catch (error) {
        console.error('Unable to read procurement user details:', error);

        return {
            name: 'Procurement User',
            userId: 'Not available',
            email: 'Not available'
        };
    }
}

// ---------------- MAIN DASHBOARD ----------------

export function renderProcurementDashboard() {
    document.querySelector('#app').innerHTML = `
        <div class="procurement-layout">

            <aside class="procurement-sidebar">
                <div class="procurement-brand">
                    <h2>ProcureAxis</h2>
                    <span>PROCUREMENT PORTAL</span>
                </div>

                <nav class="procurement-nav">
                    <button class="active" data-page="overview">Overview</button>
                    <button data-page="purchase-requests">Purchase Requests</button>
                    <button data-page="sourcing">Sourcing Opportunities</button>
                    <button data-page="bids">Bid Comparison</button>
                    <button data-page="awards">Awards</button>
                </nav>
            </aside>

            <main class="procurement-main">

                <header class="procurement-header">

                    <div>
                        <h2 id="procurement-page-title">
                            Procurement Dashboard
                        </h2>
                        <p>Manage your procurement activities</p>
                    </div>

                    <div class="user-menu-container">

                        <button
                            type="button"
                            id="procurementAccountButton"
                            class="user-info">

                            <span id="procurementDisplayName">
                                Loading...
                            </span>

                            <div
                                class="user-avatar"
                                id="procurementAvatar">
                                P
                            </div>

                        </button>

                        <div
                            id="procurementAccountDropdown"
                            class="account-dropdown"
                            hidden>

                            <h3 id="dropdownUserName">
                                Loading...
                            </h3>

                            <p id="dropdownUserId"></p>

                            <p id="dropdownUserEmail"></p>

                            <button
                                type="button"
                                id="dropdownLogoutButton">
                                Sign Out
                            </button>

                        </div>

                    </div>

                </header>

                <section id="procurement-content">
                    <div class="procurement-loading">
                        Loading dashboard...
                    </div>
                </section>

            </main>

        </div>
    `;

    // Load procurement user details
    const user = getProcurementUserDetails();

    document.querySelector('#procurementDisplayName').textContent =
        user.name;

    document.querySelector('#procurementAvatar').textContent =
        user.name.charAt(0).toUpperCase();

    document.querySelector('#dropdownUserName').textContent =
        user.name;

    document.querySelector('#dropdownUserId').textContent =
        `User ID: ${user.userId}`;

    document.querySelector('#dropdownUserEmail').textContent =
        `Email: ${user.email}`;

    // Account dropdown
    const accountButton = document.querySelector(
        '#procurementAccountButton'
    );

    const accountDropdown = document.querySelector(
        '#procurementAccountDropdown'
    );

    accountButton.addEventListener('click', (event) => {
        event.stopPropagation();

        accountDropdown.hidden = !accountDropdown.hidden;
    });

    document.addEventListener('click', (event) => {
        if (!event.target.closest('.user-menu-container')) {
            accountDropdown.hidden = true;
        }
    });

    // Sign Out
    document.querySelector('#dropdownLogoutButton')
        .addEventListener('click', () => {
            localStorage.removeItem('token');

            // Return to ProcureAxis landing page.
            // Other saved data is preserved.
            window.location.reload();
        });

    // Procurement navigation
    document.querySelectorAll('.procurement-nav button')
        .forEach(button => {
            button.addEventListener('click', () => {
                document.querySelectorAll('.procurement-nav button')
                    .forEach(item => item.classList.remove('active'));

                button.classList.add('active');

                loadProcurementPage(button.dataset.page);
            });
        });

    loadProcurementPage('overview');
}

async function loadProcurementPage(page) {
    const content = document.querySelector('#procurement-content');
    const title = document.querySelector('#procurement-page-title');

    const titles = {
        overview: 'Procurement Dashboard',
        'purchase-requests': 'Purchase Requests',
        sourcing: 'Sourcing Opportunities',
        bids: 'Bid Comparison',
        awards: 'Awards'
    };

    title.textContent = titles[page] || 'Procurement Dashboard';

    if (page === 'overview') {
        await renderOverview(content);
    } else if (page === 'purchase-requests') {
        renderPurchaseRequests(content);
    } else if (page === 'sourcing') {
        await renderSourcingRequests(content);
    } else if (page === 'bids') {
        renderBidComparison(content);
    } else if (page === 'awards') {
        renderAwards(content);
    }
}

// ---------------- SOURCING REQUESTS ----------------

async function getSourcingRequests() {
    const response = await spoApi.get(
        '/api/v1/spo/sourcing-requests'
    );

    return response.data;
}

async function renderOverview(content) {
    content.innerHTML = `
        <div class="procurement-welcome">
            <h2>Welcome to Procurement</h2>
            <p>Monitor purchase requests, sourcing activities and supplier bids.</p>
        </div>

        <div class="procurement-stats">
            <div class="procurement-stat-card">
                <span>Total Sourcing Requests</span>
                <h2 id="total-sourcing">--</h2>
            </div>

            <div class="procurement-stat-card">
                <span>Draft Requests</span>
                <h2 id="draft-sourcing">--</h2>
            </div>

            <div class="procurement-stat-card">
                <span>Published Requests</span>
                <h2 id="published-sourcing">--</h2>
            </div>

            <div class="procurement-stat-card">
                <span>Closed Requests</span>
                <h2 id="closed-sourcing">--</h2>
            </div>
        </div>

        <div class="procurement-panel">
            <h3>Recent Sourcing Requests</h3>
            <div id="recent-sourcing">Loading...</div>
        </div>
    `;

    try {
        sourcingRequests = await getSourcingRequests();

        const requests = Array.isArray(sourcingRequests)
            ? sourcingRequests
            : [];

        document.querySelector('#total-sourcing').textContent =
            requests.length;

        document.querySelector('#draft-sourcing').textContent =
            requests.filter(r => r.requestStatus === 'DRAFT').length;

        document.querySelector('#published-sourcing').textContent =
            requests.filter(r => r.requestStatus === 'PUBLISHED').length;

        document.querySelector('#closed-sourcing').textContent =
            requests.filter(r => r.requestStatus === 'CLOSED').length;

        const recent = requests.slice(0, 5);

        document.querySelector('#recent-sourcing').innerHTML =
            recent.length
                ? recent.map(request => `
                    <div class="procurement-list-row">
                        <div>
                            <strong>${escapeHtml(request.requestNumber || '-')}</strong>
                            <p>${escapeHtml(request.title || '-')}</p>
                        </div>
                        <span class="procurement-status">
                            ${escapeHtml(request.requestStatus || '-')}
                        </span>
                    </div>
                `).join('')
                : '<p>No sourcing requests found.</p>';

    } catch (error) {
        console.error('Dashboard loading failed:', error);

        document.querySelector('#recent-sourcing').innerHTML =
            '<p>Unable to load sourcing requests. Please try again.</p>';
    }
}

// ---------------- PURCHASE REQUESTS ----------------

function renderPurchaseRequests(content) {
    content.innerHTML = `
        <div class="procurement-panel">
            <div style="display:flex; justify-content:space-between; align-items:center;">
                <div>
                    <h3>Purchase Requests</h3>
                    <p>Create and manage your purchase requests.</p>
                </div>

                <button id="show-create-pr" class="procurement-action-btn">
                    + Create Purchase Request
                </button>
            </div>

            <div id="purchase-request-area">
                <p>Select Create Purchase Request to get started.</p>
            </div>
        </div>

        <div class="procurement-panel" style="margin-top:20px;">
            <h3>Submitted Requests</h3>
            <p>Purchase requests submitted for approval.</p>
            <div id="submitted-requests-table"></div>
        </div>
    `;

    document.querySelector('#show-create-pr')
        .addEventListener('click', () => {
            renderCreatePurchaseRequest(
                document.querySelector('#purchase-request-area')
            );
        });

    renderSubmittedRequests();
}

async function renderSubmittedRequests() {
    const container = document.querySelector('#submitted-requests-table');

    if (!container) return;

    const requests = getSubmittedRequests();

    if (!requests.length) {
        container.innerHTML = '<p>No submitted purchase requests found.</p>';
        return;
    }

    container.innerHTML = '<p>Loading submitted requests...</p>';

    const latestRequests = await Promise.all(
        requests.map(async savedRequest => {
            try {
                const latest = await getPurchaseRequest(savedRequest.sysId);

                saveSubmittedRequest(latest);

                return latest;
            } catch (error) {
                console.error(
                    'Unable to refresh purchase request:',
                    savedRequest.sysId,
                    error
                );

                return savedRequest;
            }
        })
    );

    container.innerHTML = `
        <div class="procurement-table-wrapper">
            <table class="procurement-table">
                <thead>
                    <tr>
                        <th>Request Number</th>
                        <th>Request ID</th>
                        <th>Status</th>
                        <th>Action</th>
                    </tr>
                </thead>

                <tbody>
                    ${latestRequests.map(request => {
        const status = request.requestStatus || 'UNKNOWN';

        return `
                            <tr>
                                <td>${escapeHtml(request.requestNumber || '-')}</td>
                                <td>${escapeHtml(request.sysId || '-')}</td>
                                <td>${escapeHtml(status)}</td>
                                <td>
                                    <button
                                        class="procurement-action-btn"
                                        data-view-pr="${escapeHtml(request.sysId)}">
                                        View Details
                                    </button>

                                    ${
            status === 'APPROVED'
                ? `
                                                <button
                                                    class="procurement-action-btn"
                                                    data-create-sourcing="${escapeHtml(request.sysId)}">
                                                    Create Sourcing Request
                                                </button>
                                            `
                : ''
        }
                                </td>
                            </tr>
                        `;
    }).join('')}
                </tbody>
            </table>
        </div>
    `;

    container.querySelectorAll('[data-view-pr]')
        .forEach(button => {
            button.addEventListener('click', () => {
                renderPurchaseRequestDetails(button.dataset.viewPr);
            });
        });

    container.querySelectorAll('[data-create-sourcing]')
        .forEach(button => {
            button.addEventListener('click', () => {
                createSourcingRequestFromPR(
                    button.dataset.createSourcing,
                    button
                );
            });
        });
}

// ---------------- CREATE SOURCING REQUEST ----------------

async function createSourcingRequestFromPR(purchaseRequestId, button) {
    button.disabled = true;
    button.textContent = 'Creating...';

    console.log('Creating Sourcing Request');
    console.log('Purchase Request ID:', purchaseRequestId);

    let response;

    try {
        response = await spoApi.post(
            `/api/v1/spo/sourcing-requests/from-purchase-request/${purchaseRequestId}`
        );

        console.log('Sourcing Request Creation Successful');
        console.log('HTTP Status:', response.status);
        console.log('Backend Response:', response.data);

    } catch (error) {
        console.error('Sourcing Request POST Failed:', error);
        console.log('HTTP Status:', error.response?.status);
        console.log('Backend Response:', error.response?.data);
        console.log(
            'Request URL:',
            error.config?.baseURL + error.config?.url
        );

        if (error.response?.status === 408) {
            alert(
                'The creation request timed out. The backend may still have processed it. Please check Sourcing Opportunities before trying again.'
            );
        } else {
            alert(
                error.response?.data?.message ||
                error.response?.data?.detail ||
                'Unable to create sourcing request.'
            );
        }

        button.disabled = false;
        button.textContent = 'Create Sourcing Request';
        return;
    }

    alert('Sourcing Request created successfully!');

    button.textContent = 'Created';

    await loadProcurementPage('sourcing');
}

// ---------------- PURCHASE REQUEST DETAILS ----------------

async function renderPurchaseRequestDetails(requestId) {
    const content = document.querySelector('#procurement-content');

    content.innerHTML = `
        <div class="procurement-panel">
            <h3>Purchase Request Details</h3>
            <p>Loading request details...</p>
        </div>
    `;

    try {
        const request = await getPurchaseRequest(requestId);

        const lines = request.requestLines || [];

        content.innerHTML = `
            <div class="procurement-panel">
                <h3>Purchase Request Details</h3>

                <p><strong>Request Number:</strong>
                    ${escapeHtml(request.requestNumber || '-')}
                </p>

                <p><strong>Request ID:</strong>
                    ${escapeHtml(request.sysId || requestId)}
                </p>

                <p><strong>Title:</strong>
                    ${escapeHtml(request.title || '-')}
                </p>

                <p><strong>Description:</strong>
                    ${escapeHtml(request.description || '-')}
                </p>

                <p><strong>Status:</strong>
                    ${escapeHtml(request.requestStatus || '-')}
                </p>

                <p><strong>Priority:</strong>
                    ${escapeHtml(request.priority || '-')}
                </p>

                <p><strong>Required Date:</strong>
                    ${escapeHtml(request.requiredDate || '-')}
                </p>

                <p><strong>Delivery Location:</strong>
                    ${escapeHtml(request.deliveryLocation || '-')}
                </p>

                <p><strong>Estimated Total:</strong>
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
                                                <td>${escapeHtml(
                    line.productName ||
                    line.productDescription ||
                    line.productId ||
                    '-'
                )}</td>
                                                <td>${escapeHtml(line.quantity ?? '-')}</td>
                                                <td>${formatAmount(
                    line.estimatedUnitPrice,
                    line.currencyCode || request.currencyCode
                )}</td>
                                            </tr>
                                        `).join('')}
                                    </tbody>
                                </table>
                            </div>
                        `
                : '<p>No product lines available.</p>'
        }

                <div id="pr-history-area"></div>

                <button class="procurement-back-btn" id="back-to-purchase-requests">
                    Back to Purchase Requests
                </button>
            </div>
        `;

        document.querySelector('#back-to-purchase-requests')
            .addEventListener('click', () => {
                loadProcurementPage('purchase-requests');
            });

        loadPurchaseRequestHistory(requestId);

    } catch (error) {
        console.error('Purchase request details failed:', error);

        content.innerHTML = `
            <div class="procurement-panel">
                <p>Unable to load purchase request details.</p>
                <button class="procurement-back-btn" id="back-to-purchase-requests">
                    Back
                </button>
            </div>
        `;

        document.querySelector('#back-to-purchase-requests')
            .addEventListener('click', () => {
                loadProcurementPage('purchase-requests');
            });
    }
}

async function loadPurchaseRequestHistory(requestId) {
    const container = document.querySelector('#pr-history-area');

    if (!container) return;

    try {
        const history = await getPurchaseRequestHistory(requestId);

        const entries = Array.isArray(history)
            ? history
            : [];

        container.innerHTML = `
            <h3>Request History</h3>
            ${
            entries.length
                ? entries.map(item => `
                        <div class="procurement-list-row">
                            <span>${escapeHtml(
                    item.toStatus ||
                    item.action ||
                    item.eventType ||
                    'Request updated'
                )}</span>
                            <span>${escapeHtml(
                    item.createdAt ||
                    item.timestamp ||
                    ''
                )}</span>
                        </div>
                    `).join('')
                : '<p>No history entries available.</p>'
        }
        `;

    } catch (error) {
        console.error('Request history loading failed:', error);
        container.innerHTML = '<p>Request history is currently unavailable.</p>';
    }
}

// ---------------- CREATE PURCHASE REQUEST ----------------

async function renderCreatePurchaseRequest(area) {
    area.innerHTML = '<p>Loading active products...</p>';

    try {
        const result = await browseActiveProducts(0, 50);
        const products = result.content || [];

        if (!products.length) {
            area.innerHTML = '<p>No active supplier products are available.</p>';
            return;
        }

        const productOptions = products.map(product => `
            <option value="${escapeHtml(product.sysId)}">
                ${escapeHtml(product.productName)}
                (${escapeHtml(product.productCode || '-')})
                - ${escapeHtml(product.supplierCompanyName || 'Supplier')}
            </option>
        `).join('');

        area.innerHTML = `
            <form id="purchase-request-form">
                <h3>Create Purchase Request</h3>

                <div class="procurement-form-grid">
                    <label>
                        Request Title *
                        <input name="title" required maxlength="200"
                               placeholder="Enter request title">
                    </label>

                    <label>
                        Priority *
                        <select name="priority" required>
                            <option value="LOW">Low</option>
                            <option value="MEDIUM" selected>Medium</option>
                            <option value="HIGH">High</option>
                            <option value="URGENT">Urgent</option>
                        </select>
                    </label>

                    <label>
                        Required Date *
                        <input type="date" name="requiredDate" required>
                    </label>

                    <label>
                        Delivery Location *
                        <input name="deliveryLocation" required maxlength="255"
                               placeholder="Enter delivery location">
                    </label>

                    <label>
                        Currency *
                        <select name="currencyCode" required>
                            <option value="INR" selected>INR</option>
                            <option value="USD">USD</option>
                            <option value="EUR">EUR</option>
                        </select>
                    </label>

                    <label>
                        Budget Code
                        <input name="budgetCode" placeholder="Optional">
                    </label>
                </div>

                <label>
                    Description
                    <textarea name="description" maxlength="2000"
                              placeholder="Describe your purchase requirement"></textarea>
                </label>

                <h3>Requested Products</h3>

                <div id="purchase-request-lines"></div>

                <button type="button" id="add-pr-line"
                        class="procurement-back-btn">
                    + Add Product
                </button>

                <div style="margin-top:20px;">
                    <button type="submit" class="procurement-action-btn">
                        Save Purchase Request
                    </button>

                    <button type="button" id="cancel-pr"
                            class="procurement-back-btn">
                        Cancel
                    </button>
                </div>

                <div id="pr-message"></div>
            </form>
        `;

        const lines = document.querySelector('#purchase-request-lines');

        function addProductRow() {
            const row = document.createElement('div');
            row.className = 'procurement-product-line';

            row.innerHTML = `
                <label>
                    Product *
                    <select class="pr-product" required>
                        <option value="">Select Product</option>
                        ${productOptions}
                    </select>
                </label>

                <label>
                    Quantity *
                    <input class="pr-quantity" type="number"
                           min="0.0001" step="0.0001" required>
                </label>

                <label>
                    Estimated Unit Price *
                    <input class="pr-price" type="number"
                           min="0" step="0.0001" required>
                </label>

                <label>
                    Notes
                    <input class="pr-notes" maxlength="1000"
                           placeholder="Optional">
                </label>

                <button type="button" class="remove-pr-line">
                    Remove
                </button>
            `;

            row.querySelector('.remove-pr-line')
                .addEventListener('click', () => row.remove());

            lines.appendChild(row);
        }

        addProductRow();

        document.querySelector('#add-pr-line')
            .addEventListener('click', addProductRow);

        document.querySelector('#cancel-pr')
            .addEventListener('click', () => {
                loadProcurementPage('purchase-requests');
            });

        document.querySelector('#purchase-request-form')
            .addEventListener('submit', async event => {
                event.preventDefault();

                const message = document.querySelector('#pr-message');
                const form = event.currentTarget;
                const formData = new FormData(form);

                const productRows = [
                    ...lines.querySelectorAll('.procurement-product-line')
                ];

                if (!productRows.length) {
                    message.textContent = 'Please add at least one product.';
                    return;
                }

                const requestLines = productRows.map(row => ({
                    productId: row.querySelector('.pr-product').value,
                    quantity: Number(row.querySelector('.pr-quantity').value),
                    estimatedUnitPrice: Number(row.querySelector('.pr-price').value),
                    notes: row.querySelector('.pr-notes').value
                }));

                if (requestLines.some(line =>
                    !line.productId ||
                    !Number.isFinite(line.quantity) ||
                    line.quantity <= 0 ||
                    !Number.isFinite(line.estimatedUnitPrice) ||
                    line.estimatedUnitPrice < 0
                )) {
                    message.textContent =
                        'Please enter valid product, quantity and price details.';
                    return;
                }

                const payload = {
                    title: formData.get('title'),
                    description: formData.get('description'),
                    priority: formData.get('priority'),
                    requiredDate: formData.get('requiredDate'),
                    deliveryLocation: formData.get('deliveryLocation'),
                    currencyCode: formData.get('currencyCode'),
                    budgetCode: formData.get('budgetCode') || null,
                    requestLines
                };

                console.log('Purchase Request Payload:', payload);

                try {
                    message.textContent = 'Saving purchase request...';

                    const created = await createPurchaseRequest(payload);

                    message.innerHTML = `
                        <p style="color:green;">
                            Purchase request saved successfully as a draft.
                        </p>

                        <p>Request Number:
                            ${escapeHtml(created.requestNumber || '-')}
                        </p>

                        <p>Request ID:
                            ${escapeHtml(created.sysId || '-')}
                        </p>

                        <button type="button" id="submit-created-pr"
                                class="procurement-action-btn">
                            Submit for Approval
                        </button>
                    `;

                    document.querySelector('#submit-created-pr')
                        .addEventListener('click', async event => {
                            const button = event.currentTarget;
                            button.disabled = true;
                            button.textContent = 'Submitting...';

                            try {
                                await submitPurchaseRequest(created.sysId);

                                let updatedRequest = created;

                                try {
                                    updatedRequest = await getPurchaseRequest(
                                        created.sysId
                                    );
                                } catch (refreshError) {
                                    console.warn(
                                        'Could not refresh request details:',
                                        refreshError
                                    );

                                    updatedRequest = {
                                        ...created,
                                        requestStatus: 'SUBMITTED'
                                    };
                                }

                                saveSubmittedRequest(updatedRequest);

                                message.innerHTML = `
                                    <p style="color:green;">
                                        Purchase request submitted successfully.
                                    </p>

                                    <p>Request Number:
                                        ${escapeHtml(updatedRequest.requestNumber || created.requestNumber || '-')}
                                    </p>

                                    <p>Request ID:
                                        ${escapeHtml(updatedRequest.sysId || created.sysId)}
                                    </p>

                                    <p>Status:
                                        ${escapeHtml(updatedRequest.requestStatus || 'SUBMITTED')}
                                    </p>

                                    <p>
                                        Your request is now available in the
                                        Submitted Requests section.
                                    </p>

                                    <button type="button"
                                            id="view-submitted-requests"
                                            class="procurement-action-btn">
                                        View Submitted Requests
                                    </button>
                                `;

                                document.querySelector('#view-submitted-requests')
                                    .addEventListener('click', () => {
                                        loadProcurementPage('purchase-requests');
                                    });

                            } catch (error) {
                                console.error('Submission failed:', error);

                                message.textContent =
                                    error.response?.data?.message ||
                                    error.response?.data?.detail ||
                                    'Unable to submit purchase request.';

                                button.disabled = false;
                                button.textContent = 'Submit for Approval';
                            }
                        });

                } catch (error) {
                    console.error('Purchase Request Error:', error);

                    console.log(
                        'Request URL:',
                        error.config?.baseURL + error.config?.url
                    );

                    console.log('HTTP Status:', error.response?.status);
                    console.log('Backend Response:', error.response?.data);

                    message.textContent =
                        error.response?.data?.message ||
                        error.response?.data?.detail ||
                        error.message ||
                        'Unable to create purchase request.';
                }
            });

    } catch (error) {
        console.error('Product loading failed:', error);

        area.innerHTML = `
            <p>Unable to load active products.</p>
            <button id="retry-products" class="procurement-back-btn">
                Retry
            </button>
        `;

        document.querySelector('#retry-products')
            .addEventListener('click', () => {
                renderCreatePurchaseRequest(area);
            });
    }
}

// ---------------- SOURCING OPPORTUNITIES ----------------

async function renderSourcingRequests(content) {
    content.innerHTML = `
        <div class="procurement-panel">
            <h3>All Sourcing Requests</h3>
            <div id="sourcing-table">Loading sourcing requests...</div>
        </div>
    `;

    try {
        sourcingRequests = await getSourcingRequests();

        const requests = Array.isArray(sourcingRequests)
            ? sourcingRequests
            : [];

        const table = document.querySelector('#sourcing-table');

        if (!requests.length) {
            table.innerHTML = '<p>No sourcing requests found.</p>';
            return;
        }

        table.innerHTML = `
            <div class="procurement-table-wrapper">
                <table class="procurement-table">
                    <thead>
                        <tr>
                            <th>Request Number</th>
                            <th>Title</th>
                            <th>Status</th>
                            <th>Required Date</th>
                            <th>Estimated Amount</th>
                            <th>Action</th>
                        </tr>
                    </thead>

                    <tbody>
                        ${requests.map(request => `
                            <tr>
                                <td>${escapeHtml(request.requestNumber || '-')}</td>
                                <td>${escapeHtml(request.title || '-')}</td>
                                <td>${escapeHtml(request.requestStatus || '-')}</td>
                                <td>${escapeHtml(request.requiredDate || '-')}</td>
                                <td>${formatAmount(
            request.estimatedTotalAmount,
            request.currencyCode
        )}</td>
                                <td>
                                    <button
                                        class="procurement-action-btn"
                                        data-compare="${escapeHtml(request.sysId)}">
                                        Compare Bids
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;

        table.querySelectorAll('[data-compare]')
            .forEach(button => {
                button.addEventListener('click', () => {
                    loadBidComparison(button.dataset.compare);
                });
            });

    } catch (error) {
        console.error('Sourcing requests failed:', error);

        document.querySelector('#sourcing-table').innerHTML =
            '<p>Unable to load sourcing requests.</p>';
    }
}

// ---------------- BID COMPARISON ----------------

function renderBidComparison(content) {
    content.innerHTML = `
        <div class="procurement-panel">
            <h3>Bid Comparison</h3>
            <p>
                Open Sourcing Opportunities and select Compare Bids
                for a sourcing request.
            </p>
        </div>
    `;
}

async function loadBidComparison(sourcingRequestId) {
    const content = document.querySelector('#procurement-content');

    content.innerHTML = `
        <div class="procurement-panel">
            <h3>Supplier Bid Comparison</h3>
            <p>Loading supplier bids...</p>
        </div>
    `;

    try {
        const response = await spoApi.get(
            `/api/v1/spo/sourcing-requests/${sourcingRequestId}/comparison`
        );

        console.log('Bid Comparison Response:', response.data);

        const data = response.data;

        const bids = Array.isArray(data)
            ? data
            : data?.content || data?.bids || [];

        content.innerHTML = `
            <div class="procurement-panel">
                <h3>Supplier Bid Comparison</h3>
                <p>Review supplier quotations before making an award decision.</p>

                ${
            bids.length
                ? `
                            <div class="procurement-table-wrapper">
                                <table class="procurement-table">
                                    <thead>
                                        <tr>
                                            <th>Select</th>
                                            <th>Bid Number</th>
                                            <th>Supplier Company</th>
                                            <th>Bid Status</th>
                                            <th>Total Bid Amount</th>
                                            <th>Delivery Lead Time</th>
                                        </tr>
                                    </thead>

                                    <tbody>
                                        ${bids.map(bid => `
                                            <tr>
                                                <td>
                                                    <input
                                                        type="radio"
                                                        name="selected-bid"
                                                        value="${escapeHtml(bid.sysId)}"
                                                    >
                                                </td>

                                                <td>${escapeHtml(bid.bidNumber || '-')}</td>

                                                <td>${escapeHtml(bid.supplierCompanyName || '-')}</td>

                                                <td>${escapeHtml(bid.bidStatus || '-')}</td>

                                                <td>${formatAmount(
                    bid.totalBidAmount,
                    bid.currencyCode
                )}</td>

                                                <td>${escapeHtml(
                    bid.deliveryLeadTimeDays ?? '-'
                )} days</td>
                                            </tr>
                                        `).join('')}
                                    </tbody>
                                </table>
                            </div>

                            <button
                                class="procurement-action-btn"
                                id="prepare-award"
                            >
                                Continue to Award
                            </button>
                        `
                : '<p>No bids have been received for this sourcing request.</p>'
        }

                <button
                    class="procurement-back-btn"
                    id="back-to-sourcing"
                >
                    Back to Sourcing
                </button>
            </div>
        `;

        const awardButton = document.querySelector('#prepare-award');

        if (awardButton) {
            awardButton.addEventListener('click', () => {
                const selectedBid = document.querySelector(
                    'input[name="selected-bid"]:checked'
                );

                if (!selectedBid) {
                    alert('Please select a supplier bid first.');
                    return;
                }

                alert(
                    'Bid selected. We will connect this selection to the Award API next.'
                );

                console.log('Selected Bid ID:', selectedBid.value);
                console.log('Sourcing Request ID:', sourcingRequestId);
            });
        }

        document.querySelector('#back-to-sourcing')
            .addEventListener('click', () => {
                loadProcurementPage('sourcing');
            });

    } catch (error) {
        console.error('Bid comparison failed:', error);

        content.innerHTML = `
            <div class="procurement-panel">
                <h3>Unable to Load Bid Comparison</h3>

                <p>
                    ${escapeHtml(
            error.response?.data?.message ||
            error.response?.data?.detail ||
            error.message ||
            'Unknown error'
        )}
                </p>

                <button
                    class="procurement-back-btn"
                    id="back-to-sourcing"
                >
                    Back to Sourcing
                </button>
            </div>
        `;

        document.querySelector('#back-to-sourcing')
            .addEventListener('click', () => {
                loadProcurementPage('sourcing');
            });
    }
}

// ---------------- AWARDS ----------------

async function renderAwards(content) {
    content.innerHTML = `
        <div class="procurement-panel">
            <h3>Sourcing Awards</h3>
            <p>Review award information for your sourcing requests.</p>
            <div id="awards-table">Loading awards...</div>
        </div>
    `;

    const table = document.querySelector('#awards-table');

    try {
        const requests = await getSourcingRequests();

        const sourcingList = Array.isArray(requests)
            ? requests
            : requests?.content || requests?.data || [];

        if (!sourcingList.length) {
            table.innerHTML = '<p>No sourcing requests found.</p>';
            return;
        }

        table.innerHTML = `
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
                        ${sourcingList.map(request => `
                            <tr>
                                <td>${escapeHtml(request.requestNumber || '-')}</td>
                                <td>${escapeHtml(request.title || '-')}</td>
                                <td>${escapeHtml(request.requestStatus || '-')}</td>
                                <td>
                                    <button
                                        class="procurement-action-btn"
                                        data-view-award="${escapeHtml(request.sysId)}">
                                        View Award
                                    </button>
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;

        table.querySelectorAll('[data-view-award]')
            .forEach(button => {
                button.addEventListener('click', () => {
                    loadAwardDetails(button.dataset.viewAward);
                });
            });

    } catch (error) {
        console.error('Awards loading failed:', error);

        table.innerHTML = `
            <p>Unable to load sourcing requests for awards.</p>
        `;
    }
}

async function loadAwardDetails(sourcingRequestId) {
    const content = document.querySelector('#procurement-content');

    content.innerHTML = `
        <div class="procurement-panel">
            <h3>Award Details</h3>
            <p>Loading award information...</p>
        </div>
    `;

    try {
        const response = await spoApi.get(
            `/api/v1/spo/sourcing-requests/${sourcingRequestId}/award`
        );

        console.log('Award API Response:', response.data);

        content.innerHTML = `
            <div class="procurement-panel">
                <h3>Award Details</h3>
                <pre style="white-space:pre-wrap; overflow-wrap:anywhere;">${escapeHtml(
            JSON.stringify(response.data, null, 2)
        )}</pre>

                <button
                    class="procurement-back-btn"
                    id="back-to-awards">
                    Back to Awards
                </button>
            </div>
        `;

    } catch (error) {
        console.error('Award details failed:', error);
        console.log('Award HTTP Status:', error.response?.status);
        console.log('Award Backend Response:', error.response?.data);

        content.innerHTML = `
            <div class="procurement-panel">
                <h3>Award Details</h3>
                <p>
                    ${
            error.response?.status === 404
                ? 'No award has been created for this sourcing request yet.'
                : escapeHtml(
                    error.response?.data?.message ||
                    error.response?.data?.detail ||
                    error.message ||
                    'Unable to load award details.'
                )
        }
                </p>

                <button
                    class="procurement-back-btn"
                    id="back-to-awards">
                    Back to Awards
                </button>
            </div>
        `;
    }

    document.querySelector('#back-to-awards')
        ?.addEventListener('click', () => {
            loadProcurementPage('awards');
        });
}

// ---------------- UTILITIES ----------------

function formatAmount(amount, currency) {
    if (amount === null || amount === undefined) return '-';

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