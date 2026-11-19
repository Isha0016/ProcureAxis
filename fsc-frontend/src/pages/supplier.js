import { getMyProducts } from '../api/products.api.js';
import { getInvitedSourcingOpportunities , getBidsForSourcingRequest } from '../api/sourcing.api.js';
import { getCompanyProfile, updateCompanyProfile } from '../api/company.api.js';

export function renderSupplierDashboard() {
    document.querySelector('#app').innerHTML = `

    <div class="dashboard">

      <aside class="sidebar">
        <div class="sidebar-brand">
          <h2>ProcureAxis</h2>
          <span>SUPPLIER PORTAL</span>
        </div>

        <nav>
          <button class="nav-item active" data-page="overview">
            Overview
          </button>
          <button class="nav-item" data-page="products">
            My Products
          </button>
          <button class="nav-item" data-page="sourcing">
            Sourcing Opportunities
          </button>
          <button class="nav-item" data-page="bids">
            My Bids
          </button>
      
          
          <button class="nav-item" data-page="profile">
            Company Profile
          </button>
        </nav>


      </aside>

      <main class="dashboard-main">

        <header class="dashboard-header">
          <div>
            <h1>Supplier Dashboard</h1>
            <p>Manage your supply chain activities</p>
          </div>

          <div class="user-menu-container">
    <button type="button" id="supplierAccountButton" class="user-info">
        <span id="supplierDisplayName">Loading...</span>
        <div class="user-avatar" id="supplierAvatar">S</div>
    </button>

    <div id="supplierAccountDropdown" class="account-dropdown" hidden>
        <h3 id="dropdownCompanyName">Loading...</h3>
        <p id="dropdownCompanyEmail"></p>
        <p id="dropdownCompanyStatus"></p>

        <button type="button" id="dropdownProfileButton">
            Company Profile
        </button>

        <button type="button" id="dropdownLogoutButton">
            Sign Out
        </button>
    </div>
</div>
        </header>

        <section class="welcome-banner">
          <div>
            <h2>Welcome to ProcureAxis</h2>
            <p>
              Manage your products, discover sourcing opportunities
              and track your bids from one place.
            </p>
          </div>
        </section>

     <section class="dashboard-cards">
    <div class="stat-card">
        <span>My Products</span>
        <h2 id="myProductsCount">--</h2>
        <p>Registered products</p>
    </div>

    <div class="stat-card">
        <span>Sourcing Opportunities</span>
        <h2 id="sourcingCount">--</h2>
        <p>Available opportunities</p>
    </div>

    <div class="stat-card">
        <span>My Bids</span>
        <h2 id="myBidsCount">--</h2>
        <p>Submitted and draft bids</p>
    </div>

    <div class="stat-card">
        <span>Awards</span>
        <h2 id="awardsCount">--</h2>
        <p>Procurement awards</p>
    </div>
    </section>
        <section class="activity-section">
          <h2>Supplier Workspace</h2>
          <p>
            Select a module from the sidebar to manage your
            supplier operations.
          </p>
        </section>

      </main>
    </div>
  `;

    // Sidebar navigation
    // Sidebar navigation
    const navItems = document.querySelectorAll('.nav-item');
    const workspace = document.querySelector('.activity-section');

    // Load the logged-in supplier's account details
    async function loadSupplierAccount() {
        try {
            const company = await getCompanyProfile();

            const companyName = company.companyName || 'Supplier';

            document.querySelector('#supplierDisplayName').textContent = companyName;
            document.querySelector('#supplierAvatar').textContent =
                companyName.charAt(0).toUpperCase();

            document.querySelector('#dropdownCompanyName').textContent = companyName;

            document.querySelector('#dropdownCompanyEmail').textContent =
                company.emailAddress || 'Email not available';

            document.querySelector('#dropdownCompanyStatus').textContent =
                `Status: ${company.status || 'Not available'}`;

        } catch (error) {
            console.error('Unable to load supplier account:', error);

            document.querySelector('#supplierDisplayName').textContent =
                'Supplier Account';

            document.querySelector('#dropdownCompanyName').textContent =
                'Account details unavailable';
        }
    }

// Account dropdown
    const accountButton = document.querySelector('#supplierAccountButton');
    const accountDropdown = document.querySelector('#supplierAccountDropdown');

    accountButton.addEventListener('click', (event) => {
        event.stopPropagation();
        accountDropdown.hidden = !accountDropdown.hidden;
    });

// Close dropdown when clicking outside
    document.addEventListener('click', (event) => {
        if (!event.target.closest('.user-menu-container')) {
            accountDropdown.hidden = true;
        }
    });

// Open Company Profile from the dropdown
    document.querySelector('#dropdownProfileButton')
        .addEventListener('click', async () => {
            accountDropdown.hidden = true;

            navItems.forEach(item => item.classList.remove('active'));

            const profileButton =
                document.querySelector('.nav-item[data-page="profile"]');

            profileButton.classList.add('active');

            await loadCompanyProfile();
        });

// Sign Out functionality
    function signOut() {
        localStorage.removeItem('token');
        window.location.reload();
    }


// Dropdown Sign Out
    document.querySelector('#dropdownLogoutButton')
        .addEventListener('click', signOut);

// Load supplier name when dashboard opens
    loadSupplierAccount();

    const pages = {
        overview: {
            title: 'Supplier Workspace',
            description: 'Select a module from the sidebar to manage your supplier operations.'
        },
        products: {
            title: 'My Products',
            description: 'View and manage your registered supplier products.'
        },
        sourcing: {
            title: 'Sourcing Opportunities',
            description: 'Explore available sourcing opportunities.'
        },
        bids: {
            title: 'My Bids',
            description: 'Track your submitted and draft bids.'
        },
        profile: {
            title: 'Company Profile',
            description: 'View and manage your company information.'
        }
    };

    navItems.forEach((button) => {
        button.addEventListener('click', async () => {
            navItems.forEach(item => item.classList.remove('active'));
            button.classList.add('active');

            const page = button.dataset.page;

            if (page === 'products') {
                await loadMyProducts();
                return;
            }
            if(page === 'sourcing'){
                await loadSourcingOpportunities();
                return;
            }
            if (page === 'bids'){
                await loadMyBids();
                return;
            }
            if (page === 'profile'){
                await loadCompanyProfile();
                return;
            }

            const selectedPage = pages[page];

            if (selectedPage) {
                workspace.innerHTML = `
          <h2>${selectedPage.title}</h2>
          <p>${selectedPage.description}</p>
        `;
            }
        });
    });

    // Load registered supplier products
    async function loadMyProducts() {
        workspace.innerHTML = `
      <h2>My Products</h2>
      <p>Loading your products...</p>
    `;

        try {
            const products = await getMyProducts();

            if (!Array.isArray(products) || products.length === 0) {
                workspace.innerHTML = `
          <h2>My Products</h2>
          <p>No registered products found.</p>
        `;
                return;
            }

            const rows = products.map(product => `
        <tr>
          <td>${product.productName || '-'}</td>
          <td>${product.productCode || '-'}</td>
          <td>${product.brand || '-'}</td>
          <td>${product.categoryName || '-'}</td>
          <td>${product.status || '-'}</td>
        </tr>
      `).join('');

            workspace.innerHTML = `
        <h2>My Products</h2>
        <p>Products registered under your supplier account.</p>

        <div class="product-table-wrapper">
          <table>
            <thead>
              <tr>
                <th>Product Name</th>
                <th>Product Code</th>
                <th>Brand</th>
                <th>Category</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>${rows}</tbody>
          </table>
        </div>
      `;

        } catch (error) {
            console.error('Unable to load products:', error);

            workspace.innerHTML = `
        <h2>My Products</h2>
        <p>Unable to load products. Please try again.</p>
      `;
        }
    }

    async function loadSourcingOpportunities() {
        workspace.innerHTML = `
        <div class="workspace-header">
            <h2>Sourcing Opportunities</h2>
            <p>View sourcing requests shared with your company.</p>
        </div>

        <p>Loading sourcing opportunities...</p>
    `;

        try {
            const response = await getInvitedSourcingOpportunities();

            const opportunities = Array.isArray(response)
                ? response
                : response?.content || response?.data || [];

            if (opportunities.length === 0) {
                workspace.innerHTML = `
                <div class="workspace-header">
                    <h2>Sourcing Opportunities</h2>
                    <p>View sourcing requests shared with your company.</p>
                </div>

                <div class="empty-state">
                    <h3>No Active Opportunities</h3>
                    <p>
                        There are currently no active sourcing
                        opportunities available for your company.
                    </p>
                </div>
            `;
                return;
            }

            workspace.innerHTML = `
            <div class="workspace-header">
                <h2>Sourcing Opportunities</h2>
                <p>Explore sourcing requests invited to your company.</p>
            </div>

            <div class="table-container">
                <table class="product-table">
                    <thead>
                        <tr>
                            <th>Request Number</th>
                            <th>Title</th>
                            <th>Status</th>
                            <th>Published Date</th>
                            <th>Deadline</th>
                        </tr>
                    </thead>

                    <tbody>
                        ${opportunities.map(item => `
                            <tr>
                                <td>
                                    ${item.requestNumber
            || item.sourcingRequestNumber
            || '-'}
                                </td>

                                <td>
                                    ${item.title
            || item.requestTitle
            || item.sourcingRequestTitle
            || '-'}
                                </td>

                                <td>
                                    ${item.requestStatus
            || item.status
            || '-'}
                                </td>

                                <td>
                                    ${item.publishedAt
                ? new Date(item.publishedAt)
                    .toLocaleDateString()
                : '-'}
                                </td>

                                <td>
                                    ${item.submissionDeadline
            || item.responseDeadline
            || item.closingDate
            || '-'}
                                </td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;

        } catch (error) {
            console.error(
                'Error loading sourcing opportunities:',
                error
            );

            workspace.innerHTML = `
            <div class="workspace-header">
                <h2>Sourcing Opportunities</h2>
            </div>

            <div class="empty-state">
                <h3>Unable to Load Opportunities</h3>
                <p>Please try again later.</p>
            </div>
        `;
        }
    }

    async function loadMyBids() {
        workspace.innerHTML = `
        <h2>My Bids</h2>
        <p>Loading your bids...</p>
    `;

        try {
            const sourcingRequestId =
                '4ec5d41f-ecc9-449f-ac86-ca4816d4f80a';

            const bids = await getBidsForSourcingRequest(sourcingRequestId);

            if (!bids || bids.length === 0) {
                workspace.innerHTML = `
                <h2>My Bids</h2>
                <p>No bids found.</p>
            `;
                return;
            }

            workspace.innerHTML = `
            <h2>My Bids</h2>

            <table class="product-table">
                <thead>
                    <tr>
                        <th>Bid Number</th>
                        <th>Sourcing Request</th>
                        <th>Amount</th>
                        <th>Status</th>
                        <th>Submission Date</th>
                    </tr>
                </thead>
                <tbody>
                    ${bids.map(bid => `
                        <tr>
                            <td>${bid.bidNumber}</td>
                            <td>${bid.sourcingRequestSysId}</td>
                            <td>${bid.currencyCode} ${bid.totalBidAmount}</td>
                            <td>${bid.bidStatus}</td>
                            <td>${bid.submissionDate
                ? new Date(bid.submissionDate).toLocaleDateString()
                : '-'}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        `;

        } catch (error) {
            console.error('Error loading bids:', error);

            workspace.innerHTML = `
            <h2>My Bids</h2>
            <p>Unable to load bids. Please try again.</p>
        `;
        }
    }

    async function loadCompanyProfile() {


        workspace.innerHTML = `
        <div class="workspace-header">
            <h2>Company Profile</h2>
            <p>View and update your supplier company information.</p>
        </div>

        <div class="profile-container">
            <p>Loading company profile...</p>
        </div>
    `;

        try {
            const company = await getCompanyProfile();

            workspace.querySelector('.profile-container').innerHTML = `
            <form id="companyProfileForm" class="profile-form">

                <div class="profile-grid">

                    <div class="form-group">
                        <label>Company Name</label>
                        <input name="companyName" value="${company.companyName ?? ''}" required>
                    </div>

                    <div class="form-group">
                        <label>Legal Name</label>
                        <input name="legalName" value="${company.legalName ?? ''}">
                    </div>

                    <div class="form-group">
                        <label>GST Number</label>
                        <input name="gstNumber" value="${company.gstNumber ?? ''}">
                    </div>

                    <div class="form-group">
                        <label>PAN Number</label>
                        <input name="panNumber" value="${company.panNumber ?? ''}">
                    </div>

                    <div class="form-group">
                        <label>Website</label>
                        <input name="website" value="${company.website ?? ''}">
                    </div>

                    <div class="form-group">
                        <label>Contact Number</label>
                        <input name="contactNumber" value="${company.contactNumber ?? ''}">
                    </div>

                    <div class="form-group">
                        <label>Supplier Category</label>
                        <input name="supplierCategory" value="${company.supplierCategory ?? ''}">
                    </div>

                    <div class="form-group">
                        <label>Lead Time (Days)</label>
                        <input type="number" name="leadTimeDays"
                            value="${company.leadTimeDays ?? ''}" min="1">
                    </div>

                    <div class="form-group">
                        <label>Minimum Order Value</label>
                        <input type="number" name="minimumOrderValue"
                            value="${company.minimumOrderValue ?? ''}" min="0.01" step="0.01">
                    </div>

                    <div class="form-group">
                        <label>Email Address</label>
                        <input value="${company.emailAddress ?? ''}" readonly>
                    </div>

                    <div class="form-group">
                        <label>Company Status</label>
                        <input value="${company.status ?? ''}" readonly>
                    </div>

                </div>

                <button type="submit" class="save-profile-btn">
                    Save Changes
                </button>

                <p id="profileMessage"></p>

            </form>
        `;

            document.querySelector('#companyProfileForm')
                .addEventListener('submit', saveCompanyProfile);

        } catch (error) {
            workspace.querySelector('.profile-container').innerHTML = `
            <p class="error-message">
                Failed to load company profile:
                ${error.response?.data?.message || error.message}
            </p>
        `;
        }
    }

    async function saveCompanyProfile(event) {
        event.preventDefault();

        const form = event.target;
        const formData = new FormData(form);

        const profileData = Object.fromEntries(formData.entries());

        profileData.leadTimeDays =
            profileData.leadTimeDays
                ? Number(profileData.leadTimeDays)
                : null;

        profileData.minimumOrderValue =
            profileData.minimumOrderValue
                ? Number(profileData.minimumOrderValue)
                : null;

        try {
            await updateCompanyProfile(profileData);

            document.querySelector('#profileMessage').textContent =
                'Company profile updated successfully!';

        } catch (error) {
            document.querySelector('#profileMessage').textContent =
                error.response?.data?.message || 'Failed to update company profile.';
        }
    }

    async function loadDashboardStats() {
        try {
            const products = await getMyProducts();

            document.querySelector('#myProductsCount').textContent =
                Array.isArray(products) ? products.length : 0;

        } catch (error) {
            console.error('Products count error:', error);
        }

        try {
            const response = await getInvitedSourcingOpportunities();

            const opportunities = Array.isArray(response)
                ? response
                : response?.content || response?.data || [];

            document.querySelector('#sourcingCount').textContent =
                opportunities.length;

        } catch (error) {
            console.error('Sourcing count error:', error);
        }

        try {
            const sourcingRequestId =
                '4ec5d41f-ecc9-449f-ac86-ca4816d4f80a';

            const bids = await getBidsForSourcingRequest(sourcingRequestId);

            document.querySelector('#myBidsCount').textContent =
                Array.isArray(bids) ? bids.length : 0;

        } catch (error) {
            console.error('Bids count error:', error);
        }
    }
    loadDashboardStats();
}